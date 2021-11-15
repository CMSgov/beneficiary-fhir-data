package gov.cms.bfd.pipeline.rda.grpc.server;

import java.util.NoSuchElementException;

/**
 * Trivial implementation of ClaimSource that returns no objects at all. Useful when only one type
 * of claim is needed for a given configuration of RdaService.
 *
 * @param <T>
 */
public class EmptyMessageSource<T> implements MessageSource<T> {

  /**
   * Factory method for use when creating RdaService instances that return no messages. Uses a
   * method rather than a static value so that types are inferred correctly by the compiler.
   *
   * @param <T> type of message being returned
   * @return a factory object
   */
  public static <T> MessageSource.Factory<T> factory() {
    return ignored -> new EmptyMessageSource<>();
  }

  @Override
  public boolean hasNext() throws Exception {
    return false;
  }

  @Override
  public T next() throws Exception {
    throw new NoSuchElementException();
  }

  @Override
  public void close() throws Exception {}
}
