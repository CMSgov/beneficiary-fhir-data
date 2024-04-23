package gov.cms.bfd.sharedutils.interfaces;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import jakarta.annotation.Nullable;

/**
 * Functional Interface for creating {@link java.util.function.Consumer} lambdas that throw.
 *
 * @param <T> The consumed parameter type of the function.
 * @param <E> The exception that is thrown by the function.
 */
public interface ThrowingConsumer<T, E extends Throwable> {
  /**
   * Performs this operation on the given argument.
   *
   * @param t the input argument
   * @throws E to indicate an error
   */
  void accept(T t) throws E;

  /**
   * Adapter that allows this consumer to be used in place of a {@link ThrowingFunction} so that
   * logic for a function can also be used with a consumer. The return value is always null since it
   * is not expected to be used.
   *
   * @param t the input argument
   * @return null
   * @throws E to indicate an error
   */
  @CanIgnoreReturnValue
  @Nullable
  default Void executeAsFunction(T t) throws E {
    accept(t);
    return null;
  }
}
