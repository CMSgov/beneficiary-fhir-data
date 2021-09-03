package gov.cms.bfd.pipeline.rda.grpc.server;

/**
 * Interface for objects that produce FissClaim objects from some source (e.g. a file, an array, a
 * database, etc). Mirrors the Iterator protocol but allows for unwrapped exceptions to be passed
 * through to the caller and adds a close() method for proper cleanup.
 */
public interface MessageSource<T> extends AutoCloseable {
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
}
