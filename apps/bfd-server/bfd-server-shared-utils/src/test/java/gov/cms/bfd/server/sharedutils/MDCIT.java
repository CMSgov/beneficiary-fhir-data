package gov.cms.bfd.server.sharedutils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.spi.MDCAdapter;

/** Unit tests for {@link MDC}. */
@ExtendWith(MockitoExtension.class)
public final class MDCIT {

  /** A mocked MDC adapter for use in the tests. */
  @Mock MDCAdapter mdcMock;

  /** Set up tests with a mocked MDC adapter and make sure the MDC context is clear. */
  @BeforeEach
  public void beforeEach() {
    mdcMock.clear();
    MDC.setMdcAdapter(mdcMock);
    Mockito.reset(mdcMock);
  }

  /** Test whether {@link MDC#formatMdcKey(String)} replaces "." with "_". */
  @Test
  public void convertsPeriodsToUnderscore() {
    assertEquals(
        "http_access_request_header_foo", MDC.formatMdcKey("http_access.request.header.foo"));
  }

  /**
   * Test whether {@link MDC#put(String, String)} calls the underlying {@link
   * org.slf4j.MDC#put(String, String)} method.
   */
  @Test
  public void putsToMdc() {
    MDC.put("http_access_response_key", "bar");
    Mockito.verify(mdcMock).put("http_access_response_key", "bar");
    Mockito.verifyNoMoreInteractions(mdcMock);
  }

  /**
   * Test whether {@link MDC#put(String, String)} calls the underlying {@link
   * org.slf4j.MDC#put(String, String)} method, after first replacing "." characters with "_".
   */
  @Test
  public void putsToMdcAndFormats() {
    String mdcValue = "This should be retrievable by the reformatted key";
    MDC.put("http_access.response.dot_separated.key", mdcValue);
    Mockito.verify(mdcMock).put("http_access_response_dot_separated_key", mdcValue);
    Mockito.verifyNoMoreInteractions(mdcMock);
  }

  /**
   * Test whether {@link MDC#clear()} calls the underlying {@link org.slf4j.MDC#clear()} function.
   */
  @Test
  public void clearsMDC() {
    MDC.put("test_string", "This should disappear when we clear the MDC.");
    Mockito.verify(mdcMock).put("test_string", "This should disappear when we clear the MDC.");
    MDC.clear();
    Mockito.verify(mdcMock).clear();
    Mockito.verifyNoMoreInteractions(mdcMock);
  }
}
