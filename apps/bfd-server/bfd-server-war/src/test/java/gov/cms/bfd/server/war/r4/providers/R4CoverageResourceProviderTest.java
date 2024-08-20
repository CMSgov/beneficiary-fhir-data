package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.ProfileConstants;
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
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link R4CoverageResourceProvider} that do not require a full fhir setup to
 * validate. Anything we want to validate from the fhir client level should go in {@link
 * CoverageE2E}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class R4CoverageResourceProviderTest {

  /** The class under test. */
  R4CoverageResourceProvider coverageProvider;

  /** The mocked input id value. */
  @Mock IdType coverageId;

  /** The Metric registry. */
  @Mock private MetricRegistry metricRegistry;

  /** The Loaded filter manager. */
  @Mock private LoadedFilterManager loadedFilterManager;

  /** The mock coverage transformer. */
  @Mock private CoverageTransformerV2 coverageTransformer;

  /** The mock entity manager for mocking database calls. */
  @Mock private EntityManager entityManager;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** Used to mock bene data for a bene search. */
  @Mock ReferenceParam beneficiary;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The test data bene. */
  private Beneficiary testBene;

  /** The mock coverage item. * */
  @Mock private Coverage testCoverage;

  /** A valid coverage id number. Abides by the coverage id pattern in the resource provider. */
  private static final String VALID_PART_A_COVERAGE_ID = "part-a-1234";

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    coverageProvider =
        new R4CoverageResourceProvider(
            metricRegistry, loadedFilterManager, coverageTransformer, false);
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
            .get();

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    // loaded filter mocking
    when(loadedFilterManager.isResultSetEmpty(any(), any())).thenReturn(false);

    // transformer mocking
    when(coverageTransformer.transform(any(), (EnumSet<Profile>) any()))
        .thenReturn(List.of(testCoverage));
    when(coverageTransformer.transform(any(), (Beneficiary) any(), any())).thenReturn(testCoverage);

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
    when(mockCriteria.subquery(any(Class.class))).thenReturn(mockSubquery);
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
   * Tests that when the coverage id is formatted in a valid way, but the bene is not found in the
   * search query, an exception is thrown.
   */
  @Test
  public void testCoverageReadWhereMissingBeneExpectException() {
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          coverageProvider.read(coverageId);
        });
  }

  /**
   * Tests that findBeneficiaryById properly calls the metrics registry start and stop with the
   * correct metrics name.
   */
  @Test
  public void testCoverageByIdExpectMetrics() {
    when(coverageId.getIdPart()).thenReturn("part-a-9145");

    coverageProvider.read(coverageId);

    String expectedTimerName = coverageProvider.getClass().getSimpleName() + ".query.bene_by_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }

  /**
   * Tests that the validator (for the format of the coverage id) works when the id part is a
   * negative number (such as we use for our synthetic data).
   */
  @Test
  public void testCoverageReadWhereNegativeIdPartExpectNoError() {
    when(coverageId.getIdPart()).thenReturn("part-a--9145");

    Coverage coverage = coverageProvider.read(coverageId);

    assertNotNull(coverage);
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} throws an exception when the {@link
   * IdType} has an invalidly formatted coverageId parameter.
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
        "Coverage ID pattern: '1?234' does not match expected patterns: {alphaNumericString}?-{alphaNumericString}-{idNumber} or {alphaNumericString}?-{alphaNumericString}?-{alphaNumericString}-{idNumber}",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} throws an exception when the {@link
   * IdType} has a blank value.
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
   * Verifies that {@link R4CoverageResourceProvider#read} throws an exception when the {@link
   * IdType} has a null value.
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
   * Verifies that {@link R4CoverageResourceProvider#read} throws an exception when the {@link
   * IdType} has a version supplied with the coverageId parameter, as our read requests do not
   * support versioned requests.
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

  /**
   * Tests that when searching by beneficiary, if no beneficiary is found, 0 results are returned in
   * the bundle.
   */
  @Test
  public void testCoverageByBeneficiaryWhereMissingBeneExpectEmptyBundle() {
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);

    Bundle bundle =
        coverageProvider.searchByBeneficiary(beneficiary, null, null, null, requestDetails);

    assertEquals(0, bundle.getTotal());
  }

  /** Tests that the transformer is called with only the C4BB profile when C4DIC is not enabled. */
  @Test
  public void testCoverageByBeneficiaryCount() {
    coverageProvider.searchByBeneficiary(beneficiary, null, null, null, requestDetails);
    verify(coverageTransformer).transform(any(), eq(EnumSet.of(Profile.C4BB)));
  }

  /**
   * Tests that the transformer is called with only the C4BB profile when the C4BB profile is
   * requested.
   */
  @Test
  public void testCoverageByBeneficiaryCountC4BBProfile() {
    coverageProvider.searchByBeneficiary(
        beneficiary, null, null, ProfileConstants.C4BB_COVERAGE_URL, requestDetails);

    verify(coverageTransformer).transform(any(), eq(EnumSet.of(Profile.C4BB)));
  }

  /**
   * Tests that the transformer is called with only the C4DIC profile when the C4DIC profile is
   * requested.
   */
  @Test
  public void testCoverageByBeneficiaryCountC4DICProfile() {
    coverageProvider =
        new R4CoverageResourceProvider(
            metricRegistry, loadedFilterManager, coverageTransformer, true);
    coverageProvider.setEntityManager(entityManager);

    coverageProvider.searchByBeneficiary(
        beneficiary, null, null, ProfileConstants.C4DIC_COVERAGE_URL, requestDetails);

    verify(coverageTransformer).transform(any(), eq(EnumSet.of(Profile.C4DIC)));
  }

  /** Tests that the transformer is called with only the C4BB profile if no profile is specified. */
  @Test
  public void testCoverageByBeneficiaryCountBothProfiles() {
    coverageProvider =
        new R4CoverageResourceProvider(
            metricRegistry, loadedFilterManager, coverageTransformer, true);
    coverageProvider.setEntityManager(entityManager);

    coverageProvider.searchByBeneficiary(beneficiary, null, null, null, requestDetails);

    verify(coverageTransformer).transform(any(), eq(EnumSet.of(Profile.C4BB)));
  }

  /** Tests that the transformer is called with the C4DIC profile when a C4DIC ID is used. */
  @Test
  public void testCoverageByIdC4Dic() {
    coverageProvider =
        new R4CoverageResourceProvider(
            metricRegistry, loadedFilterManager, coverageTransformer, true);
    coverageProvider.setEntityManager(entityManager);

    when(coverageId.getIdPart()).thenReturn("c4dic-part-a-9145");

    coverageProvider.read(coverageId);

    verify(coverageTransformer).transform(eq(MedicareSegment.PART_A), any(), any());
  }

  /** Test coverage by beneficiary when paging is requested, expect paging links are returned. */
  @Test
  public void testCoverageByBeneficiaryWherePagingRequestedExpectPageData() {
    // Set paging params
    Map<String, String[]> params = new HashMap<>();
    params.put(Constants.PARAM_COUNT, new String[] {"1"});
    when(requestDetails.getParameters()).thenReturn(params);

    // Note: startIndex in the param is not used, must be passed from requestDetails
    Bundle bundle =
        coverageProvider.searchByBeneficiary(beneficiary, null, null, null, requestDetails);

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
   * Test coverage by beneficiary when paging is not requested, expect no paging links are returned.
   */
  @Test
  public void testCoverageByBeneficiaryWhereNoPagingRequestedExpectNoPageData() {
    when(requestDetails.getHeader(any())).thenReturn("");

    Bundle bundle =
        coverageProvider.searchByBeneficiary(beneficiary, null, null, null, requestDetails);

    /*
     * Check that no paging was added when not requested
     */
    assertNull(bundle.getLink(Constants.LINK_FIRST));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
    assertNull(bundle.getLink(Constants.LINK_PREVIOUS));
    assertNull(bundle.getLink(Constants.LINK_LAST));
  }
}
