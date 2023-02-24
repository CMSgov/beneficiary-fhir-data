package gov.cms.bfd.sharedutils.interfaces;

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
}
