package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;
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
 * R4CoverageResourceProviderE2E}.
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

  /** The mock metric timer. */
  @Mock Timer mockTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

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
        new R4CoverageResourceProvider(metricRegistry, loadedFilterManager, coverageTransformer);
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
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

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
        "Coverage ID pattern: '1?234' does not match expected pattern: {alphaNumericString}-{singleCharacter}-{idNumber}",
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

    Bundle bundle = coverageProvider.searchByBeneficiary(beneficiary, null, null, requestDetails);

    assertEquals(0, bundle.getTotal());
  }

  /** Test coverage by beneficiary when paging is requested, expect paging links are returned. */
  @Test
  public void testCoverageByBeneficiaryWherePagingRequestedExpectPageData() {
    // Set paging params
    Map<String, String[]> params = new HashMap<>();
    params.put(Constants.PARAM_COUNT, new String[] {"1"});
    when(requestDetails.getParameters()).thenReturn(params);

    // Note: startIndex in the param is not used, must be passed from requestDetails
    Bundle bundle = coverageProvider.searchByBeneficiary(beneficiary, null, null, requestDetails);

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

    Bundle bundle = coverageProvider.searchByBeneficiary(beneficiary, null, null, requestDetails);

    /*
     * Check that no paging was added when not requested
     */
    assertNull(bundle.getLink(Constants.LINK_FIRST));
    assertNull(bundle.getLink(Constants.LINK_NEXT));
    assertNull(bundle.getLink(Constants.LINK_PREVIOUS));
    assertNull(bundle.getLink(Constants.LINK_LAST));
  }
}
