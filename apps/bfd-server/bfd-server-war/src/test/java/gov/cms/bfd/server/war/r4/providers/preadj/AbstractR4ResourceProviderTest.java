package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.ReflectionTestUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Ignore;
import org.junit.Test;

public class AbstractR4ResourceProviderTest {

  @Test
  public void shouldHaveInitMethodAnnotatedWithPostConstruct() throws NoSuchMethodException {
    Method method = AbstractR4ResourceProvider.class.getDeclaredMethod("init");

    assertTrue(method.isAnnotationPresent(PostConstruct.class));
  }

  @Test
  public void shouldCreateDaoInstance() throws NoSuchFieldException, IllegalAccessException {
    AbstractR4ResourceProvider<?> providerSpy = spy(AbstractR4ResourceProvider.class);

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry mockMetricRegistry = mock(MetricRegistry.class);

    ReflectionTestUtils.setField(providerSpy, "entityManager", mockEntityManager);
    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockMetricRegistry);

    doNothing().when(providerSpy).setResourceType();

    providerSpy.init();

    ClaimDao expected = new ClaimDao(mockEntityManager, mockMetricRegistry);
    Object actual = ReflectionTestUtils.getField(providerSpy, "claimDao");

    assertEquals(expected, actual);
  }

  @Test
  public void shouldInvokeSetResourceType() throws NoSuchFieldException, IllegalAccessException {
    AbstractR4ResourceProvider<?> providerSpy = spy(AbstractR4ResourceProvider.class);

    EntityManager mockEntityManager = mock(EntityManager.class);
    MetricRegistry mockMetricRegistry = mock(MetricRegistry.class);

    ReflectionTestUtils.setField(providerSpy, "entityManager", mockEntityManager);
    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockMetricRegistry);

    doNothing().when(providerSpy).setResourceType();

    providerSpy.init();

    verify(providerSpy, times(1)).setResourceType();
  }

  @Test
  public void shouldSetCorrectResourceType() throws NoSuchFieldException, IllegalAccessException {
    AbstractR4ResourceProvider<Claim> provider = new MockR4ResourceProvider();

    provider.setResourceType();

    Class<Claim> expected = Claim.class;
    Object actual = ReflectionTestUtils.getField(provider, "resourceType");

    assertEquals(expected, actual);
  }

  @Test
  public void shouldHaveReadMethodAnnotatedWithRead() throws NoSuchMethodException {
    Method readMethod =
        AbstractR4ResourceProvider.class.getMethod("read", IdType.class, RequestDetails.class);

    assertTrue(readMethod.isAnnotationPresent(Read.class));
  }

  @Test
  public void shouldHaveReadMethodWithAnnotatedId() throws NoSuchMethodException {
    Method readMethod =
        AbstractR4ResourceProvider.class.getMethod("read", IdType.class, RequestDetails.class);

    IdParam annotation = null;

    for (Parameter p : readMethod.getParameters()) {
      if (p.getType() == IdType.class) {
        annotation = p.getAnnotation(IdParam.class);
      }
    }

    assertNotNull(annotation);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdIsNull() {
    Exception expected = new IllegalArgumentException("Resource ID can not be null");
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(null, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNotNumeric() {
    IdType id = new IdType(null, "f-123", null, "null");

    Exception expected = new NumberFormatException("For input string: \"null\"");
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNumeric() {
    IdType id = new IdType(null, "f-123", null, "123");

    Exception expected = new IllegalArgumentException("Resource ID must not define a version.");
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNull() {
    IdType id = new IdType(null, null, null, null);

    Exception expected = new IllegalArgumentException("Resource ID can not be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsBlank() {
    IdType id = new IdType(null, null, " ", null);

    Exception expected = new IllegalArgumentException("Resource ID can not be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNumeric() {
    String idText = "123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new IllegalArgumentException("Unsupported ID pattern: " + idText);
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsDoesNotMatchPattern() {
    String idText = "abc123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new IllegalArgumentException("Unsupported ID pattern: " + idText);
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNotSupportedType() {
    String idText = "a-123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new IllegalArgumentException("Unsupported ID pattern: " + idText);
    Exception actual =
        AssertUtils.catchExceptions(() -> new MockR4ResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowResourceNotFoundExceptionIfUnsupportedType() {
    String idText = "f-123";
    IdType id = new IdType(null, null, idText, null);

    MockR4ResourceProvider providerSpy = spy(new MockR4ResourceProvider());

    doReturn(null).when(providerSpy).parseClaimType(anyString());

    doReturn(Optional.empty()).when(providerSpy).parseClaimType("f");

    Exception expected = new ResourceNotFoundException(id);
    Exception actual = AssertUtils.catchExceptions(() -> providerSpy.read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldReturnClaimObjectForFissClaim()
      throws NoSuchFieldException, IllegalAccessException {
    IdType id = new IdType(null, null, "f-123", null);

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry));
    PreAdjFissClaim mockClaim = mock(PreAdjFissClaim.class);

    ResourceTypeV2<?> mockClaimType = mock(ResourceTypeV2.class);

    doReturn(null).when(daoSpy).getEntityById(any(ResourceTypeV2.class), anyString());

    doReturn(mockClaim).when(daoSpy).getEntityById(mockClaimType, "123");

    MockR4ResourceProvider providerSpy = spy(new MockR4ResourceProvider());

    doReturn(null).when(providerSpy).parseClaimType(anyString());

    ResourceTransformer<?> mockTransformer = mock(ResourceTransformer.class);

    Claim expected = new Claim();
    expected.setId("Expected claim");

    doReturn(expected).when(mockTransformer).transform(registry, mockClaim);

    doReturn(mockTransformer).when(mockClaimType).getTransformer();

    doReturn(Optional.of(mockClaimType)).when(providerSpy).parseClaimType("f");

    providerSpy.setMetricRegistry(registry);

    ReflectionTestUtils.setField(providerSpy, "claimDao", daoSpy);

    Claim actual = providerSpy.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }

  @Ignore("Ignoring until we have MCS claims")
  @Test
  public void shouldReturnClaimObjectForMcsClaim()
      throws NoSuchFieldException, IllegalAccessException {
    IdType id = new IdType(null, null, "m-123", null);

    MetricRegistry registry = new MetricRegistry();
    EntityManager mockEntityManager = mock(EntityManager.class);

    ClaimDao daoSpy = spy(new ClaimDao(mockEntityManager, registry));
    PreAdjFissClaim mockClaim = mock(PreAdjFissClaim.class);

    ResourceTypeV2<?> mockClaimType = mock(ResourceTypeV2.class);

    doReturn(null).when(daoSpy).getEntityById(any(ClaimTypeV2.class), anyString());

    doReturn(mockClaim).when(daoSpy).getEntityById(mockClaimType, "123");

    MockR4ResourceProvider providerSpy = spy(new MockR4ResourceProvider());

    doReturn(null).when(providerSpy).parseClaimType(anyString());

    ResourceTransformer<?> mockTransformer = mock(ResourceTransformer.class);

    Claim expected = new Claim();
    expected.setId("Expected claim");

    doReturn(expected).when(mockTransformer).transform(registry, mockClaim);

    doReturn(mockTransformer).when(mockClaimType).getTransformer();

    doReturn(Optional.of(mockClaimType)).when(providerSpy).parseClaimType("m");

    providerSpy.setMetricRegistry(registry);

    ReflectionTestUtils.setField(providerSpy, "preAdjClaimDao", daoSpy);

    Claim actual = providerSpy.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }

  private static class MockR4ResourceProvider extends AbstractR4ResourceProvider<Claim> {

    @Override
    Optional<ResourceTypeV2<Claim>> parseClaimType(String typeText) {
      return Optional.empty();
    }
  }
}
