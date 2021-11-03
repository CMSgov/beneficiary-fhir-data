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
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ClaimDao;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTransformer;
import gov.cms.bfd.server.war.r4.providers.preadj.common.ResourceTypeV2;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.ReflectionTestUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
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
  public void shouldReturnSetOfClaimTypes() {
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> mockTypeA = mock(ResourceTypeV2.class);
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> mockTypeB = mock(ResourceTypeV2.class);

    TokenAndListParam types =
        new TokenAndListParam()
            .addAnd(new TokenParam(null, "typeA"), new TokenParam(null, "typeB"));

    Map<String, ResourceTypeV2<Claim>> typeMap =
        ImmutableMap.of(
            "typea", mockTypeA,
            "typeb", mockTypeB);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    doReturn(typeMap).when(providerSpy).getResourceTypeMap();

    // unchecked - This should be fine.
    //noinspection unchecked
    Set<ResourceTypeV2<Claim>> expected = Sets.newHashSet(mockTypeA, mockTypeB);
    Set<ResourceTypeV2<Claim>> actual = providerSpy.parseClaimTypes(types);

    assertEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfMbiNull() {
    ReferenceParam mockParam = null;
    TokenAndListParam types =
        new TokenAndListParam().addAnd(new TokenOrListParam().add(null, "fiss"));
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
                        types,
                        hashed,
                        "false",
                        mockLastUpdated,
                        mockServiceDate,
                        mockRequestDetails));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfMbiEmpty() {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    TokenAndListParam types =
        new TokenAndListParam().addAnd(new TokenOrListParam().add(null, "fiss"));
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
                        types,
                        hashed,
                        "false",
                        mockLastUpdated,
                        mockServiceDate,
                        mockRequestDetails));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldCreateBundleOfSpecificType()
      throws NoSuchFieldException, IllegalAccessException {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    TokenAndListParam types =
        new TokenAndListParam().addAnd(new TokenOrListParam().add(null, "fiss"));
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

    doReturn(Collections.singleton(mockResourceType)).when(providerSpy).parseClaimTypes(types);

    Bundle expected = mock(Bundle.class);

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(
            anySet(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(DateRangeParam.class),
            any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(
            Collections.singleton(mockResourceType),
            mbi,
            false,
            false,
            mockLastUpdated,
            mockServiceDate);

    Bundle actual =
        providerSpy.findByPatient(
            mockParam,
            types,
            hashed,
            "false",
            mockLastUpdated,
            mockServiceDate,
            mockRequestDetails);

    assertSame(expected, actual);
  }

  @Test
  public void shouldCreateBundleOfUnspecificType()
      throws NoSuchFieldException, IllegalAccessException {
    ReferenceParam mockParam = mock(ReferenceParam.class);
    TokenAndListParam types = null;
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

    Bundle expected = mock(Bundle.class);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    Set<ResourceTypeV2<Claim>> mockTypesList = mock(Set.class);

    doReturn(mockTypesList).when(providerSpy).getResourceTypes();

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(
            anySet(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(DateRangeParam.class),
            any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(mockTypesList, mbi, false, false, mockLastUpdated, mockServiceDate);

    Bundle actual =
        providerSpy.findByPatient(
            mockParam,
            types,
            hashed,
            "false",
            mockLastUpdated,
            mockServiceDate,
            mockRequestDetails);

    assertSame(expected, actual);
  }

  @Test
  public void shouldThrowErrorForNoMbiParameter() {
    ReferenceParam mockRefParam = null;
    TokenAndListParam mockTokenListParam = mock(TokenAndListParam.class);
    String hashed = "false";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    doReturn(null).when(providerSpy).parseClaimTypes(any(TokenAndListParam.class));

    doReturn(null).when(providerSpy).getResourceTypes();

    Exception expected = new IllegalArgumentException("mbi can't be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(
            () ->
                providerSpy.findByPatient(
                    mockRefParam,
                    mockTokenListParam,
                    hashed,
                    "false",
                    mockLastUpdated,
                    mockServiceDate,
                    null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowErrorForBlankMbiParameter() {
    ReferenceParam mockRefParam = mock(ReferenceParam.class);
    TokenAndListParam mockTokenListParam = mock(TokenAndListParam.class);
    String hashed = "false";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    doReturn(null).when(mockRefParam).getIdPart();

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    doReturn(null).when(providerSpy).parseClaimTypes(any(TokenAndListParam.class));

    doReturn(null).when(providerSpy).getResourceTypes();

    Exception expected = new IllegalArgumentException("mbi can't be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(
            () ->
                providerSpy.findByPatient(
                    mockRefParam,
                    mockTokenListParam,
                    hashed,
                    "false",
                    mockLastUpdated,
                    mockServiceDate,
                    null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldReturnBundleForMbi() {
    String mbi = "mbimbimbi";

    ReferenceParam mockRefParam = mock(ReferenceParam.class);
    TokenAndListParam mockTokenListParam = mock(TokenAndListParam.class);
    String hashed = "false";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    doReturn(mbi).when(mockRefParam).getIdPart();

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    Set<ResourceTypeV2<Claim>> mockTypeSet = mock(Set.class);

    doReturn(null).when(providerSpy).parseClaimTypes(any(TokenAndListParam.class));

    doReturn(mockTypeSet).when(providerSpy).parseClaimTypes(mockTokenListParam);

    doReturn(null).when(providerSpy).getResourceTypes();

    Bundle expected = mock(Bundle.class);

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(
            anySet(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(DateRangeParam.class),
            any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(mockTypeSet, mbi, false, false, mockLastUpdated, mockServiceDate);

    Bundle actual =
        providerSpy.findByPatient(
            mockRefParam,
            mockTokenListParam,
            hashed,
            "false",
            mockLastUpdated,
            mockServiceDate,
            null);

    assertSame(expected, actual);
  }

  @Test
  public void shouldReturnBundleForHashedMbi() {
    String mbi = "mbimbimbi";

    ReferenceParam mockRefParam = mock(ReferenceParam.class);
    String hashed = "true";
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    doReturn(mbi).when(mockRefParam).getIdPart();

    AbstractR4ResourceProvider<Claim> providerSpy = spy(new MockR4ResourceProvider());

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    Set<ResourceTypeV2<Claim>> mockTypeSet = mock(Set.class);

    doReturn(null).when(providerSpy).parseClaimTypes(any(TokenAndListParam.class));

    doReturn(mockTypeSet).when(providerSpy).getResourceTypes();

    Bundle expected = mock(Bundle.class);

    doReturn(null)
        .when(providerSpy)
        .createBundleFor(
            anySet(),
            anyString(),
            anyBoolean(),
            anyBoolean(),
            any(DateRangeParam.class),
            any(DateRangeParam.class));

    doReturn(expected)
        .when(providerSpy)
        .createBundleFor(mockTypeSet, mbi, true, false, mockLastUpdated, mockServiceDate);

    Bundle actual =
        providerSpy.findByPatient(
            mockRefParam, null, hashed, "false", mockLastUpdated, mockServiceDate, null);

    assertSame(expected, actual);
  }

  @Test
  public void shouldCreateBundleForNonHashedMbi()
      throws NoSuchFieldException, IllegalAccessException {
    String mbi = "mbimbimbi";
    String mbiAttribute = "mbi";
    String mbiHashAttribute = "mbiHash";
    String endAttribute1 = "endAttribute1";
    String endAttribute2 = "endAttribute2";

    boolean isHashed = false;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    MockR4ResourceProvider provider = new MockR4ResourceProvider();

    ReflectionTestUtils.setField(provider, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(provider, "claimDao", mockDao);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> resourceType1 = mock(ResourceTypeV2.class);
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> resourceType2 = mock(ResourceTypeV2.class);

    // Need to use an ordered set to have deterministic testing
    Set<ResourceTypeV2<Claim>> resourceTypes = new LinkedHashSet<>(2);
    resourceTypes.add(resourceType1);
    resourceTypes.add(resourceType2);

    Class<?> mockEntityType1 = Integer.class;
    Class<?> mockEntityType2 = String.class;

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTransformer<Claim> mockTransformer1 = mock(ResourceTransformer.class);
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTransformer<Claim> mockTransformer2 = mock(ResourceTransformer.class);

    Object object1 = "object1";
    Object object2 = "object2";

    List<?> entityList1 = Collections.singletonList(object1);
    List<?> entityList2 = Collections.singletonList(object2);

    Resource resource1 = new Claim().setId("Thing1");
    Resource resource2 = new Claim().setId("Thing2");

    doReturn(resource1).when(mockTransformer1).transform(mockRegistry, object1);

    doReturn(resource2).when(mockTransformer2).transform(mockRegistry, object2);

    doReturn(mbiAttribute).when(resourceType1).getEntityMbiAttribute();

    doReturn(mbiHashAttribute).when(resourceType1).getEntityMbiHashAttribute();

    doReturn(mockEntityType1).when(resourceType1).getEntityClass();

    doReturn(endAttribute1).when(resourceType1).getEntityEndDateAttribute();

    doReturn(mockTransformer1).when(resourceType1).getTransformer();

    doReturn(mbiAttribute).when(resourceType2).getEntityMbiAttribute();

    doReturn(mbiHashAttribute).when(resourceType2).getEntityMbiHashAttribute();

    doReturn(mockEntityType2).when(resourceType2).getEntityClass();

    doReturn(endAttribute2).when(resourceType2).getEntityEndDateAttribute();

    doReturn(mockTransformer2).when(resourceType2).getTransformer();

    doReturn(entityList1)
        .when(mockDao)
        .findAllByAttribute(
            mockEntityType1, mbiAttribute, mbi, mockLastUpdated, mockServiceDate, endAttribute1);

    doReturn(entityList2)
        .when(mockDao)
        .findAllByAttribute(
            mockEntityType2, mbiAttribute, mbi, mockLastUpdated, mockServiceDate, endAttribute2);

    Bundle expected = new Bundle();
    expected.addEntry().setResource(resource1);
    expected.addEntry().setResource(resource2);

    Bundle actual =
        provider.createBundleFor(
            resourceTypes, mbi, isHashed, false, mockLastUpdated, mockServiceDate);

    assertTrue(expected.equalsDeep(actual));
  }

  @Test
  public void shouldCreateBundleForHashedMbi() throws NoSuchFieldException, IllegalAccessException {
    String mbi = "mbimbimbi";
    String mbiAttribute = "mbi";
    String mbiHashAttribute = "mbiHash";
    String endAttribute1 = "endAttribute1";
    String endAttribute2 = "endAttribute2";

    boolean isHashed = true;
    DateRangeParam mockLastUpdated = mock(DateRangeParam.class);
    DateRangeParam mockServiceDate = mock(DateRangeParam.class);

    MetricRegistry mockRegistry = mock(MetricRegistry.class);

    ClaimDao mockDao = mock(ClaimDao.class);

    MockR4ResourceProvider provider = new MockR4ResourceProvider();

    ReflectionTestUtils.setField(provider, "metricRegistry", mockRegistry);
    ReflectionTestUtils.setField(provider, "claimDao", mockDao);

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> resourceType1 = mock(ResourceTypeV2.class);
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTypeV2<Claim> resourceType2 = mock(ResourceTypeV2.class);

    // Need to use an ordered set to have deterministic testing
    Set<ResourceTypeV2<Claim>> resourceTypes = new LinkedHashSet<>(2);
    resourceTypes.add(resourceType1);
    resourceTypes.add(resourceType2);

    Class<?> mockEntityType1 = Integer.class;
    Class<?> mockEntityType2 = String.class;

    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTransformer<Claim> mockTransformer1 = mock(ResourceTransformer.class);
    // unchecked - This is fine for creating a mock.
    //noinspection unchecked
    ResourceTransformer<Claim> mockTransformer2 = mock(ResourceTransformer.class);

    Object object1 = "object1";
    Object object2 = "object2";

    List<?> entityList1 = Collections.singletonList(object1);
    List<?> entityList2 = Collections.singletonList(object2);

    Resource resource1 = new Claim().setId("Thing1");
    Resource resource2 = new Claim().setId("Thing2");

    doReturn(resource1).when(mockTransformer1).transform(mockRegistry, object1);

    doReturn(resource2).when(mockTransformer2).transform(mockRegistry, object2);

    doReturn(mbiAttribute).when(resourceType1).getEntityMbiAttribute();

    doReturn(mbiHashAttribute).when(resourceType1).getEntityMbiHashAttribute();

    doReturn(mockEntityType1).when(resourceType1).getEntityClass();

    doReturn(endAttribute1).when(resourceType1).getEntityEndDateAttribute();

    doReturn(mockTransformer1).when(resourceType1).getTransformer();

    doReturn(mbiAttribute).when(resourceType2).getEntityMbiAttribute();

    doReturn(mbiHashAttribute).when(resourceType2).getEntityMbiHashAttribute();

    doReturn(mockEntityType2).when(resourceType2).getEntityClass();

    doReturn(endAttribute2).when(resourceType2).getEntityEndDateAttribute();

    doReturn(mockTransformer2).when(resourceType2).getTransformer();

    doReturn(entityList1)
        .when(mockDao)
        .findAllByAttribute(
            mockEntityType1,
            mbiHashAttribute,
            mbi,
            mockLastUpdated,
            mockServiceDate,
            endAttribute1);

    doReturn(entityList2)
        .when(mockDao)
        .findAllByAttribute(
            mockEntityType2,
            mbiHashAttribute,
            mbi,
            mockLastUpdated,
            mockServiceDate,
            endAttribute2);

    Bundle expected = new Bundle();
    expected.addEntry().setResource(resource1);
    expected.addEntry().setResource(resource2);

    Bundle actual =
        provider.createBundleFor(
            resourceTypes, mbi, isHashed, false, mockLastUpdated, mockServiceDate);

    assertTrue(expected.equalsDeep(actual));
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

    @Override
    Map<String, ResourceTypeV2<Claim>> getResourceTypeMap() {
      return null;
    }

    @Override
    boolean hasNoSamhsaData(MetricRegistry metricRegistry, Object entity) {
      return true;
    }
  }
}
