package gov.cms.bfd.server.war.r4.providers.partadj;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class R4ClaimResourceProviderTest {

  @Test
  public void shouldExtendAbstractR4ResourceProvider() {
    assertTrue(AbstractR4ResourceProvider.class.isAssignableFrom(R4ClaimResourceProvider.class));
  }
}
