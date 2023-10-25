package gov.cms.bfd.sharedutils.exceptions;

import static gov.cms.bfd.sharedutils.exceptions.CheckedExceptionWrapper.unchecked;
import static gov.cms.bfd.sharedutils.exceptions.CheckedExceptionWrapper.unwrapped;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.sharedutils.interfaces.ThrowingRunnable;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.Test;

public class CheckedExceptionWrapperTest {
  @Test
  void uncheckedShouldWrapExceptions() {
    final var unchecked = new RuntimeException();
    final var checked = new IOException();
    final var alreadyWrapped = new CheckedExceptionWrapper(checked);

    assertSame(unchecked, unchecked(unchecked));
    assertIsWrapperFor(checked, unchecked(checked));
    assertSame(alreadyWrapped, unchecked(alreadyWrapped));
  }

  @Test
  void unwrappedShouldUnwrapExceptions() {
    final var unchecked = new RuntimeException();
    final var checked = new IOException();
    final var alreadyWrapped = new CheckedExceptionWrapper(checked);

    assertSame(unchecked, unwrapped(unchecked));
    assertSame(checked, unwrapped(checked));
    assertSame(checked, unwrapped(alreadyWrapped));
    assertSame(checked, unwrapped(new CheckedExceptionWrapper(alreadyWrapped)));
  }

  @Test
  void unwrapShouldHandleNesting() {
    final var original = new IOException();
    assertUnwrapsTo(original, new CheckedExceptionWrapper(original));
    assertUnwrapsTo(original, new CheckedExceptionWrapper(new CheckedExceptionWrapper(original)));
  }

  @Test
  void callShouldWrapCheckedExceptions() {
    // value from successful is returned directly
    String result = "hello, world!";
    assertSame(result, CheckedExceptionWrapper.call(() -> result));

    // unchecked exceptions are rethrown without being wrapped
    final var unchecked = new RuntimeException();
    assertCall(
        () -> {
          throw unchecked;
        },
        t -> assertSame(unchecked, t));

    // checked exceptions are wrapped
    final var checked = new IOException();
    assertCall(
        () -> {
          throw checked;
        },
        t -> assertIsWrapperFor(checked, t));
  }

  @Test
  void runShouldWrapCheckedExceptions() {
    // unchecked exceptions are rethrown without being wrapped
    final var unchecked = new RuntimeException();
    assertRun(
        () -> {
          throw unchecked;
        },
        t -> assertSame(unchecked, t));

    // checked exceptions are wrapped
    final var checked = new IOException();
    assertRun(
        () -> {
          throw checked;
        },
        t -> assertIsWrapperFor(checked, t));
  }

  private void assertCall(Callable<?> callable, Consumer<Throwable> assertion) {
    try {
      CheckedExceptionWrapper.call(callable);
      fail("call() should have thrown an exception");
    } catch (AssertionFailedError ex) {
      // we don't want to test the exception thrown by fail()
      throw ex;
    } catch (Throwable t) {
      assertion.accept(t);
    }
  }

  private void assertRun(ThrowingRunnable<?> runnable, Consumer<Throwable> assertion) {
    try {
      CheckedExceptionWrapper.run(runnable);
      fail("run() should have thrown an exception");
    } catch (AssertionFailedError ex) {
      // we don't want to test the exception thrown by fail()
      throw ex;
    } catch (Throwable t) {
      assertion.accept(t);
    }
  }

  private void assertIsWrapperFor(Throwable realException, Throwable wrappedException) {
    assertInstanceOf(CheckedExceptionWrapper.class, wrappedException);
    assertSame(realException, wrappedException.getCause());
  }

  private void assertUnwrapsTo(Throwable realException, Throwable wrappedException) {
    assertInstanceOf(CheckedExceptionWrapper.class, wrappedException);
    assertSame(realException, ((CheckedExceptionWrapper) wrappedException).unwrap());
  }
}
