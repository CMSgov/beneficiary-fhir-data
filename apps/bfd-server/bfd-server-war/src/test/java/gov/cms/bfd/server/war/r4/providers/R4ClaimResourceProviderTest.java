package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.IdType;
import org.junit.Test;

public class R4ClaimResourceProviderTest {

  @Test
  public void shouldReturnCorrectIBaseResourceType() {
    assertEquals(new R4ClaimResourceProvider().getResourceType(), Claim.class);
  }

  @Test
  public void shouldHaveReadMethodAnnotatedWithRead() throws NoSuchMethodException {
    R4ClaimResourceProvider provider = new R4ClaimResourceProvider();

    Method readMethod = provider.getClass().getMethod("read", IdType.class, RequestDetails.class);

    Read annotation = readMethod.getAnnotation(Read.class);

    assertNotNull(annotation);
  }

  @Test
  public void shouldHaveReadMethodWithAnnotatedId() throws NoSuchMethodException {
    R4ClaimResourceProvider provider = new R4ClaimResourceProvider();

    Method readMethod = provider.getClass().getMethod("read", IdType.class, RequestDetails.class);

    IdParam annotation = null;

    for (Parameter p : readMethod.getParameters()) {
      if (p.getType() == IdType.class) {
        annotation = p.getAnnotation(IdParam.class);
      }
    }

    assertNotNull(annotation);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdIsNull() {
    new R4ClaimResourceProvider().read(null, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNotNumeric() {
    IdType id = new IdType(null, "f-123", null, "null");
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdVersionIsNumeric() {
    IdType id = new IdType(null, "f-123", null, "123");
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNull() {
    IdType id = new IdType(null, null, null, null);
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsBlank() {
    IdType id = new IdType(null, null, " ", null);
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsNumeric() {
    IdType id = new IdType(null, null, "123", null);
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowIllegalArgumentExceptionIfIdPartIsDoesNotMatchPattern() {
    IdType id = new IdType(null, null, "abc123", null);
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test(expected = ResourceNotFoundException.class)
  public void shouldResourceNotFoundExceptionIfIdPartIsNotSupportedType() {
    IdType id = new IdType(null, null, "a-123", null);
    new R4ClaimResourceProvider().read(id, null);
  }

  @Test
  public void shouldReturnClaimObjectForFissClaim() {
    IdType id = new IdType(null, null, "f-123", null);

    MetricRegistry registry = new MetricRegistry();

    R4ClaimResourceProvider provider = new R4ClaimResourceProvider();
    provider.setMetricRegistry(registry);

    Claim expected = new Claim();
    Claim actual = provider.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }

  @Test
  public void shouldReturnClaimObjectForMcsClaim() {
    IdType id = new IdType(null, null, "m-123", null);

    MetricRegistry registry = new MetricRegistry();

    R4ClaimResourceProvider provider = new R4ClaimResourceProvider();
    provider.setMetricRegistry(registry);

    Claim expected = new Claim();
    Claim actual = provider.read(id, null);

    assertTrue(expected.equalsShallow(actual));
  }
}
