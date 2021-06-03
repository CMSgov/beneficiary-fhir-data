package gov.cms.bfd.server.war.r4.providers.preadj.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.codahale.metrics.MetricRegistry;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.junit.Test;

public class ClaimDaoTest {

  @Test
  public void shouldInvokeGetByIdForClaimType() {
    String claimId = "123";

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    Object expected = 5L;

    IClaimTypeV2 mockClaimType = new MockClaimType();

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
  public void shouldInvokeGetByIdForClaimResponseType() {
    String claimId = "123";

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    Object expected = 5L;

    IClaimResponseTypeV2 mockClaimType = new MockClaimResponseType();

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
  public void shouldGetEntity() {
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

  private static class MockClaimType implements IClaimTypeV2 {

    @Override
    public Class<?> getEntityClass() {
      return Long.class;
    }

    @Override
    public String getEntityIdAttribute() {
      return "somePropertyName";
    }

    @Override
    public ClaimTypeTransformerV2 getTransformer() {
      return null;
    }
  }

  private static class MockClaimResponseType implements IClaimResponseTypeV2 {

    @Override
    public Class<?> getEntityClass() {
      return Long.class;
    }

    @Override
    public String getEntityIdAttribute() {
      return "somePropertyName";
    }

    @Override
    public ClaimResponseTypeTransformerV2 getTransformer() {
      return null;
    }
  }
}
