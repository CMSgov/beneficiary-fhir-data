package gov.cms.bfd.pipeline.rda.grpc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
}
