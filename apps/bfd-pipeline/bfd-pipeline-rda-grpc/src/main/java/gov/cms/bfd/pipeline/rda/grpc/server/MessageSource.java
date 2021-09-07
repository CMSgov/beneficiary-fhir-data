package gov.cms.bfd.pipeline.rda.grpc.server;

import java.io.Closeable;
import java.util.function.Predicate;

/**
 * Interface for objects that produce FissClaim objects from some source (e.g. a file, an array, a
 * database, etc). Mirrors the Iterator protocol but allows for unwrapped exceptions to be passed
 * through to the caller and adds a close() method for proper cleanup.
 */
public interface MessageSource<T> extends Closeable {
  /**
   * Checks to determine if there is another object available. Always call this before calling
   * next(). A true value here indicates that there is data remaining to be consumed but does not
   * guarantee that next will not throw an exception.
   *
   * @return true if data is available and a call to next() is possible
   * @throws Exception any error in reading data could throw an exception here
   */
  boolean hasNext() throws Exception;

  /**
   * Returns the next available claim. Calling this method when hasNext() would return true throws a
   * NoSuchElementException. An exception could be thrown if the underlying source of data has an
   * error.
   *
   * @return the next claim in the sequence
   * @throws Exception any error in reading data could throw an exception here
   * @throws NoSuchElementException if this method is called when hasNext() would have returned
   *     false
   */
  T next() throws Exception;

  /**
   * Used when creating random or json based sources to skip past some records to reach a specific
   * desired sequence number record.
   *
   * @param numberToSkip number of records to skip past
   * @return this source after skipping the records
   */
  default MessageSource<T> skip(long numberToSkip) throws Exception {
    while (numberToSkip-- > 0 && hasNext()) {
      next();
    }
    return this;
  }

  /**
   * Filters objects from this source to only include objects for which the predicate returns true.
   *
   * @param predicate returns true for objects to keep and false for objects to skip
   * @return filtered version of this source
   */
  default MessageSource<T> filter(Predicate<T> predicate) {
    return new FilteredMessageSource<>(this, predicate);
  }

  @FunctionalInterface
  interface Factory<T> {
    MessageSource<T> apply(long sequenceNumber) throws Exception;
  }
}
