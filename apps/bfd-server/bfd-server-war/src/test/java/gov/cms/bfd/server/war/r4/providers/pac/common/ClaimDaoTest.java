package gov.cms.bfd.server.war.r4.providers.pac.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.Mbi;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClaimDaoTest {

  /**
   * Verifies that the {@link ClaimDao#getEntityById(ResourceTypeV2, String)} method forwards a
   * request to the overloaded {@link ClaimDao#getEntityById(Class, String, String)} method.
   */
  @Test
  void shouldInvokeGetByIdForClaimType() {
    String claimId = "123";

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    Object expected = 5L;

    ResourceTypeV2<IBaseResource> mockClaimType = new MockClaimType();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry, true));

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    doReturn(null).when(daoSpy).getEntityById(any(Class.class), anyString(), anyString());

    doReturn(expected)
        .when(daoSpy)
        .getEntityById(
            mockClaimType.getEntityClass(), mockClaimType.getEntityIdAttribute(), claimId);

    Object actual = daoSpy.getEntityById(mockClaimType, claimId);

    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#getEntityById(Class, String, String)} builds the correct query to
   * return the expected entity using a given ID.
   */
  @Test
  void shouldGetEntityById() {
    String idAttributeName = "someAttribute";
    String claimId = "123";

    Object expected = 5L;

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry, true));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
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

    doReturn(mockPath).when(mockRoot).get(idAttributeName);

    doReturn(mockRoot).when(mockQuery).from(Long.class);

    doReturn(mockQuery).when(mockBuilder).createQuery(Long.class);

    doReturn(mockPredicate).when(mockBuilder).equal(mockPath, claimId);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(expected).when(mockTypedQuery).getSingleResult();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    doNothing().when(daoSpy).logQueryMetric(anyLong(), anyInt());

    Object actual = daoSpy.getEntityById(Long.class, idAttributeName, claimId);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockPredicate);
    verify(daoSpy, times(1)).logQueryMetric(timeCaptor.capture(), sizeCaptor.capture());

    assertEquals(1, sizeCaptor.getValue());
    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#getEntityById(Class, String, String)} builds the correct query to
   * return the expected null value when no entity found using a given ID.
   */
  @Test
  void shouldGetEntityByIdWhenNull() {
    String idAttributeName = "someAttribute";
    String claimId = "123";

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry, true));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
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

    doReturn(mockPath).when(mockRoot).get(idAttributeName);

    doReturn(mockRoot).when(mockQuery).from(Long.class);

    doReturn(mockQuery).when(mockBuilder).createQuery(Long.class);

    doReturn(mockPredicate).when(mockBuilder).equal(mockPath, claimId);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(null).when(mockTypedQuery).getSingleResult();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    doNothing().when(daoSpy).logQueryMetric(anyLong(), anyInt());

    Object actual = daoSpy.getEntityById(Long.class, idAttributeName, claimId);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockPredicate);
    verify(daoSpy, times(1)).logQueryMetric(timeCaptor.capture(), sizeCaptor.capture());

    assertEquals(0, sizeCaptor.getValue());

    assertNull(actual);
  }

  /**
   * Verifies that {@link ClaimDao#findAllByMbiAttribute(Class, String, String, boolean,
   * DateRangeParam, DateRangeParam, String, String)} builds the correct query to find dall entities
   * by a given MBI.
   */
  @Test
  void shouldFindEntitiesByMbi() {
    final Class<Object> entityClass = Object.class;
    final String mbiRecordAttributeName = "attr";
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = false;
    final String mbiValueAttributeName = Mbi.Fields.mbi;
    final String idAttribute = "idAttribute";
    final String endAttribute = "endAttribute";

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    CriteriaQuery<Object> mockQuery = mock(CriteriaQuery.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Root<Object> mockRoot = mock(Root.class);

    Predicate mockEmptyAndPredicate = mock(Predicate.class);
    Predicate mockAndPredicate = mock(Predicate.class);
    Predicate mockEqualsPredicate = mock(Predicate.class);

    Path<?> mockPathToMbiRecord = mock(Path.class);
    Path<?> mockPathToMbiField = mock(Path.class);

    doReturn(mockEqualsPredicate).when(mockBuilder).equal(mockPathToMbiField, mbiSearchValue);

    doReturn(null).when(mockBuilder).and(mockEqualsPredicate, null);

    doReturn(mockAndPredicate)
        .when(mockBuilder)
        .and(mockEqualsPredicate, mockEmptyAndPredicate, mockEmptyAndPredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(null)
        .when(daoSpy)
        .serviceDateRangePredicate(
            any(Root.class), any(DateRangeParam.class), any(CriteriaBuilder.class), anyString());

    doReturn(mockPathToMbiRecord).when(mockRoot).get(mbiRecordAttributeName);
    doReturn(mockPathToMbiField).when(mockPathToMbiRecord).get(mbiValueAttributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByMbiAttribute(
            entityClass,
            mbiRecordAttributeName,
            mbiSearchValue,
            isMbiSearchValueHashed,
            null,
            null,
            idAttribute,
            endAttribute);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#findAllByMbiAttribute(Class, String, String, boolean,
   * DateRangeParam, DateRangeParam, String, String)} builds the correct query to find dall entities
   * by a given old MBI hash.
   */
  @Test
  void shouldFindEntitiesByOldMbiHash() {
    final Class<Object> entityClass = Object.class;
    final String mbiRecordAttributeName = "attr";
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = true;
    final String mbiHashAttributeName = Mbi.Fields.hash;
    final String oldMbiHashAttributeName = Mbi.Fields.oldHash;
    final String idAttribute = "idAttribute";
    final String endAttribute = "endAttribute";

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, true));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    CriteriaQuery<Object> mockQuery = mock(CriteriaQuery.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Root<Object> mockRoot = mock(Root.class);

    Predicate mockEmptyAndPredicate = mock(Predicate.class);
    Predicate mockAndPredicate = mock(Predicate.class);
    Predicate mockHashEqualsPredicate = mock(Predicate.class);
    Predicate mockOldHashEqualsPredicate = mock(Predicate.class);
    Predicate mockCombinedMbiPredicate = mock(Predicate.class);

    Path<?> mockPathToMbiRecord = mock(Path.class);
    Path<?> mockPathToMbiHashField = mock(Path.class);
    Path<?> mockPathToOldMbiHashField = mock(Path.class);

    doReturn(mockHashEqualsPredicate)
        .when(mockBuilder)
        .equal(mockPathToMbiHashField, mbiSearchValue);
    doReturn(mockOldHashEqualsPredicate)
        .when(mockBuilder)
        .equal(mockPathToOldMbiHashField, mbiSearchValue);
    doReturn(mockCombinedMbiPredicate)
        .when(mockBuilder)
        .or(mockHashEqualsPredicate, mockOldHashEqualsPredicate);

    doReturn(null).when(mockBuilder).and(mockCombinedMbiPredicate, null);

    doReturn(mockAndPredicate)
        .when(mockBuilder)
        .and(mockCombinedMbiPredicate, mockEmptyAndPredicate, mockEmptyAndPredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(null)
        .when(daoSpy)
        .serviceDateRangePredicate(
            any(Root.class), any(DateRangeParam.class), any(CriteriaBuilder.class), anyString());

    doReturn(mockPathToMbiRecord).when(mockRoot).get(mbiRecordAttributeName);
    doReturn(mockPathToMbiHashField).when(mockPathToMbiRecord).get(mbiHashAttributeName);
    doReturn(mockPathToOldMbiHashField).when(mockPathToMbiRecord).get(oldMbiHashAttributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByMbiAttribute(
            entityClass,
            mbiRecordAttributeName,
            mbiSearchValue,
            isMbiSearchValueHashed,
            null,
            null,
            idAttribute,
            endAttribute);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#findAllByMbiAttribute(Class, String, String, boolean,
   * DateRangeParam, DateRangeParam, String, String)} builds the correct query to find all entities
   * by a given MBI and lastUpdated value.
   */
  @Test
  void shouldFindEntitiesByMbiHashAndLastUpdated() {
    final Class<Object> entityClass = Object.class;
    final String mbiRecordAttributeName = "attr";
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = true;
    final String mbiValueAttributeName = Mbi.Fields.hash;
    final String idAttribute = "idAttribute";
    final String endAttribute = "endAttribute";
    final DateRangeParam mockLastUpdatedParam = mock(DateRangeParam.class);
    final DateRangeParam mockServiceDateParam = mock(DateRangeParam.class);

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    CriteriaQuery<Object> mockQuery = mock(CriteriaQuery.class);
    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Root<Object> mockRoot = mock(Root.class);

    Predicate mockEmptyAndPredicate = mock(Predicate.class);
    Predicate mockAndPredicate = mock(Predicate.class);
    Predicate mockEqualsPredicate = mock(Predicate.class);

    Path<?> mockPathToMbiRecord = mock(Path.class);
    Path<?> mockPathToMbiField = mock(Path.class);

    doReturn(mockEqualsPredicate).when(mockBuilder).equal(mockPathToMbiField, mbiSearchValue);

    Predicate mockLastUpdatedPredicate = mock(Predicate.class);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Path<Instant> mockLastUpdatedPath = mock(Path.class);

    doReturn(null)
        .when(daoSpy)
        .createDateRangePredicate(
            any(Root.class), any(DateRangeParam.class), any(CriteriaBuilder.class));

    doReturn(mockLastUpdatedPredicate)
        .when(daoSpy)
        .createDateRangePredicate(mockRoot, mockLastUpdatedParam, mockBuilder);

    Predicate mockServiceDatePredicate = mock(Predicate.class);

    doReturn(null)
        .when(daoSpy)
        .serviceDateRangePredicate(
            any(Root.class), any(DateRangeParam.class), any(CriteriaBuilder.class), anyString());

    doReturn(mockServiceDatePredicate)
        .when(daoSpy)
        .serviceDateRangePredicate(mockRoot, mockServiceDateParam, mockBuilder, endAttribute);

    doReturn(mockLastUpdatedPath).when(mockRoot).get("lastUpdated");

    doReturn(null).when(mockBuilder).and(mockEqualsPredicate, null);

    doReturn(mockAndPredicate)
        .when(mockBuilder)
        .and(mockEqualsPredicate, mockLastUpdatedPredicate, mockServiceDatePredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(mockPathToMbiRecord).when(mockRoot).get(mbiRecordAttributeName);
    doReturn(mockPathToMbiField).when(mockPathToMbiRecord).get(mbiValueAttributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByMbiAttribute(
            entityClass,
            mbiRecordAttributeName,
            mbiSearchValue,
            isMbiSearchValueHashed,
            mockLastUpdatedParam,
            mockServiceDateParam,
            idAttribute,
            endAttribute);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  /**
   * Verifies that {@link ClaimDao#logQueryMetric(long, int)} was invoked with the correct return
   * size of the query.
   */
  @Test
  void shouldSetClaimByMbiMetricForClaimsSearch() {
    final Class<Object> entityClass = Object.class;
    final String mbiRecordAttributeName = "attr";
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = false;
    final String idAttribute = "idAttribute";
    final String endAttribute = "endAttribute";

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
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
        .createMbiPredicate(
            any(), anyString(), anyBoolean(), anyBoolean(), any(CriteriaBuilder.class));

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(mockQuery).when(mockBuilder).createQuery(any());

    List<Object> mockList = List.of(1, 1, 1, 1, 1);

    doReturn(mockList).when(mockTypedQuery).getResultList();

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(any(CriteriaQuery.class));

    doNothing().when(daoSpy).logQueryMetric(anyLong(), anyInt());

    daoSpy.findAllByMbiAttribute(
        entityClass,
        mbiRecordAttributeName,
        mbiSearchValue,
        isMbiSearchValueHashed,
        null,
        null,
        idAttribute,
        endAttribute);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(daoSpy, times(1)).logQueryMetric(timeCaptor.capture(), sizeCaptor.capture());

    assertEquals(5, sizeCaptor.getValue());
  }

  /**
   * Verifies that {@link ClaimDao#logQueryMetric(long, int)} was invoked with size set to 0 if the
   * query result was null.
   */
  @Test
  void shouldSetClaimByMbiMetricForNullClaimsSearch() {
    final Class<Object> entityClass = Object.class;
    final String mbiRecordAttributeName = "attr";
    final String mbiSearchValue = "value";
    final boolean isMbiSearchValueHashed = false;
    final String idAttribute = "idAttribute";
    final String endAttribute = "endAttribute";

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry, false));

    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);
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
        .createMbiPredicate(
            any(), anyString(), anyBoolean(), anyBoolean(), any(CriteriaBuilder.class));

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    doReturn(mockQuery).when(mockBuilder).createQuery(any());

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(any(CriteriaQuery.class));

    doNothing().when(daoSpy).logQueryMetric(anyLong(), anyInt());

    daoSpy.findAllByMbiAttribute(
        entityClass,
        mbiRecordAttributeName,
        mbiSearchValue,
        isMbiSearchValueHashed,
        null,
        null,
        idAttribute,
        endAttribute);

    ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(daoSpy, times(1)).logQueryMetric(timeCaptor.capture(), sizeCaptor.capture());

    assertEquals(0, sizeCaptor.getValue());
  }

  /** A helper class to use for testing methods in place of actual resources. */
  private static class MockClaimType implements ResourceTypeV2<IBaseResource> {

    @Override
    public Class<?> getEntityClass() {
      return Long.class;
    }

    @Override
    public String getEntityIdAttribute() {
      return "somePropertyName";
    }

    @Override
    public String getEntityMbiRecordAttribute() {
      return "mbiAttribute";
    }

    @Override
    public String getEntityEndDateAttribute() {
      return "endDeAttribute";
    }

    @Override
    public ResourceTransformer<IBaseResource> getTransformer() {
      return null;
    }
  }
}
