package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import java.util.function.Supplier;

/**
 * Wrapper for a real MessageSource that throws an exception after some number of messages have been
 * delivered. Intended for use in integration tests that use RdaServer.
 *
 * @param <T> type of objects being delivered
 */
public class ExceptionMessageSource<T> implements MessageSource<T> {
  /** The message source being wrapped. */
  private final MessageSource<T> source;

  /** Factory for the exception(s) to be thrown. */
  private final Supplier<Exception> exceptionFactory;

  /** The maximum number of messages to deliver before throwing an exception. */
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
  public MessageSource<T> skipTo(long startingSequenceNumber) throws Exception {
    source.skipTo(startingSequenceNumber);
    return this;
  }

  @Override
  public boolean hasNext() throws Exception {
    return source.hasNext();
  }

  /**
   * Works normally until configured number of messages have been returned. Then throws the
   * configured exception.
   *
   * <p>{@inheritDoc}
   *
   * @return value from real source
   * @throws Exception the configured exception or pass through from source
   */
  @Override
  public T next() throws Exception {
    if (remainingBeforeThrow <= 0) {
      throw exceptionFactory.get();
    }
    remainingBeforeThrow -= 1;
    return source.next();
  }

  @Override
  public ClaimSequenceNumberRange getSequenceNumberRange() {
    return ClaimSequenceNumberRange.newBuilder().setLower(0).setUpper(remainingBeforeThrow).build();
  }

  @Override
  public void close() throws Exception {
    source.close();
  }
}
