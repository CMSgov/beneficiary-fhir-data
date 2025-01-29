package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;

/**
 * Interface for objects that produce message objects from some source (e.g. a file, an array, a
 * database, etc). Mirrors the Iterator protocol but allows for unwrapped exceptions to be passed
 * through to the caller. Also adds a method to skip ahead in the stream and a close() method for
 * proper cleanup.
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
   * Returns the next available claim. Calling this method when hasNext() would return false throws
   * a NoSuchElementException. An exception could be thrown if the underlying source of data has an
   * error.
   *
   * @return the next claim in the sequence
   * @throws Exception any error in reading data could throw an exception here
   */
  T next() throws Exception;

  /**
   * Used when creating random or json based sources to skip past some records to reach a specific
   * desired sequence number record.
   *
   * @param startingSequenceNumber desired next sequence number
   * @return this source after skipping the records
   * @throws Exception if there is an issue getting the next claim
   */
  MessageSource<T> skipTo(long startingSequenceNumber) throws Exception;

  /**
   * Returns the current range of sequence numbers.
   *
   * @return sequence number range
   */
  ClaimSequenceNumberRange getSequenceNumberRange();
}
