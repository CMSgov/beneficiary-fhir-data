package gov.cms.bfd.server.war.r4.providers.preadj.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import com.codahale.metrics.MetricRegistry;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;

public class ClaimDaoTest {

  @Test
  public void shouldInvokeGetByIdForClaimType() {
    String claimId = "123";

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    Object expected = 5L;

    ResourceTypeV2<IBaseResource> mockClaimType = new MockClaimType();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry));

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

  @Test
  public void shouldGetEntityById() {
    String idAttributeName = "someAttribute";
    String claimId = "123";

    Object expected = 5L;

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry));

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

    Object actual = daoSpy.getEntityById(Long.class, idAttributeName, claimId);

    // unchecked - This is fine for the purposes of this test.
    //noinspection unchecked
    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockPredicate);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldInvokeFindEntitiesByAttributeForMbi() {
    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    DateRangeParam mockDateRangeParam = mock(DateRangeParam.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, mockRegistry));

    List<Object> expected = Collections.singletonList(5L);

    doReturn(null)
        .when(daoSpy)
        .findAllByAttribute(any(), anyString(), anyString(), any(DateRangeParam.class));

    doReturn(expected)
        .when(daoSpy)
        .findAllByAttribute(Object.class, "mbi", "123", mockDateRangeParam);

    List<Object> actual = daoSpy.findAllByMbi(Object.class, "123", mockDateRangeParam);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldInvokeFindEntitiesByAttributeForMbiHash() {
    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    DateRangeParam mockDateRangeParam = mock(DateRangeParam.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, mockRegistry));

    List<Object> expected = Collections.singletonList(5L);

    doReturn(null)
        .when(daoSpy)
        .findAllByAttribute(any(), anyString(), anyString(), any(DateRangeParam.class));

    doReturn(expected)
        .when(daoSpy)
        .findAllByAttribute(Object.class, "mbiHash", "123", mockDateRangeParam);

    List<Object> actual = daoSpy.findAllByMbiHash(Object.class, "123", mockDateRangeParam);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldFindEntitiesByAttribute() {
    Class<Object> entityClass = Object.class;
    String attributeName = "attr";
    String attributeValue = "value";

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry));

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

    Path<?> mockPath = mock(Path.class);

    doReturn(mockEqualsPredicate).when(mockBuilder).equal(mockPath, attributeValue);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(null)
        .when(daoSpy)
        .createDateRangePredicate(
            any(Path.class), any(DateRangeParam.class), any(CriteriaBuilder.class));

    doReturn(null).when(mockBuilder).and(mockEqualsPredicate, null);

    doReturn(mockAndPredicate).when(mockBuilder).and(mockEqualsPredicate, mockEmptyAndPredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(mockPath).when(mockRoot).get(attributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByAttribute(entityClass, attributeName, attributeValue, null);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldFindEntitiesByAttributeAndLastUpdated() {
    Class<Object> entityClass = Object.class;
    String attributeName = "attr";
    String attributeValue = "value";
    DateRangeParam mockDateRangeParam = mock(DateRangeParam.class);

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry));

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

    Path<?> mockPath = mock(Path.class);

    doReturn(mockEqualsPredicate).when(mockBuilder).equal(mockPath, attributeValue);

    Predicate mockDateRangePredicate = mock(Predicate.class);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Path<Instant> mockLastUpdatedPath = mock(Path.class);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(null)
        .when(daoSpy)
        .createDateRangePredicate(
            any(Path.class), any(DateRangeParam.class), any(CriteriaBuilder.class));

    doReturn(mockDateRangePredicate)
        .when(daoSpy)
        .createDateRangePredicate(mockLastUpdatedPath, mockDateRangeParam, mockBuilder);

    doReturn(mockLastUpdatedPath).when(mockRoot).get("lastUpdated");

    doReturn(null).when(mockBuilder).and(mockEqualsPredicate, null);

    doReturn(mockAndPredicate).when(mockBuilder).and(mockEqualsPredicate, mockDateRangePredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(mockPath).when(mockRoot).get(attributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByAttribute(entityClass, attributeName, attributeValue, mockDateRangeParam);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldCreateDateRangePredicateIfValidDateRange() {
    long toEpoch = 5L;
    long fromEpoch = 2L;

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry));

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Path<Instant> mockPath = mock(Path.class);
    DateRangeParam mockDateRangeParam = mock(DateRangeParam.class);
    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);

    Instant to = Instant.ofEpochMilli(toEpoch);
    Instant from = Instant.ofEpochMilli(fromEpoch);

    Date mockUpperBound = mock(Date.class);
    Date mockLowerBound = mock(Date.class);

    doReturn(to).when(mockUpperBound).toInstant();

    doReturn(from).when(mockLowerBound).toInstant();

    doReturn(mockUpperBound).when(mockDateRangeParam).getUpperBoundAsInstant();

    doReturn(mockLowerBound).when(mockDateRangeParam).getLowerBoundAsInstant();

    Predicate mockFromPredicate = mock(Predicate.class);
    Predicate mockToPredicate = mock(Predicate.class);

    DateParam mockLowerDateParam = mock(DateParam.class);
    DateParam mockUpperDateParam = mock(DateParam.class);

    ParamPrefixEnum lowerPrefix = ParamPrefixEnum.GREATERTHAN;
    ParamPrefixEnum upperPrefix = ParamPrefixEnum.LESSTHAN_OR_EQUALS;

    doReturn(lowerPrefix).when(mockLowerDateParam).getPrefix();

    doReturn(upperPrefix).when(mockUpperDateParam).getPrefix();

    doReturn(mockLowerDateParam).when(mockDateRangeParam).getLowerBound();

    doReturn(mockUpperDateParam).when(mockDateRangeParam).getUpperBound();

    doReturn(mockFromPredicate).when(mockBuilder).greaterThan(mockPath, from);

    doReturn(mockToPredicate).when(mockBuilder).lessThanOrEqualTo(mockPath, to);

    Predicate expected = mock(Predicate.class);

    doReturn(expected).when(mockBuilder).and(mockFromPredicate, mockToPredicate);

    Predicate actual = daoSpy.createDateRangePredicate(mockPath, mockDateRangeParam, mockBuilder);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldCreateDateRangePredicateIfNoDateRange() {
    // The ClaimDao class considers this the maximum upper bound
    long toEpoch = 253370765800000L;
    long fromEpoch = Long.MIN_VALUE;

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry metricRegistry = new MetricRegistry();

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, metricRegistry));

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    Path<Instant> mockPath = mock(Path.class);
    DateRangeParam mockDateRangeParam = mock(DateRangeParam.class);
    CriteriaBuilder mockBuilder = mock(CriteriaBuilder.class);

    Instant to = Instant.ofEpochMilli(toEpoch);
    Instant from = Instant.ofEpochMilli(fromEpoch);

    doReturn(null).when(mockDateRangeParam).getUpperBoundAsInstant();

    doReturn(null).when(mockDateRangeParam).getLowerBoundAsInstant();

    Predicate mockFromPredicate = mock(Predicate.class);
    Predicate mockToPredicate = mock(Predicate.class);

    doReturn(null).when(mockDateRangeParam).getLowerBound();

    doReturn(null).when(mockDateRangeParam).getUpperBound();

    doReturn(mockFromPredicate).when(mockBuilder).greaterThanOrEqualTo(mockPath, from);

    doReturn(mockToPredicate).when(mockBuilder).lessThan(mockPath, to);

    Predicate expected = mock(Predicate.class);

    doReturn(expected).when(mockBuilder).and(mockFromPredicate, mockToPredicate);

    Predicate actual = daoSpy.createDateRangePredicate(mockPath, mockDateRangeParam, mockBuilder);

    assertEquals(expected, actual);
  }

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
    public ResourceTransformer<IBaseResource> getTransformer() {
      return null;
    }
  }
}
