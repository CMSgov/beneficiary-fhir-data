package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
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
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * PatientE2E}.
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

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock query, for mocking native function call. */
  @Mock TypedQuery mockQueryFunction;

  /** The test data bene. */
  private Beneficiary testBene;

  /** Test return Patient. */
  @Mock private Patient testPatient;

  /** Logical id to pass in which is considered legal for validation checks. */
  private final TokenParam logicalId = new TokenParam("", "1234");

  /** Hash search identifier using a made-up hash. */
  private final TokenParam mbiHashIdentifier =
      new TokenParam(
          TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
          "7004708ca44c2ff45b663ef661059ac98131ccb90b4a7e53917e3af7f50c4c56");

  /**
   * US-MBI search identifier using sample MBI value from
   * https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi.pdf.
   */
  private final TokenParam unhashedMbiIdentifier =
      new TokenParam(
          TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, "1EG4TE5MK73");

  /** A 'valid' contract id to use in tests. */
  private final TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "abcde");

  /** A 'valid' contract reference year to use in tests. */
  TokenParam refYear = new TokenParam("", "2001");

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
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    // transformer mocking
    when(testPatient.getId()).thenReturn("123");
    when(beneficiaryTransformerV2.transform(any(), any(), anyBoolean())).thenReturn(testPatient);
    when(beneficiaryTransformerV2.transform(any(), any())).thenReturn(testPatient);

    setupLastUpdatedMocks();

    mockHeaders();
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
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore on v2, so set it to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
    when(requestDetails.getCompleteUrl()).thenReturn("test");
  }

  /** Sets up the default entity manager mocks. */
  private void mockEntityManager() {
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Beneficiary> mockCriteria = mock(CriteriaQuery.class);
    Root<Beneficiary> root = mock(Root.class);
    Path mockPath = mock(Path.class);
    Subquery mockSubquery = mock(Subquery.class);
    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    doReturn(mockCriteria).when(criteriaBuilder).createQuery(any());
    when(mockCriteria.select(any())).thenReturn(mockCriteria);
    when(mockCriteria.from(any(Class.class))).thenReturn(root);
    when(root.get(isNull(SingularAttribute.class))).thenReturn(mockPath);
    when(entityManager.createQuery(mockCriteria)).thenReturn(mockQuery);
    when(mockQuery.setHint(any(), anyBoolean())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getResultList()).thenReturn(List.of(testBene));
    when(mockQuery.getSingleResult()).thenReturn(testBene);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockCriteria.subquery(any(Class.class))).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);

    when(entityManager.createNativeQuery(anyString())).thenReturn(mockQueryFunction);
    when(mockQueryFunction.setHint(any(), any())).thenReturn(mockQueryFunction);
    when(mockQueryFunction.setParameter(anyString(), anyString())).thenReturn(mockQueryFunction);
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

    String expectedTimerName =
        patientProvider.getClass().getSimpleName() + ".query.bene_by_mbi_or_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
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
    // We dont use includeIdentifiers for v2, so dont bother testing it

    patientProvider.read(patientId, requestDetails);

    final ArgumentCaptor<RequestHeaders> captor = ArgumentCaptor.forClass(RequestHeaders.class);
    verify(beneficiaryTransformerV2, times(1)).transform(any(), captor.capture(), anyBoolean());

    // Grab the relevant headers passed into the transformer and ensure they are set with our values
    RequestHeaders passedHeader = captor.getValue();
    assertEquals(
        Boolean.TRUE, passedHeader.getValue(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS));
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
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} triggers all expected
   * metrics when a valid request is made.
   */
  @Test
  public void testSearchByCoverageContractWhenValidContractExpectMetrics() {
    patientProvider.searchByCoverageContract(contractId, refYear, null, null, requestDetails);

    // Three queries are made with metrics here
    String expectedTimerName =
        patientProvider.getClass().getSimpleName()
            + ".query.bene_exists_by_year_month_part_d_contract_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    String expectedTimerName2 =
        patientProvider.getClass().getSimpleName()
            + ".query.bene_ids_by_year_month_part_d_contract_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName2);
    String expectedTimerName3 =
        patientProvider.getClass().getSimpleName()
            + ".query.bene_exists_by_year_month_part_d_contract_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName3);

    verify(metricsTimer, times(3)).time();
    verify(metricsTimerContext, times(3)).stop();
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} calls the transformer
   * when making a happy path call where the contract is found.
   */
  @Test
  public void testSearchByCoverageContractWhenContractExistsExpectTransformerCalled() {
    Bundle response =
        patientProvider.searchByCoverageContract(contractId, refYear, null, null, requestDetails);

    assertEquals(1, response.getTotal());
    verify(beneficiaryTransformerV2, times(1)).transform(eq(testBene), any());

    /*
     * Check that no paging was added
     */
    assertNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} does not return paging
   * data when none is requested.
   */
  @Test
  public void testSearchByCoverageContractWhenNoPagingRequestedExpectNoPageData() {
    Bundle response =
        patientProvider.searchByCoverageContract(contractId, refYear, null, null, requestDetails);

    /*
     * Check that no paging was added
     */
    assertNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} returns paging links
   * when making a happy path call where the contract is found and paging is requested.
   */
  @Test
  public void testSearchByCoverageContractWhenPagingRequestedExpectPageData() {
    // Set paging params
    // Apparently the contract endpoint gets the count from the request url instead of how the other
    // endpoints do
    when(requestDetails.getCompleteUrl()).thenReturn("https://test?_count=1");
    when(requestDetails.getParameters()).thenReturn(Map.of("_count", new String[] {"1"}));
    // Note: cursor in the param is not used, must be passed from requestDetails

    Bundle response =
        patientProvider.searchByCoverageContract(contractId, refYear, null, null, requestDetails);

    /*
     * Check paging; Paging on contract also apparently returns differently
     * and gives back first/next unlike the others which give back
     * first/last. May be due to how it sets/reads count. Maybe a bug?
     */
    assertNotNull(response.getLink(Constants.LINK_FIRST));
    assertNotNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} returns an empty
   * bundle when no patients can be found for the input contract/year.
   */
  @Test
  public void testSearchByCoverageContractWhenNoPatientsExpectEmptyBundle() {
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);
    when(mockQuery.getResultList()).thenReturn(new ArrayList());

    Bundle response =
        patientProvider.searchByCoverageContract(contractId, refYear, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a number.
   */
  @Test
  public void testSearchByCoverageContractWhenNonNumericContractIdExpectException() {
    TokenParam refYear = new TokenParam("", "abc");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, null, requestDetails));
    assertEquals(
        "Failed to parse value for Contract Year as a number.", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} throws an exception
   * when searching by contract id where the contract id is not a valid length.
   */
  @Test
  public void testSearchByCoverageContractWhenWrongLengthContractIdExpectException() {
    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "123");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () ->
                patientProvider.searchByCoverageContract(
                    contractId, refYear, null, null, requestDetails));
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

    Bundle response =
        patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails);

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

    Bundle response =
        patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#read} calls the transformer when making a happy
   * path call where the patient is found in the DB and paging is requested, the page data is
   * returned on the bundle.
   */
  @Test
  public void testSearchByLogicalIdWhenPagingRequestedExpectPageData() {
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
    Bundle response =
        patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails);

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
  public void testSearchByLogicalIdWhenNoPagingRequestedExpectNoPageData() {
    // Set no paging params
    Map<String, String[]> params = new HashMap<>();
    when(requestDetails.getParameters()).thenReturn(params);
    Bundle response =
        patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails);

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
            () -> patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails));
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
            () -> patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails));
    assertEquals(
        "System is unsupported here and should not be set (system)",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} returns a Bundle with a
   * patient result when the db search is successful (mocked) and searching by hashed mbi. Also
   * check that the expected metrics are called.
   */
  @Test
  public void testSearchByIdentifierIdWhenMbiHashExpectPatientAndMetrics() {

    when(requestDetails.getHeader(any())).thenReturn("");
    when(mockQueryFunction.setParameter(anyString(), anyString())).thenReturn(mockQueryFunction);
    List<Object> rawValues =
        new ArrayList<Object>() {
          {
            add(Long.toString(testBene.getBeneficiaryId()));
          }
        };
    when(mockQueryFunction.getResultList()).thenReturn(rawValues);

    Bundle bundle =
        patientProvider.searchByIdentifier(mbiHashIdentifier, null, null, null, requestDetails);

    assertEquals(1, bundle.getTotal());

    String expectedTimerName =
        patientProvider.getClass().getSimpleName() + ".query.bene_by_mbi.bene_by_mbi_or_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    String expectedTimerName2 =
        patientProvider.getClass().getSimpleName() + ".query.bene_by_mbi.bene_by_mbi_or_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName2);

    verify(metricsTimer, times(2)).time();
    verify(metricsTimerContext, times(2)).stop();
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} returns no paging when no
   * paging is requested.
   */
  @Test
  public void testSearchByIdentifierIdWhenNoPagingExpectNoPageData() {

    when(requestDetails.getHeader(any())).thenReturn("");

    Bundle bundle =
        patientProvider.searchByIdentifier(mbiHashIdentifier, null, null, null, requestDetails);

    /*
     * Check that no paging was added when not requested
     */
    assertNull(bundle.getLink(Constants.LINK_FIRST));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
    assertNull(bundle.getLink(Constants.LINK_PREVIOUS));
    assertNull(bundle.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} returns a Bundle with paging
   * links when paging is requested.
   */
  @Test
  public void testSearchByIdentifierIdWhenPagingRequestedExpectPageData() {

    when(requestDetails.getHeader(any())).thenReturn("");
    // Set paging params
    Map<String, String[]> params = new HashMap<>();
    params.put(Constants.PARAM_COUNT, new String[] {"1"});
    when(requestDetails.getParameters()).thenReturn(params);
    // Note: startIndex in the param is not used, must be passed from requestDetails
    Bundle bundle =
        patientProvider.searchByIdentifier(mbiHashIdentifier, null, null, null, requestDetails);

    /*
     * Check paging; Verify that only the first and last paging links exist, since there should
     * only be one page.
     */
    assertNotNull(bundle.getLink(Constants.LINK_FIRST));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
    assertNull(bundle.getLink(Constants.LINK_PREVIOUS));
    assertNotNull(bundle.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} returns an empty bundle when
   * the patient is not found in the DB and searching by MBI hash.
   */
  @Test
  public void testSearchByIdentifierIdWhenBeneDoesntExistExpectEmptyBundle() {
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
    when(mockQueryFunction.getSingleResult()).thenThrow(NoResultException.class);

    Bundle bundle =
        patientProvider.searchByIdentifier(mbiHashIdentifier, null, null, null, requestDetails);

    assertEquals(0, bundle.getTotal());
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
            () -> patientProvider.searchByIdentifier(identifier, null, null, null, requestDetails));
    assertEquals("lookup value cannot be null/empty", exception.getLocalizedMessage());
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
            () -> patientProvider.searchByIdentifier(identifier, null, null, null, requestDetails));
    assertEquals("Unsupported identifier system: bad-system", exception.getLocalizedMessage());
  }
}
