package gov.cms.bfd.server.war.r4.providers.pac.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.r4.providers.pac.AbstractResourceTypeV2;
import gov.cms.bfd.server.war.r4.providers.pac.ClaimTypeV2;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests the {@link ClaimDao}. */
@ExtendWith(MockitoExtension.class)
class ClaimDaoTest {
  /** Dummy type used just for this unit test. */
  private static final MockClaimType claimType = new MockClaimType();

  /** Common value used in tests when a non-null lastUpdated parameter is needed. */
  private static final DateRangeParam LastUpdated =
      new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN, "2020-08-01"), null);

  /** Common value used in tests when a non-null serviceDate parameter is needed. */
  private static final DateRangeParam ServiceDate =
      new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN, "2020-08-01"), null);

  /** Used when creating a {@link ClaimDao}. */
  private MetricRegistry metricRegistry;

  /** Used when mocking query construction. */
  @Mock private EntityManager mockEntityManager;

  /** Used when mocking query construction. */
  @Mock private CriteriaBuilder mockBuilder;

  /** Initializes non-mock fields for each test. */
  @BeforeEach
  void setUp() {
    metricRegistry = new MetricRegistry();
  }

  /** Verifies the generated metric names follow the expected pattern. */
  @Test
  void testCreateMetricNameForResourceQuery() {
    assertEquals(
        "ClaimDao.query.claim_by_id.mockType",
        ClaimDao.createMetricNameForResourceQuery(claimType, ClaimDao.CLAIM_BY_ID_QUERY));
    assertEquals(
        "ClaimDao.query.claim_by_mbi.mockType",
        ClaimDao.createMetricNameForResourceQuery(claimType, ClaimDao.CLAIM_BY_MBI_QUERY));
  }

  /**
   * Verifies that {@link ClaimDao#getEntityById(ResourceTypeV2, String)} builds the correct query
   * to return the expected entity using a given ID.
   */
  @Test
  void shouldGetEntityById() {
    String claimId = "123";

    Object expected = 5L;

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, true));

    CriteriaQuery<?> mockQuery = mock(CriteriaQuery.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    Root mockRoot = mock(Root.class);

    Predicate mockPredicate = mock(Predicate.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    Path mockPath = mock(Path.class);

    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    TypedQuery mockTypedQuery = mock(TypedQuery.class);

    doReturn(mockPath).when(mockRoot).get(claimType.getEntityIdAttribute());

    doReturn(mockRoot).when(mockQuery).from(Long.class);

    doReturn(mockQuery).when(mockBuilder).createQuery(Long.class);

    doReturn(mockPredicate).when(mockBuilder).equal(mockPath, claimId);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(expected).when(mockTypedQuery).getSingleResult();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    doNothing().when(daoSpy).logQueryMetric(any(), anyString(), anyLong(), anyInt());

    Object actual = daoSpy.getEntityById(claimType, claimId);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockPredicate);
    verify(daoSpy, times(1))
        .logQueryMetric(
            same(claimType),
            eq(ClaimDao.CLAIM_BY_ID_QUERY),
            timeCaptor.capture(),
            sizeCaptor.capture());

    assertEquals(1, sizeCaptor.getValue());
    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#getEntityById(ResourceTypeV2, String)} builds the correct query
   * to return the expected null value when no entity found using a given ID.
   */
  @Test
  void shouldGetEntityByIdWhenNull() {
    String claimId = "123";

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, true));

    CriteriaQuery<?> mockQuery = mock(CriteriaQuery.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    Root mockRoot = mock(Root.class);

    Predicate mockPredicate = mock(Predicate.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    Path mockPath = mock(Path.class);

    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    TypedQuery mockTypedQuery = mock(TypedQuery.class);

    doReturn(mockPath).when(mockRoot).get(claimType.getEntityIdAttribute());

    doReturn(mockRoot).when(mockQuery).from(Long.class);

    doReturn(mockQuery).when(mockBuilder).createQuery(Long.class);

    doReturn(mockPredicate).when(mockBuilder).equal(mockPath, claimId);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(null).when(mockTypedQuery).getSingleResult();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    doNothing().when(daoSpy).logQueryMetric(any(), anyString(), anyLong(), anyInt());

    Object actual = daoSpy.getEntityById(claimType, claimId);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockPredicate);
    verify(daoSpy, times(1))
        .logQueryMetric(
            same(claimType),
            eq(ClaimDao.CLAIM_BY_ID_QUERY),
            timeCaptor.capture(),
            sizeCaptor.capture());

    assertEquals(0, sizeCaptor.getValue());

    assertNull(actual);
  }

  /**
   * Verifies that {@link ClaimDao#logQueryMetric(ResourceTypeV2, String, long, int)} was invoked
   * with the correct return size of the query.
   */
  @Test
  void shouldSetClaimByMbiMetricForClaimsSearch() {
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = false;

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    CriteriaQuery<Object> mockQuery = mock(CriteriaQuery.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Root<Object> mockRoot = mock(Root.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    TypedQuery mockTypedQuery = mock(TypedQuery.class);

    doReturn(null)
        .when(daoSpy)
        .createMbiPredicate(any(), anyString(), anyBoolean(), any(CriteriaBuilder.class));

    doReturn(mockRoot).when(mockQuery).from(claimType.getEntityClass());

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(mockQuery).when(mockBuilder).createQuery(any());

    List<Long> mockList = List.of(1L, 1L, 1L, 1L, 1L);

    doReturn(mockList).when(mockTypedQuery).getResultList();

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(any(CriteriaQuery.class));

    doNothing().when(daoSpy).logQueryMetric(any(), anyString(), anyLong(), anyInt());

    daoSpy.findAllByMbiAttribute(claimType, mbiSearchValue, isMbiSearchValueHashed, null, null);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(daoSpy, times(1))
        .logQueryMetric(
            same(claimType),
            eq(ClaimDao.CLAIM_BY_MBI_QUERY),
            timeCaptor.capture(),
            sizeCaptor.capture());

    assertEquals(5, sizeCaptor.getValue());
  }

  /**
   * Verifies that {@link ClaimDao#logQueryMetric(ResourceTypeV2, String, long, int)} was invoked
   * with size set to 0 if the query result was null.
   */
  @Test
  void shouldSetClaimByMbiMetricForNullClaimsSearch() {
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = false;

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    CriteriaQuery<Object> mockQuery = mock(CriteriaQuery.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Root<Object> mockRoot = mock(Root.class);
    // rawtypes - Due to mocking the object.
    //noinspection rawtypes
    TypedQuery mockTypedQuery = mock(TypedQuery.class);

    doReturn(null)
        .when(daoSpy)
        .createMbiPredicate(any(), anyString(), anyBoolean(), any(CriteriaBuilder.class));

    doReturn(mockRoot).when(mockQuery).from(claimType.getEntityClass());

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(mockQuery).when(mockBuilder).createQuery(any());

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(any(CriteriaQuery.class));

    doNothing().when(daoSpy).logQueryMetric(any(), anyString(), anyLong(), anyInt());

    daoSpy.findAllByMbiAttribute(claimType, mbiSearchValue, isMbiSearchValueHashed, null, null);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(daoSpy, times(1))
        .logQueryMetric(
            same(claimType),
            eq(ClaimDao.CLAIM_BY_MBI_QUERY),
            timeCaptor.capture(),
            sizeCaptor.capture());

    assertEquals(0, sizeCaptor.getValue());
  }

  /**
   * Verify that {@link ClaimDao#createServiceDatePredicates} handles cases with one and two service
   * date attribute names properly.
   */
  @Test
  @SuppressWarnings("unchecked") // untyped mock creation is harmless
  public void testCreateServiceDatePredicates() {
    final var daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, true));
    final Root<RdaFissClaim> root = mock(Root.class);
    final var serviceDateParam = mock(DateRangeParam.class);

    final Path<LocalDate> fromDatePath = mock(Path.class);
    doReturn(fromDatePath).when(root).get("from");

    final Path<LocalDate> toDatePath = mock(Path.class);
    doReturn(toDatePath).when(root).get("to");

    final var fromDatePredicate = mock(Predicate.class);
    doReturn(fromDatePredicate)
        .when(daoSpy)
        .serviceDatePredicate(mockBuilder, serviceDateParam, fromDatePath);

    final var toDatePredicate = mock(Predicate.class);
    doReturn(toDatePredicate)
        .when(daoSpy)
        .serviceDatePredicate(mockBuilder, serviceDateParam, toDatePath);

    final var fromDateResult = mock(Predicate.class);
    doReturn(fromDateResult).when(mockBuilder).or(new Predicate[] {fromDatePredicate});

    final var bothDatesResult = mock(Predicate.class);
    doReturn(bothDatesResult)
        .when(mockBuilder)
        .or(new Predicate[] {fromDatePredicate, toDatePredicate});

    assertSame(
        fromDateResult,
        daoSpy.createServiceDatePredicates(mockBuilder, root, serviceDateParam, List.of("from")));
    assertSame(
        bothDatesResult,
        daoSpy.createServiceDatePredicates(
            mockBuilder, root, serviceDateParam, List.of("from", "to")));
  }

  /**
   * Generates parameters for {@link ClaimDaoTest#testMbiLookup}.
   *
   * @return all test parameters
   */
  private static Stream<MbiLookupTestParameter<?, ?>> getMbiLookupParameters() {
    return Stream.of(
        new MbiLookupTestParameter<>(
            "FISS", ClaimTypeV2.F, LastUpdated, ServiceDate, RdaFissClaim::new),
        new MbiLookupTestParameter<>(
            "MCS - without serviceDate", ClaimTypeV2.M, LastUpdated, null, RdaMcsClaim::new),
        new MbiLookupTestParameter<>(
            "MCS - with serviceDate", ClaimTypeV2.M, LastUpdated, ServiceDate, RdaMcsClaim::new));
  }

  /**
   * Test the {@link ClaimDao#findAllByMbiAttribute} method.
   *
   * @param param defines the specific test case
   * @param <TResource> FHIR resource type
   * @param <TEntity> JPA entity type
   */
  @ParameterizedTest()
  @MethodSource("getMbiLookupParameters")
  @SuppressWarnings("unchecked") // untyped mock creation is harmless
  <TResource extends IBaseResource, TEntity> void testMbiLookup(
      MbiLookupTestParameter<TResource, TEntity> param) {
    final var resourceType = param.resourceType;
    final String mbiSearchValue = "find-me";
    final boolean isMbiSearchValueHashed = false;
    final DateRangeParam lastUpdated = param.lastUpdated;
    final DateRangeParam serviceDate = param.serviceDate;
    final CriteriaQuery<TEntity> claimsQuery = mock(CriteriaQuery.class);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();
    doReturn(claimsQuery).when(mockBuilder).createQuery(resourceType.getEntityClass());

    final Root<TEntity> claim = mock(Root.class);
    doReturn(claim).when(claimsQuery).from(resourceType.getEntityClass());

    final ClaimDao dao = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    final Predicate wherePredicate = mock(Predicate.class);
    doReturn(List.of(wherePredicate))
        .when(dao)
        .createStandardPredicatesForMbiLookup(
            mockBuilder,
            claim,
            resourceType,
            mbiSearchValue,
            isMbiSearchValueHashed,
            lastUpdated,
            serviceDate);

    final Path<?> claimId = mock(Path.class);
    doReturn(claimId).when(claim).get(resourceType.getEntityIdAttribute());

    final Order sortOrder = mock(Order.class);
    doReturn(sortOrder).when(mockBuilder).asc(claimId);

    final TypedQuery<TEntity> query = mock(TypedQuery.class);
    doReturn(query).when(mockEntityManager).createQuery(claimsQuery);

    final List<TEntity> queryResult =
        List.of(
            param.instanceFactory.get(), param.instanceFactory.get(), param.instanceFactory.get());
    doReturn(queryResult).when(query).getResultList();

    final List<TEntity> result =
        dao.findAllByMbiAttribute(
            resourceType, mbiSearchValue, isMbiSearchValueHashed, lastUpdated, serviceDate);
    assertEquals(queryResult, result);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(dao)
        .logQueryMetric(
            same(resourceType),
            eq(ClaimDao.CLAIM_BY_MBI_QUERY),
            timeCaptor.capture(),
            sizeCaptor.capture());

    assertEquals(3, sizeCaptor.getValue());

    verify(claimsQuery).select(claim);
    verify(claimsQuery).where(new Predicate[] {wherePredicate});
    verify(claimsQuery).orderBy(sortOrder);
  }

  /**
   * Generates parameters for {@link ClaimDaoTest#testCreateStandardDateRangePredicateList}.
   *
   * @return all test parameters
   */
  private static Stream<DateRangeTestParameter>
      getCreateStandardDateRangePredicateListParameters() {
    return Stream.of(
        new DateRangeTestParameter("neither", null, null),
        new DateRangeTestParameter("lastUpdated only", LastUpdated, null),
        new DateRangeTestParameter("serviceDate only", null, ServiceDate),
        new DateRangeTestParameter("both", LastUpdated, ServiceDate));
  }

  /**
   * Test the {@link ClaimDao#createStandardPredicatesForMbiLookup} method.
   *
   * @param param defines the specific test case
   */
  @ParameterizedTest()
  @MethodSource("getCreateStandardDateRangePredicateListParameters")
  @SuppressWarnings("unchecked") // untyped mock creation is harmless
  void testCreateStandardDateRangePredicateList(DateRangeTestParameter param) {
    final Root<RdaFissClaim> claim = mock(Root.class);
    final var resourceType = ClaimTypeV2.F;
    final String mbiSearchValue = "hash";
    final boolean isMbiSearchValueHashed = true;
    final DateRangeParam lastUpdated = param.lastUpdated;
    final DateRangeParam serviceDate = param.serviceDate;

    final Path<?> mbiRecord = mock(Path.class);
    doReturn(mbiRecord).when(claim).get(resourceType.getEntityMbiRecordAttribute());

    final ClaimDao dao = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    final List<Predicate> expectedPredicates = new ArrayList<>();

    final Predicate mbiPredicate = mock(Predicate.class);
    doReturn(mbiPredicate)
        .when(dao)
        .createMbiPredicate(mbiRecord, mbiSearchValue, isMbiSearchValueHashed, mockBuilder);
    expectedPredicates.add(mbiPredicate);

    final Predicate lastUpdatedPredicate = mock(Predicate.class);
    if (lastUpdated != null) {
      doReturn(lastUpdatedPredicate)
          .when(dao)
          .lastUpdatedPredicate(claim, lastUpdated, mockBuilder);
      expectedPredicates.add(lastUpdatedPredicate);
    }

    final Predicate serviceDatePredicate = mock(Predicate.class);
    if (serviceDate != null) {
      doReturn(serviceDatePredicate)
          .when(dao)
          .createServiceDatePredicates(
              same(mockBuilder),
              same(claim),
              same(serviceDate),
              same(resourceType.getEntityServiceDateAttributes()));
      expectedPredicates.add(serviceDatePredicate);
    }

    final List<Predicate> result =
        dao.createStandardPredicatesForMbiLookup(
            mockBuilder,
            claim,
            resourceType,
            mbiSearchValue,
            isMbiSearchValueHashed,
            lastUpdated,
            serviceDate);

    assertEquals(expectedPredicates, result);
  }

  /** Test the {@link ClaimDao#createMbiPredicate} method for the case of searching with an MBI. */
  @Test
  void testCreateMbiPredicateForMbi() {
    final Path<?> root = mock(Path.class);
    final Path<?> mbi = mock(Path.class);
    final String searchString = "mbi";
    final Predicate mbiPredicate = mock(Predicate.class);

    doReturn(mbiPredicate).when(mockBuilder).equal(mbi, searchString);
    doReturn(mbi).when(root).get(Mbi.Fields.mbi);

    final ClaimDao dao = spy(new ClaimDao(mockEntityManager, metricRegistry, false));
    assertSame(mbiPredicate, dao.createMbiPredicate(root, searchString, false, mockBuilder));
  }

  /**
   * Test the {@link ClaimDao#createMbiPredicate} method for the case of searching with a hashed MBI
   * and old hash support disabled.
   */
  @Test
  void testCreateMbiPredicateForHash() {
    final Path<?> root = mock(Path.class);
    final Path<?> hash = mock(Path.class);
    final String searchString = "hashed";
    final Predicate hashPredicate = mock(Predicate.class);

    doReturn(hashPredicate).when(mockBuilder).equal(hash, searchString);
    doReturn(hash).when(root).get(Mbi.Fields.hash);

    final ClaimDao dao = spy(new ClaimDao(mockEntityManager, metricRegistry, false));
    assertSame(hashPredicate, dao.createMbiPredicate(root, searchString, true, mockBuilder));
  }

  /**
   * Test the {@link ClaimDao#createMbiPredicate} method for the case of searching with a hashed MBI
   * and old hash support enabled.
   */
  @Test
  void testCreateMbiPredicateForHashOrOldHash() {
    final Path<?> root = mock(Path.class);
    final Path<?> hash = mock(Path.class);
    final Path<?> oldHash = mock(Path.class);
    final String searchString = "hashed";
    final Predicate hashPredicate = mock(Predicate.class);
    final Predicate oldHashPredicate = mock(Predicate.class);
    final Predicate combinedPredicate = mock(Predicate.class);

    doReturn(hashPredicate).when(mockBuilder).equal(hash, searchString);
    doReturn(oldHashPredicate).when(mockBuilder).equal(oldHash, searchString);
    doReturn(combinedPredicate).when(mockBuilder).or(hashPredicate, oldHashPredicate);
    doReturn(hash).when(root).get(Mbi.Fields.hash);
    doReturn(oldHash).when(root).get(Mbi.Fields.oldHash);

    final ClaimDao dao = spy(new ClaimDao(mockEntityManager, metricRegistry, true));
    assertSame(combinedPredicate, dao.createMbiPredicate(root, searchString, true, mockBuilder));
  }

  /** A helper class to use for testing methods in place of actual resources. */
  private static class MockClaimType extends AbstractResourceTypeV2<IBaseResource, Long> {
    /** A mock claim type for testing. */
    public MockClaimType() {
      super(
          "mock",
          "mockType",
          Long.class,
          "mbiAttribute",
          "somePropertyName",
          List.of("endDateAttribute"));
    }
  }

  /**
   * Parameter object defining a test case for {@link ClaimDaoTest#testMbiLookup}.
   *
   * @param <TResource> FHIR resource type
   * @param <TEntity> JPA entity type
   */
  @AllArgsConstructor
  private static class MbiLookupTestParameter<TResource extends IBaseResource, TEntity> {
    /** Short name to identify the test case in log output. */
    private final String testName;

    /** Valid {@link ResourceTypeV2} instance being looked up. */
    private final ResourceTypeV2<TResource, TEntity> resourceType;

    /** Value to pass as lastUpdated parameter. */
    private final DateRangeParam lastUpdated;

    /** Value to pass as serviceDate parameter. */
    private final DateRangeParam serviceDate;

    /** Constructor to create sample instance of entity class. */
    private final Supplier<TEntity> instanceFactory;

    @Override
    public String toString() {
      return testName;
    }
  }

  /**
   * Parameter object defining a test case for {@link
   * ClaimDaoTest#testCreateStandardDateRangePredicateList}.
   */
  @AllArgsConstructor
  private static class DateRangeTestParameter {
    /** Short name to identify the test case in log output. */
    private final String testName;

    /** Value to pass as lastUpdated parameter. */
    private final DateRangeParam lastUpdated;

    /** Value to pass as serviceDate parameter. */
    private final DateRangeParam serviceDate;

    @Override
    public String toString() {
      return testName;
    }
  }
}
