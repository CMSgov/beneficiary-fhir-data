package gov.cms.bfd.sharedutils.exceptions;

import com.google.common.base.Preconditions;
import gov.cms.bfd.sharedutils.interfaces.ThrowingRunnable;
import java.util.concurrent.Callable;

/**
 * In many places we implement lambdas that are not allowed to throw checked exceptions but call
 * methods that might throw them. One approach to that is a try block that wraps every exception in
 * a new {@link RuntimeException} and throws it but that can lead to unnecessary wrapping if the
 * caught exception was already unchecked.
 *
 * <p>This class provides a more efficient wrapper with static methods that can wrap exceptions only
 * when necessary and a getter for the underlying exception. There are also methods for executing
 * code in a block that takes care of catching and wrapping exceptions so that callers don't need a
 * try/catch block.
 */
public class CheckedExceptionWrapper extends RuntimeException {
  /**
   * Initializes an instance that wraps the given {@link Throwable}.
   *
   * @param cause the real exception
   */
  public CheckedExceptionWrapper(Throwable cause) {
    super(cause);
    Preconditions.checkNotNull(cause, "cause cannot be null");
  }

  /**
   * Returns the original, unwrapped exception.
   *
   * @return the unwrapped exception
   */
  public Throwable unwrap() {
    Throwable answer = getCause();
    assert answer != null;

    // Continue in case we have nested wrappers.  This drills down to the
    // first non-wrapper exception.
    while (answer instanceof CheckedExceptionWrapper wrapper) {
      answer = wrapper.getCause();
    }

    return answer;
  }

  /**
   * Call the given lambda and return its result. If the lambda throws a checked exception a wrapper
   * containing the real exception will be thrown. If the lambda throws an unchecked exception it
   * will be rethrown unchanged.
   *
   * @param callable a lambda to call
   * @return the result of the function
   * @param <T> the type of value returned by the function
   */
  public static <T> T call(Callable<T> callable) {
    try {
      return callable.call();
    } catch (Exception ex) {
      throw unchecked(ex);
    }
  }

  /**
   * Call the given lambda. If the lambda throws a checked exception a wrapper containing the real
   * exception will be thrown. If the function throws an unchecked exception it will be rethrown
   * unchanged.
   *
   * @param runnable a lambda to call
   */
  public static void run(ThrowingRunnable<?> runnable) {
    try {
      runnable.run();
    } catch (Throwable ex) {
      throw unchecked(ex);
    }
  }

  /**
   * Wraps a checked exception or returns an unchecked exception directly.
   *
   * @param exceptionToWrap the exception to wrap
   * @return the exception or a wrapper containing it
   */
  public static RuntimeException unchecked(Throwable exceptionToWrap) {
    if (exceptionToWrap instanceof RuntimeException runtimeException) {
      return runtimeException;
    } else {
      return new CheckedExceptionWrapper(exceptionToWrap);
    }
  }

  /**
   * If the given exception is a wrapper it is unwrapped. Otherwise it is returned unchanged.
   *
   * @param exceptionToUnwrap exception to unwrap if possible
   * @return the unwrapped exception
   */
  public static Throwable unwrapped(Throwable exceptionToUnwrap) {
    if (exceptionToUnwrap instanceof CheckedExceptionWrapper wrapper) {
      exceptionToUnwrap = wrapper.unwrap();
    }
    return exceptionToUnwrap;
  }
}
