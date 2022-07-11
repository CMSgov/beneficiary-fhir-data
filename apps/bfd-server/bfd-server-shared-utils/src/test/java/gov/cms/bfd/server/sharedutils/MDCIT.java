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
    MDC.setMDCAdapter(mdcMock);
    Mockito.reset(mdcMock);
  }

  /** Test whether {@link MDC#formatMDCKey(String)} replaces "." with "_". */
  @Test
  public void convertsPeriodsToUnderscore() {
    assertEquals("key_that_needs_formatting", MDC.formatMDCKey("key.that.needs.formatting"));
  }

  /**
   * Test whether {@link MDC#put(String, String)} calls the underlying {@link
   * org.slf4j.MDC#put(String, String)} method.
   */
  @Test
  public void putsToMDC() {
    MDC.put("key_that_does_not_need_formatting", "This key should be inserted as-is.");
    Mockito.verify(mdcMock)
        .put("key_that_does_not_need_formatting", "This key should be inserted as-is.");
    Mockito.verifyNoMoreInteractions(mdcMock);
  }

  /**
   * Test whether {@link MDC#put(String, String)} calls the underlying {@link
   * org.slf4j.MDC#put(String, String)} method, after first replacing "." characters with "_".
   */
  @Test
  public void putsToMDCAndFormats() {
    MDC.put("dot.separated.key", "This key needs to be reformatted to change . into _");
    Mockito.verify(mdcMock)
        .put("dot_separated_key", "This key needs to be reformatted to change . into _");
    Mockito.verifyNoMoreInteractions(mdcMock);
  }

  /**
   * Test whether {@link MDC#clear()} calls the underlying {@link org.slf4j.MDC#clear()} function.
   */
  @Test
  public void clearsMDC() {
    MDC.clear();
    Mockito.verify(mdcMock).clear();
    Mockito.verifyNoMoreInteractions(mdcMock);
  }
}
