package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link R4PatientResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * R4PatientResourceProviderIT}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class R4PatientResourceProviderTest {

  /** The class under test. */
  R4PatientResourceProvider patientProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType patientId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;
  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;
  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;
  /** The Beneficiary transformer. */
  @Mock private BeneficiaryTransformerV2 beneficiaryTransformerV2;
  /** The mock metric timer. */
  @Mock Timer mockTimer;
  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;
  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The test data bene. */
  private Beneficiary testBene;

  /** Test return Patient. */
  @Mock private Patient testPatient;

  /** Logical id to pass in which is considered legal for validation checks. */
  private final TokenParam logicalId = new TokenParam("", "1234");

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    patientProvider =
        new R4PatientResourceProvider(
            metricRegistry, loadedFilterManager, beneficiaryTransformerV2);
    patientProvider.setEntityManager(entityManager);

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    when(patientId.getIdPart()).thenReturn(String.valueOf(testBene.getBeneficiaryId()));
    when(patientId.getVersionIdPartAsLong()).thenReturn(null);

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

    // transformer mocking
    when(testPatient.getId()).thenReturn("123test");
    lenient()
        .when(beneficiaryTransformerV2.transform(any(), any(), anyBoolean()))
        .thenReturn(testPatient);

    setupLastUpdatedMocks();

    mockHeaders();
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the transformer when making a happy
   * path call where the patient is found in the DB.
   */
  @Test
  public void testReadWhenBeneExistsExpectTransformerCalled() {
    Patient response = patientProvider.read(patientId, requestDetails);

    assertEquals(testPatient, response);
    verify(beneficiaryTransformerV2, times(1)).transform(eq(testBene), any(), eq(true));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the metrics when making a happy path
   * call where the patient is found in the DB.
   */
  @Test
  public void testReadWhenBeneExistsExpectMetricsCalled() {
    patientProvider.read(patientId, requestDetails);

    verify(mockTimer, times(1)).time();
    verify(mockTimerContext, times(1)).stop();
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} correctly passes the headers to the
   * transformer when they are set.
   */
  @Test
  public void testReadWhenHeadersSetExpectHeadersPassedToTransformer() {

    // Set the headers to test, so we can tell they got passed down
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("true");
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn("true");
    // We dont use includeIdentifiers for v2, so dont bother testing it

    patientProvider.read(patientId, requestDetails);

    final ArgumentCaptor<RequestHeaders> captor = ArgumentCaptor.forClass(RequestHeaders.class);
    verify(beneficiaryTransformerV2, times(1)).transform(any(), captor.capture(), anyBoolean());

    // Grab the relevant headers passed into the transformer and ensure they are set with our values
    RequestHeaders passedHeader = captor.getValue();
    assertEquals(
        Boolean.TRUE, passedHeader.getValue(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS));
    assertEquals(
        Boolean.TRUE, passedHeader.getValue(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception when a beneficiary is
   * not found in the DB.
   */
  @Test
  public void testReadWhenNoDbResultExpectException() {
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          patientProvider.read(patientId, requestDetails);
        });
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a null patientId parameter.
   */
  @Test
  public void testReadWhenNullIdExpectException() {
    // pass null as id so the entire object is null
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(null, requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a missing patientId parameter.
   */
  @Test
  public void testReadWhenEmptyIdExpectException() {
    // Dont set up patient id to return anything, so our id value is null
    patientId = mock(IdType.class);
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a non-numeric value.
   */
  @Test
  public void testReadWhenNonNumericIdExpectException() {
    when(patientId.getIdPart()).thenReturn("abc");
    when(patientId.getIdPartAsLong()).thenThrow(NumberFormatException.class);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must be a number", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} throws an exception for an {@link
   * org.hl7.fhir.r4.model.IdType} that has a version supplied with the patientId parameter, as our
   * read requests do not support versioned requests.
   */
  @Test
  public void testReadWhenVersionedIdExpectException() {
    when(patientId.getIdPart()).thenReturn("1234");
    when(patientId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must not define a version", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a number.
   */
  @Test
  public void testSearchByCoverageContractWhenNonNumericContractIdExpectException() {
    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "abcde");
    TokenParam refYear = new TokenParam("", "abc");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, requestDetails));
    assertEquals("Contract year must be a number.", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a valid length.
   */
  @Test
  public void testSearchByCoverageContractWhenWrongLengthContractIdExpectException() {
    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "123");
    TokenParam refYear = new TokenParam("", "2001");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, requestDetails));
    assertEquals(
        "Coverage id is not expected length; value 123 is not expected length 5",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the transformer when making a happy
   * path call where the patient is found in the DB.
   */
  @Test
  public void testSearchByLogicalIdWhenBeneExistsExpectTransformerCalled() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testPatient.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());

    Bundle response = patientProvider.searchByLogicalId(logicalId, null, null, requestDetails);

    assertEquals(testPatient, response.getEntry().get(0).getResource());
    verify(beneficiaryTransformerV2, times(1)).transform(eq(testBene), any(), eq(true));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} returns an empty bundle when the patient
   * is not found in the DB.
   */
  @Test
  public void testSearchByLogicalIdWhenBeneDoesntExistExpectEmptyBundle() {
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);

    Bundle response = patientProvider.searchByLogicalId(logicalId, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the transformer when making a happy
   * path call where the patient is found in the DB and paging is requested, the page data is
   * returned on the bundle.
   */
  @Test
  public void testSearchByLogicalIdWhenPagingExpectBundlePages() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testPatient.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());

    // Set paging params
    Map<String, String[]> params = new HashMap<>();
    params.put(Constants.PARAM_COUNT, new String[] {"1"});
    when(requestDetails.getParameters()).thenReturn(params);
    // Note: startIndex in the param is not used, must be passed from requestDetails
    Bundle response = patientProvider.searchByLogicalId(logicalId, null, null, requestDetails);

    /*
     * Check paging; Verify that only the first and last paging links exist, since there should
     * only be one page.
     */
    assertNotNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNotNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the transformer when making a happy
   * path call where the patient is found in the DB and paging is not requested, the page data is
   * not returned on the bundle.
   */
  @Test
  public void testSearchByLogicalIdWhenNoPagingExpectNoBundlePages() {
    // Set no paging params
    Map<String, String[]> params = new HashMap<>();
    when(requestDetails.getParameters()).thenReturn(params);
    Bundle response = patientProvider.searchByLogicalId(logicalId, null, null, requestDetails);

    /*
     * Check that no paging was added
     */
    assertNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id is blank.
   */
  @Test
  public void testSearchByLogicalIdWhenBlankIdExpectException() {

    TokenParam logicalId = new TokenParam("", "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByLogicalId(logicalId, null, null, requestDetails));
    assertEquals("Missing required id value", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id.system is blank.
   */
  @Test
  public void testSearchByLogicalIdWhenNonEmptySystemExpectException() {

    TokenParam logicalId = new TokenParam("system", "1234");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByLogicalId(logicalId, null, null, requestDetails));
    assertEquals(
        "System is unsupported here and should not be set (system)",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search hash is empty.
   */
  @Test
  public void testSearchByIdentifierIdWhenEmptyHashExpectException() {

    when(requestDetails.getHeader(any())).thenReturn("");
    TokenParam identifier = new TokenParam(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, requestDetails));
    assertEquals("Hash value cannot be null/empty", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search system is not supported.
   */
  @Test
  public void testSearchByIdentifierIdWhenBadSystemExpectException() {

    TokenParam identifier = new TokenParam("bad-system", "4ffa4tfgd4ggddgh4h");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, requestDetails));
    assertEquals("Unsupported identifier system: bad-system", exception.getLocalizedMessage());
  }

  /** Sets up the last updated mocks. */
  private void setupLastUpdatedMocks() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testPatient.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
  }

  /** Mocks the default header values. */
  private void mockHeaders() {
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn("false");
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore on v2, so set it to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
  }

  /** Sets up the default entity manager mocks. */
  private void mockEntityManager() {
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Beneficiary> mockCriteria = mock(CriteriaQuery.class);
    Root<Beneficiary> root = mock(Root.class);
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    doReturn(mockCriteria).when(criteriaBuilder).createQuery(any());
    doReturn(root).when(mockCriteria).from(any(Class.class));
    when(entityManager.createQuery(mockCriteria)).thenReturn(mockQuery);
    when(mockQuery.getSingleResult()).thenReturn(testBene);
  }
}
