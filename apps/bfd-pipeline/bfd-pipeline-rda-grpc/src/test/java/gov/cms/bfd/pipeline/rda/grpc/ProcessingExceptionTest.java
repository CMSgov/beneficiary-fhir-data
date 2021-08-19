package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.Assert.*;

import java.io.IOException;
import org.junit.Test;

public class ProcessingExceptionTest {
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

  @Test
  public void testOriginalCause() {
    IOException cause = new IOException();
    ProcessingException root = new ProcessingException(cause, 3);
    ProcessingException parent = new ProcessingException(root, 5);
    assertSame(cause, root.getOriginalCause());
    assertSame(cause, parent.getOriginalCause());
  }
}
