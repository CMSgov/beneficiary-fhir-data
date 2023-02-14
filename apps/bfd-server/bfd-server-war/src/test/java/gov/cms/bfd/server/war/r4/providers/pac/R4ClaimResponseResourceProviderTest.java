package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests the {@link R4ClaimResponseResourceProvider}. */
public class R4ClaimResponseResourceProviderTest {

  /** Tests that the provider extends the abstract provider. */
  @Test
  public void shouldExtendAbstractR4ResourceProvider() {
    assertTrue(
        AbstractR4ResourceProvider.class.isAssignableFrom(R4ClaimResponseResourceProvider.class));
  }
}
