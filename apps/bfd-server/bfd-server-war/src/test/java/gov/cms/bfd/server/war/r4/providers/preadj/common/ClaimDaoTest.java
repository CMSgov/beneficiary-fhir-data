package gov.cms.bfd.server.war.r4.providers.preadj.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
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
  public void shouldFindEntitiesByAttribute() {
    Class<Object> entityClass = Object.class;
    String attributeName = "attr";
    String attributeValue = "value";
    String endAttribute = "endAttribute";

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

    doReturn(null).when(mockBuilder).and(mockEqualsPredicate, null);

    doReturn(mockAndPredicate)
        .when(mockBuilder)
        .and(mockEqualsPredicate, mockEmptyAndPredicate, mockEmptyAndPredicate);

    doReturn(mockEmptyAndPredicate).when(mockBuilder).and();

    doReturn(null)
        .when(daoSpy)
        .serviceDateRangePredicate(
            any(Root.class), any(DateRangeParam.class), any(CriteriaBuilder.class), anyString());

    doReturn(mockPath).when(mockRoot).get(attributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByAttribute(
            entityClass, attributeName, attributeValue, null, null, endAttribute);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
    assertEquals(expected, actual);
  }

  @Test
  public void shouldFindEntitiesByAttributeAndLastUpdated() {
    Class<Object> entityClass = Object.class;
    String attributeName = "attr";
    String attributeValue = "value";
    String endAttribute = "endAttribute";
    DateRangeParam mockLastUpdatedParam = mock(DateRangeParam.class);
    DateRangeParam mockServiceDateParam = mock(DateRangeParam.class);

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

    doReturn(mockPath).when(mockRoot).get(attributeName);

    doReturn(mockRoot).when(mockQuery).from(entityClass);

    doReturn(mockQuery).when(mockBuilder).createQuery(entityClass);

    doReturn(mockBuilder).when(mockEntityManager).getCriteriaBuilder();

    List<Object> expected = Collections.singletonList(5L);

    TypedQuery<?> mockTypedQuery = mock(TypedQuery.class);

    doReturn(expected).when(mockTypedQuery).getResultList();

    doReturn(mockTypedQuery).when(mockEntityManager).createQuery(mockQuery);

    List<Object> actual =
        daoSpy.findAllByAttribute(
            entityClass,
            attributeName,
            attributeValue,
            mockLastUpdatedParam,
            mockServiceDateParam,
            endAttribute);

    verify(mockQuery, times(1)).select(mockRoot);
    verify(mockQuery, times(1)).where(mockAndPredicate);
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
    public String getEntityMbiAttribute() {
      return "mbiAttribute";
    }

    @Override
    public String getEntityMbiHashAttribute() {
      return "mbiHashAttribute";
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
