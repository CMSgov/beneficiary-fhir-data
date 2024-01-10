package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests the {@link MultiCloser}. */
public class MultiCloserTest {

  /** Tests that all closeables are executed with no exceptions. */
  @Test
  public void allExecutedWhenNoExceptions() throws Exception {
    final var values = new ArrayList<Integer>();
    var closer = new MultiCloser();
    closer.close(() -> values.add(1));
    closer.close(() -> values.add(2));
    closer.close(() -> values.add(3));
    closer.finish();
    assertEquals(List.of(1, 2, 3), values);
  }

  /**
   * Tests that a single exception is caught and thrown only after all closeables have been
   * executed.
   */
  @Test
  public void exceptionHeldUntilFinishIsCalled() throws Exception {
    final var error = new IOException("oops");
    final var values = new ArrayList<Integer>();
    var closer = new MultiCloser();
    closer.close(() -> values.add(1));
    closer.close(
        () -> {
          throw error;
        });
    closer.close(() -> values.add(3));
    try {
      closer.finish();
      fail("finish should have thrown");
    } catch (Exception thrown) {
      assertSame(error, thrown);
    }
    assertEquals(List.of(1, 3), values);
  }

  /**
   * Tests that when several exceptions are thrown, all closeables have first been executed before
   * they are thrown, with subsequent exceptions added as suppressed exceptions.
   */
  @Test
  public void multipleExceptionsCombined() throws Exception {
    final var error1 = new IOException("oops");
    final var error2 = new IOException("uh-oh");
    final var values = new ArrayList<Integer>();
    var closer = new MultiCloser();
    closer.close(() -> values.add(1));
    closer.close(
        () -> {
          throw error1;
        });
    closer.close(() -> values.add(2));
    closer.close(
        () -> {
          throw error2;
        });
    closer.close(() -> values.add(3));
    try {
      closer.finish();
      fail("finish should have thrown");
    } catch (Exception thrown) {
      assertSame(error1, thrown);
      var suppressed = thrown.getSuppressed();
      assertEquals(1, suppressed.length);
      assertSame(error2, suppressed[0]);
    }
    assertEquals(List.of(1, 2, 3), values);
  }
}
