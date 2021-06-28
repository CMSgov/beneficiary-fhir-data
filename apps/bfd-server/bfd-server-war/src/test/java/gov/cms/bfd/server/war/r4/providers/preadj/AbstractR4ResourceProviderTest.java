package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anySet;
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
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
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

  @Test
  public void shouldThrowIllegalArgumentExceptionIfMbiNull() {
    ReferenceParam mockParam = null;
    String type = "f";
    String hashed = null;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);
    RequestDetails mockRequestDetails = mock(RequestDetails.class);

    Exception expected = new IllegalArgumentException("mbi can't be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(
            () ->
                new MockR4ResourceProvider()
                    .findByPatient(
                        mockParam,
                        type,
                        hashed,
                        mockLastUpdated,
                        mockServiceDate,
                        mockRequestDetails));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfMbiEmpty() {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    String type = "f";
    String hashed = null;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);
    RequestDetails mockRequestDetails = mock(RequestDetails.class);

    doReturn(" ").when(mockParam).getIdPart();

    Exception expected = new IllegalArgumentException("mbi can't be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(
            () ->
                new MockR4ResourceProvider()
                    .findByPatient(
                        mockParam,
                        type,
                        hashed,
                        mockLastUpdated,
                        mockServiceDate,
                        mockRequestDetails));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldCreateBundleOfSpecificType()
      throws NoSuchFieldException, IllegalAccessException {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    String type = "f";
    String hashed = "false";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);
    RequestDetails mockRequestDetails = mock(RequestDetails.class);

    String mbi = "mbimbimbi";

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(providerSpy, "claimDao", mockDao);

    doReturn(mbi).when(mockParam).getIdPart();

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> mockResourceType = mock(ResourceTypeV2.class);

    Class<Object> claimType = Object.class;

    doReturn(null).when(providerSpy).parseClaimType(anyString());

    doReturn(claimType).when(mockResourceType).getEntityClass();

    doReturn(Optional.of(mockResourceType)).when(providerSpy).parseClaimType(type);

    Bundle expected = mock(Bundle.class);

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(anySet(), anyString(), anyBoolean(), any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(Collections.singleton(mockResourceType), mbi, false, mockLastUpdated);

    Bundle actual =
        providerSpy.findByPatient(
            mockParam, type, hashed, mockLastUpdated, mockServiceDate, mockRequestDetails);

    assertSame(expected, actual);
  }

  @Test
  public void shouldCreateBundleOfUnspecificType()
      throws NoSuchFieldException, IllegalAccessException {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    String type = null;
    String hashed = "false";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);
    RequestDetails mockRequestDetails = mock(RequestDetails.class);

    String mbi = "mbimbimbi";

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(providerSpy, "claimDao", mockDao);

    doReturn(mbi).when(mockParam).getIdPart();

    doReturn(null).when(providerSpy).parseClaimType(anyString());

    doReturn(Optional.empty()).when(providerSpy).parseClaimType(type);

    Bundle expected = mock(Bundle.class);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    Set<ResourceTypeV2<Claim>> mockTypesList = mock(Set.class);

    doReturn(mockTypesList).when(providerSpy).getResourceTypes();

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(anySet(), anyString(), anyBoolean(), any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(mockTypesList, mbi, false, mockLastUpdated);

    Bundle actual =
        providerSpy.findByPatient(
            mockParam, type, hashed, mockLastUpdated, mockServiceDate, mockRequestDetails);

    assertSame(expected, actual);
  }

  @Test
  public void shouldReturnBundleForMbi() throws NoSuchFieldException, IllegalAccessException {
    String mbi = "mbimbimbi";
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> mockResourceType = mock(ResourceTypeV2.class);
    Set<ResourceTypeV2<Claim>> types = Collections.singleton(mockResourceType);
    boolean isHashed = false;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(providerSpy, "claimDao", mockDao);

    Class<Object> claimType = Object.class;

    Object object1 = "thing1";
    Object object2 = "thing2";
    List<Object> entities = Arrays.asList(object1, object2);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(null)
        .when(mockDao)
        .findAllByMbiHash(any(Class.class), anyString(), any(DateRangeParam.class));

    doReturn(claimType).when(mockResourceType).getEntityClass();

    doReturn(entities).when(mockDao).findAllByMbi(claimType, mbi, mockLastUpdated);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    ResourceTransformer<IBaseResource> mockResourceTransformer = mock(ResourceTransformer.class);

    doReturn(mockResourceTransformer).when(mockResourceType).getTransformer();

    Resource resource1 = new Claim().setId("Thing1");
    Resource resource2 = new Claim().setId("Thing2");

    doReturn(resource1).when(mockResourceTransformer).transform(mockRegistry, object1);

    doReturn(resource2).when(mockResourceTransformer).transform(mockRegistry, object2);

    Bundle expected = new Bundle();
    expected.addEntry().setResource(resource1);
    expected.addEntry().setResource(resource2);

    Bundle actual = providerSpy.createBundleFor(types, mbi, isHashed, mockLastUpdated);

    assertTrue(actual.equalsDeep(expected));
  }

  @Test
  public void shouldReturnBundleForMbiHash() throws NoSuchFieldException, IllegalAccessException {
    String mbi = "mbimbimbi";
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> mockResourceType = mock(ResourceTypeV2.class);
    Set<ResourceTypeV2<Claim>> types = Collections.singleton(mockResourceType);
    boolean isHashed = true;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    ReflectionTestUtils.setField(providerSpy, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(providerSpy, "claimDao", mockDao);

    Class<Object> claimType = Object.class;

    Object object1 = "thing1";
    Object object2 = "thing2";
    List<Object> entities = Arrays.asList(object1, object2);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    doReturn(null)
        .when(mockDao)
        .findAllByMbi(any(Class.class), anyString(), any(DateRangeParam.class));

    doReturn(claimType).when(mockResourceType).getEntityClass();

    doReturn(entities).when(mockDao).findAllByMbiHash(claimType, mbi, mockLastUpdated);

    // unchecked - Creating mocks, this is ok.
    //noinspection unchecked
    ResourceTransformer<IBaseResource> mockResourceTransformer = mock(ResourceTransformer.class);

    doReturn(mockResourceTransformer).when(mockResourceType).getTransformer();

    Resource resource1 = new Claim().setId("Thing1");
    Resource resource2 = new Claim().setId("Thing2");

    doReturn(resource1).when(mockResourceTransformer).transform(mockRegistry, object1);

    doReturn(resource2).when(mockResourceTransformer).transform(mockRegistry, object2);

    Bundle expected = new Bundle();
    expected.addEntry().setResource(resource1);
    expected.addEntry().setResource(resource2);

    Bundle actual = providerSpy.createBundleFor(types, mbi, isHashed, mockLastUpdated);

    assertTrue(actual.equalsDeep(expected));
  }

  private static class MockR4ResourceProvider extends AbstractR4ResourceProvider<Claim> {

    @Override
    Optional<ResourceTypeV2<Claim>> parseClaimType(String typeText) {
      return Optional.empty();
    }

    @Override
    Set<ResourceTypeV2<Claim>> getResourceTypes() {
      return Collections.emptySet();
    }
  }
}
