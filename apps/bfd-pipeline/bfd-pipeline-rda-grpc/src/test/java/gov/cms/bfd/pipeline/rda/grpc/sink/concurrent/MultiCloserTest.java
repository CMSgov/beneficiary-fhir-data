package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MultiCloserTest {
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
