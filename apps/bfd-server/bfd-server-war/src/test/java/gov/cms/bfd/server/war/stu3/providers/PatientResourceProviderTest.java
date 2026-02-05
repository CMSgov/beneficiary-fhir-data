package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import jakarta.persistence.EntityManager;
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
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link PatientResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * PatientE2E}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class PatientResourceProviderTest {

  /** The class under test. */
  PatientResourceProvider patientProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType patientId;

  /** The mock metric registry. */
  @Mock private MetricRegistry metricRegistry;

  /** The mock loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;

  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;

  /** The mock transformer. */
  @Mock private BeneficiaryTransformer beneficiaryTransformer;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The test data bene. */
  private Beneficiary testBene;

  /** Test return Patient. */
  @Mock private Patient testPatient;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    patientProvider =
        new PatientResourceProvider(metricRegistry, loadedFilterManager, beneficiaryTransformer);
    when(patientId.getVersionIdPartAsLong()).thenReturn(null);
    patientProvider.setEntityManager(entityManager);

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .orElseThrow();

    when(patientId.getIdPart()).thenReturn(String.valueOf(testBene.getBeneficiaryId()));
    when(patientId.getVersionIdPartAsLong()).thenReturn(null);

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    // transformer mocking
    when(testPatient.getId()).thenReturn("123");
    when(beneficiaryTransformer.transform(any(), any())).thenReturn(testPatient);

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
    when(mockCriteria.subquery(any(Class.class))).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);
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
   * Verifies that {@link PatientResourceProvider#read} throws an exception for an {@link IdType}
   * that has a null patientId parameter.
   */
  @Test
  public void testPatientReadWhereNullIdExpectException() {
    // pass null as id so the entire object is null
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(null, requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#read} throws an exception for an {@link IdType}
   * that has a missing patientId parameter.
   */
  @Test
  public void testPatientReadWhereEmptyIdExpectException() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.read(new IdType(), requestDetails));
    assertEquals("Missing required patient ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#read} throws an exception for an {@link IdType}
   * that has a non-numeric value.
   */
  @Test
  public void testPatientReadWhereNonNumericIdExpectException() {
    when(patientId.getIdPart()).thenReturn("abc");
    when(patientId.getIdPartAsLong()).thenThrow(NumberFormatException.class);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must be a number", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#read} throws an exception for an {@link IdType}
   * that has a version supplied with the patientId parameter, as our read requests do not support
   * versioned requests.
   */
  @Test
  public void testPatientReadWhereVersionedIdExpectException() {
    when(patientId.getIdPart()).thenReturn("1234");
    when(patientId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> patientProvider.read(patientId, requestDetails));
    assertEquals("Patient ID must not define a version", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#searchByCoverageContract} throws an exception when
   * searching by contract id where the contract id is not a number.
   */
  @Test
  public void testSearchByCoverageContractWhereNonNumericContractIdExpectException() {

    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "abcde");
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
   * Verifies that {@link PatientResourceProvider#searchByCoverageContract} throws an exception when
   * searching by contract id where the contract id is not a valid length.
   */
  @Test
  public void testSearchByCoverageContractWhereWrongLengthContractIdExpectException() {

    TokenParam contractId = new TokenParam("2001/PTDCNTRCT10", "123");
    TokenParam refYear = new TokenParam("", "2001");

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
   * Verifies that {@link PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id is blank.
   */
  @Test
  public void testSearchByLogicalIdWhereBlankIdExpectException() {

    TokenParam logicalId = new TokenParam("", "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByLogicalId(logicalId, null, null, null, requestDetails));
    assertEquals("Missing required id value", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#searchByLogicalId} throws an exception when
   * searching by logical id where the id.system is blank.
   */
  @Test
  public void testSearchByLogicalIdWhereNonEmptySystemExpectException() {

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
   * Verifies that {@link PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search hash is empty.
   */
  @Test
  public void testSearchByIdentifierIdWhereEmptyHashExpectException() {

    when(requestDetails.getHeader(any())).thenReturn("");
    TokenParam identifier = new TokenParam(TransformerConstants.CODING_BBAPI_BENE_MBI_HASH, "");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, null, requestDetails));
    assertEquals("lookup value cannot be null/empty", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link PatientResourceProvider#searchByIdentifier} throws an exception when
   * searching by identifier where the search system is not supported.
   */
  @Test
  public void testSearchByIdentifierIdWhereBadSystemExpectException() {

    TokenParam identifier = new TokenParam("bad-system", "4ffa4tfgd4ggddgh4h");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> patientProvider.searchByIdentifier(identifier, null, null, null, requestDetails));
    assertEquals("Unsupported identifier system: bad-system", exception.getLocalizedMessage());
  }
}
