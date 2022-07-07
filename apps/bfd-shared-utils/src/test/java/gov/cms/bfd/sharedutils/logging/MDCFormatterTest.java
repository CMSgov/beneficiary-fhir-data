package gov.cms.bfd.sharedutils.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MDCFormatterTest {
  @Test
  public void convertsPeriodsToUnderscore() {
    assertEquals(
        "http_access_request_header_foo",
        MDCFormatter.formatMdcKey("http_access.request.header.foo"));
  }
}
