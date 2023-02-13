package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Tests the {@link ProcessingException}. */
public class ProcessingExceptionTest {
  /**
   * Verifies that {@link ProcessingException#isInterrupted} returns the expected value for various
   * inputs.
   */
  @Test
  public void testInterruptedDetection() {
    Throwable error = null;
    assertFalse(ProcessingException.isInterrupted(error));

    error = new InterruptedException();
    assertTrue(ProcessingException.isInterrupted(error));

    error = new IOException();
    assertFalse(ProcessingException.isInterrupted(error));

    error = new IOException(error);
    assertFalse(ProcessingException.isInterrupted(error));

    error = new IOException(new InterruptedException());
    assertTrue(ProcessingException.isInterrupted(error));

    error = new IOException(error);
    assertTrue(ProcessingException.isInterrupted(error));
  }

  /**
   * Verifies the original cause is correctly set whether the root exception or another processing
   * exception is used.
   */
  @Test
  public void testOriginalCause() {
    IOException cause = new IOException();
    ProcessingException root = new ProcessingException(cause, 3);
    ProcessingException parent = new ProcessingException(root, 5);
    assertSame(cause, root.getOriginalCause());
    assertSame(cause, parent.getOriginalCause());
  }
}
