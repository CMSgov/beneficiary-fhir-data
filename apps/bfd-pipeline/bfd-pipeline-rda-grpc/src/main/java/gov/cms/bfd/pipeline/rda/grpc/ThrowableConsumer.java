package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Used to define lambdas that might throw a checked exception. Allowing the exception to pass
 * through makes it easier for specific unit tests to assert on it.
 */
@FunctionalInterface
public interface ThrowableConsumer<T> {
  void accept(T arg) throws Exception;
}
