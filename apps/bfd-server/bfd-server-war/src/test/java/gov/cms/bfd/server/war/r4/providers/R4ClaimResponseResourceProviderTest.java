package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.server.war.utils.AssertUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Test;

public class R4ClaimResponseResourceProviderTest {

  @Test
  public void shouldReturnCorrectIBaseResourceType() {
    assertEquals(new R4ClaimResponseResourceProvider().getResourceType(), ClaimResponse.class);
  }

  @Test
  public void shouldHaveReadMethodAnnotatedWithRead() throws NoSuchMethodException {
    R4ClaimResponseResourceProvider provider = new R4ClaimResponseResourceProvider();

    Method readMethod = provider.getClass().getMethod("read", IdType.class, RequestDetails.class);

    Read annotation = readMethod.getAnnotation(Read.class);

    assertNotNull(annotation);
  }

  @Test
  public void shouldHaveReadMethodWithAnnotatedId() throws NoSuchMethodException {
    R4ClaimResponseResourceProvider provider = new R4ClaimResponseResourceProvider();

    Method readMethod = provider.getClass().getMethod("read", IdType.class, RequestDetails.class);

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
    // ConstantConditions - Still need to test
    //noinspection ConstantConditions
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(null, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNotNumeric() {
    IdType id = new IdType(null, "f-123", null, "null");

    Exception expected = new NumberFormatException("For input string: \"null\"");
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNumeric() {
    IdType id = new IdType(null, "f-123", null, "123");

    Exception expected = new IllegalArgumentException("Resource ID must not define a version.");
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNull() {
    IdType id = new IdType(null, null, null, null);

    Exception expected = new IllegalArgumentException("Resource ID can not be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsBlank() {
    IdType id = new IdType(null, null, " ", null);

    Exception expected = new IllegalArgumentException("Resource ID can not be null/blank");
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNumeric() {
    String idText = "123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new IllegalArgumentException("Unsupported ID pattern: " + idText);
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsDoesNotMatchPattern() {
    String idText = "abc123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new IllegalArgumentException("Unsupported ID pattern: " + idText);
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldResourceNotFoundExceptionIfIdPartIsNotSupportedType() {
    String idText = "a-123";
    IdType id = new IdType(null, null, idText, null);

    Exception expected = new ResourceNotFoundException(new IdType(idText));
    Exception actual =
        AssertUtils.catchExceptions(() -> new R4ClaimResponseResourceProvider().read(id, null));

    AssertUtils.assertThrowEquals(expected, actual);
  }

  @Test
  public void shouldReturnClaimObjectForFissClaim() {
    IdType id = new IdType(null, null, "f-123", null);

    MetricRegistry registry = new MetricRegistry();

    R4ClaimResponseResourceProvider provider = new R4ClaimResponseResourceProvider();
    provider.setMetricRegistry(registry);

    ClaimResponse expected = new ClaimResponse();
    ClaimResponse actual = provider.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }

  @Test
  public void shouldReturnClaimObjectForMcsClaim() {
    IdType id = new IdType(null, null, "m-123", null);

    MetricRegistry registry = new MetricRegistry();

    R4ClaimResponseResourceProvider provider = new R4ClaimResponseResourceProvider();
    provider.setMetricRegistry(registry);

    ClaimResponse expected = new ClaimResponse();
    ClaimResponse actual = provider.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }
}
