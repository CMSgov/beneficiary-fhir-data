package gov.cms.bfd.pipeline.rda.grpc.server;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Wrapper around a MessageSource to allow the values to be filtered using some condition. Generally
 * the filter will be a minimum sequence number.
 *
 * @param <T> type of objects being returned by the source.
 */
public class FilteredMessageSource<T> implements MessageSource<T> {
  private final MessageSource<T> source;
  private final Predicate<T> filter;
  private T nextValue;

  /**
   * Wrap the specified source and filter its objects using the supplied predicate. The predicate
   * must return true for objects that are valid and false for objects that are invalid.
   *
   * @param source real source of objects
   * @param filter function returning true for objects to keep or false for objects to skip
   */
  public FilteredMessageSource(MessageSource<T> source, Predicate<T> filter) {
    this.source = source;
    this.filter = filter;
  }

  @Override
  public boolean hasNext() throws Exception {
    // allow for cases of hasNext() being called multiple times in a row
    if (nextValue != null) {
      return true;
    }

    while (source.hasNext()) {
      nextValue = source.next();
      if (filter.test(nextValue)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public T next() throws Exception {
    if (nextValue == null) {
      throw new NoSuchElementException();
    }
    T answer = nextValue;
    nextValue = null;
    return answer;
  }

  @Override
  public void close() throws IOException {
    source.close();
  }
}
