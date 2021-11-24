package gov.cms.bfd.sharedutils.interfaces;

/**
 * Functional Interface for creating {@link java.util.function.Function} lambdas that throw.
 *
 * @param <R> The return type of the function.
 * @param <T> The consumed parameter type of the function.
 * @param <E> The exception that is thrown by the function.
 */
@FunctionalInterface
public interface ThrowingFunction<R, T, E extends Throwable> {
  R apply(T value) throws E;
}
