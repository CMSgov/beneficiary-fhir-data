package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link CoverageResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * CoverageE2E}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CoverageResourceProviderTest {

  /** The class under test. */
  CoverageResourceProvider coverageProvider;

  /** The mocked input id value. */
  @Mock IdType coverageId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;

  /** The mock coverage transformer. */
  @Mock private CoverageTransformer coverageTransformer;

  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;

  /** Used to mock bene data for a bene search. */
  @Mock ReferenceParam beneficiary;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The test data bene. */
  private Beneficiary testBene;

  /** The mock coverage item. * */
  @Mock private Coverage testCoverage;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** A valid coverage id number. Abides by the coverage id pattern in the resource provider. */
  private static final String VALID_PART_A_COVERAGE_ID = "part-a-1234";

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    coverageProvider =
        new CoverageResourceProvider(metricRegistry, loadedFilterManager, coverageTransformer);
    coverageProvider.setEntityManager(entityManager);
    lenient().when(coverageId.getVersionIdPartAsLong()).thenReturn(null);
    when(beneficiary.getIdPart()).thenReturn("111199991111");

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .orElseThrow();

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    // loaded filter mocking
    when(loadedFilterManager.isResultSetEmpty(any(), any())).thenReturn(false);

    // transformer mocking
    when(coverageTransformer.transform(any())).thenReturn(List.of(testCoverage));
    when(coverageTransformer.transform(any(), any())).thenReturn(testCoverage);

    // Mock coverage id
    when(coverageId.getIdPart()).thenReturn(VALID_PART_A_COVERAGE_ID);

    // Mock headers
    when(requestDetails.getCompleteUrl()).thenReturn("test");

    setupLastUpdatedMocks();
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
    when(mockCriteria.subquery(any())).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);
  }

  /** Sets up the last updated mocks. */
  private void setupLastUpdatedMocks() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testCoverage.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
  }

  /**
   * Tests that findBeneficiaryById properly calls the metrics registry start and stop with the
   * correct metrics name.
   */
  @Test
  public void testCoverageByIdExpectMetrics() {
    when(coverageId.getIdPart()).thenReturn(VALID_PART_A_COVERAGE_ID);

    coverageProvider.read(coverageId);

    String expectedTimerName = coverageProvider.getClass().getSimpleName() + ".query.bene_by_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has an invalidly formatted coverageId parameter.
   */
  @Test
  public void testCoverageReadWhereInvalidIdExpectException() {
    when(coverageId.getIdPart()).thenReturn("1?234");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals(
        "Coverage ID pattern: '1?234' does not match expected pattern: {alphaNumericString}-{singleCharacter}-{idNumber}",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a blank value.
   */
  @Test
  public void testCoverageReadWhereBlankIdExpectException() {
    when(coverageId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals("Missing required coverage ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a null value.
   */
  @Test
  public void testCoverageReadWhereNullIdExpectException() {
    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(null);
            });
    assertEquals("Missing required coverage ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link CoverageResourceProvider#read} throws an exception when the {@link IdType}
   * has a version supplied with the coverageId parameter, as our read requests do not support
   * versioned requests.
   */
  @Test
  public void testCoverageReadWhereVersionedIdExpectException() {
    when(coverageId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class,
            () -> {
              coverageProvider.read(coverageId);
            });
    assertEquals("Coverage ID must not define a version", exception.getLocalizedMessage());
  }
}
