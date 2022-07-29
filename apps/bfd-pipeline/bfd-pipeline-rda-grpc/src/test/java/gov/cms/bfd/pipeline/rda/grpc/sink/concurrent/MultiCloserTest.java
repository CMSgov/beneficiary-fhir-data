package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import gov.cms.bfd.pipeline.rda.grpc.MultiCloser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MultiCloserTest {

  /** Tests that all closeables are executed with no exceptions. */
  @Test
  public void allExecutedWhenNoExceptions() throws Exception {
    final var values = new ArrayList<Integer>();
    try (var closer = new MultiCloser()) {
      closer.add(() -> values.add(1));
      closer.add(() -> values.add(2));
      closer.add(() -> values.add(3));
    }
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
    boolean didThrow = false;
    try (var closer = new MultiCloser()) {
      closer.add(() -> values.add(1));
      closer.add(
          () -> {
            throw error;
          });
      closer.add(() -> values.add(3));
    } catch (Exception thrown) {
      didThrow = true;
      assertSame(error, thrown);
    }
    assertTrue(didThrow, "closer should have thrown, but didn't");
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
    boolean didThrow = false;
    try (var closer = new MultiCloser()) {
      closer.add(() -> values.add(1));
      closer.add(
          () -> {
            throw error1;
          });
      closer.add(() -> values.add(2));
      closer.add(
          () -> {
            throw error2;
          });
      closer.add(() -> values.add(3));
    } catch (Exception thrown) {
      didThrow = true;
      assertSame(error1, thrown);
      var suppressed = thrown.getSuppressed();
      assertEquals(1, suppressed.length);
      assertSame(error2, suppressed[0]);
    }
    assertTrue(didThrow, "closer should have thrown, but didn't");
    assertEquals(List.of(1, 2, 3), values);
  }
}
