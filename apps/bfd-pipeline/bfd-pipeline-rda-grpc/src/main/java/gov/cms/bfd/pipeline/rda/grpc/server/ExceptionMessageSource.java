package gov.cms.bfd.pipeline.rda.grpc.server;

import java.util.function.Supplier;

/**
 * Wrapper for a real MessageSource that throws an exception after some number of messages have been
 * delivered. Intended for use in integration tests that use RdaServer.
 *
 * @param <T> type of objects being delivered
 */
public class ExceptionMessageSource<T> implements MessageSource<T> {
  private final MessageSource<T> source;
  private final Supplier<Exception> exceptionFactory;
  private int remainingBeforeThrow;

  /**
   * Creates a new instance that delivers at most countBeforeThrow messages from source and then
   * throws an exception created by a call to exceptionFactory.
   *
   * @param source actual MessageSource
   * @param countBeforeThrow maximum number of messages to deliver before throwing
   * @param exceptionFactory factory to create the exception
   */
  public ExceptionMessageSource(
      MessageSource<T> source, int countBeforeThrow, Supplier<Exception> exceptionFactory) {
    this.source = source;
    this.exceptionFactory = exceptionFactory;
    remainingBeforeThrow = countBeforeThrow;
  }

  @Override
  public boolean hasNext() throws Exception {
    return source.hasNext();
  }

  @Override
  public T next() throws Exception {
    if (remainingBeforeThrow <= 0) {
      throw exceptionFactory.get();
    }
    remainingBeforeThrow -= 1;
    return source.next();
  }

  @Override
  public void close() throws Exception {
    source.close();
  }
}
