package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import java.util.NoSuchElementException;

/**
 * Trivial implementation of ClaimSource that returns no objects at all. Useful when only one type
 * of claim is needed for a given configuration of RdaService.
 *
 * @param <T> the type parameter
 */
public class EmptyMessageSource<T> implements MessageSource<T> {

  /**
   * Nothing to skip so just return this.
   *
   * <p>{@inheritDoc}
   *
   * @param startingSequenceNumber number of records to skip past
   * @return this
   */
  @Override
  public MessageSource<T> skipTo(long startingSequenceNumber) {
    return this;
  }

  /**
   * Always returns false.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public boolean hasNext() {
    return false;
  }

  /**
   * We never have a value so always throw {@link NoSuchElementException}.
   *
   * <p>{@inheritDoc}
   *
   * @throws NoSuchElementException because we never have a value
   */
  @Override
  public T next() throws Exception {
    throw new NoSuchElementException();
  }

  @Override
  public ClaimSequenceNumberRange getSequenceNumberRange() {
    return ClaimSequenceNumberRange.newBuilder().setLower(0).setUpper(0).build();
  }

  @Override
  public void close() throws Exception {}
}
