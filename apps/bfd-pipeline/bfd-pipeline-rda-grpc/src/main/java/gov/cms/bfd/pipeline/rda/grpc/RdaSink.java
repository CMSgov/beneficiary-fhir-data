package gov.cms.bfd.pipeline.rda.grpc;

import gov.cms.bfd.model.rda.MessageError;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Interface for objects that process incoming RDA API messages and write them to the database in
 * some manner. Implementations may be either single threaded or multi-threaded.
 *
 * <p>At least one of the write methods (writeMessage or writeMessages) must be implemented.
 *
 * <p>All implementations are AutoCloseable since they will generally hold a database connection
 * and/or thread pool.
 *
 * <p>All implementations MUST be idempotent. If the same messages/claims are written multiple times
 * the sink must ensure that it does not create duplicates.
 *
 * @param <TMessage> type of RDA API messages written to the database
 * @param <TClaim> type of entity objects written to the database
 */
public interface RdaSink<TMessage, TClaim> extends AutoCloseable {
  /**
   * The pipeline job passes a starting sequence number to the RDA API to get a stream of change
   * objects for processing. This method allows the sink to provide the next logical starting
   * sequence number for the call. An Optional is returned to handle the case when no records have
   * been added to the database yet.
   *
   * @return Possibly empty Optional containing highest recorded sequence number.
   * @throws ProcessingException if the operation fails
   */
  default Optional<Long> readMaxExistingSequenceNumber() throws ProcessingException {
    return Optional.empty();
  }

  /**
   * Write the specified lastSequenceNumber to the database. This write could happen in the current
   * thread or in a background thread depending on the sink implementation.
   *
   * @param lastSequenceNumber value to write to the database
   */
  void updateLastSequenceNumber(long lastSequenceNumber);

  /**
   * Hook to allow the {@link RdaSource} to avoid processing messages that are invalid and should
   * not be stored. Specifically this is to filter out FISS claims with specific invalid DCN values.
   * Adding the detection logic here allows it to be applied generically. Defaults to true so that
   * no specific implementation is necessary for MCS claims.
   *
   * @param message Message received from the RDA API
   * @return true if the message is valid and should be processed, false otherwise
   */
  default boolean isValidMessage(TMessage message) {
    return true;
  }

  /**
   * Called to determine if a message represents a {@link
   * gov.cms.mpsm.rda.v1.ChangeType#CHANGE_TYPE_DELETE} change. We filter these messages because
   * they do not have any meaning within the BFD system.
   *
   * @param message message to check
   * @return true if the message is a delete
   */
  default boolean isDeleteMessage(TMessage message) {
    return false;
  }

  /**
   * Write the object to the data store and return the number of objects successfully written. The
   * count returned is just the most recent unreported processed count and for asynchronous sinks
   * can reflect values from previously submitted batches.
   *
   * <p>Implementations MUST provide a non-default implementation of either writeObject() or
   * writeBatch() or both. The default implementations call one another so implementing only one of
   * the two provides a usable implementation for the other.
   *
   * @param dataVersion value for the apiSource column of the claim record
   * @param object single object to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeMessage(String dataVersion, TMessage object) throws ProcessingException {
    return writeMessages(dataVersion, List.of(object));
  }

  /**
   * Writes out the transformation error for the given message and given apiVersion. The logic for
   * this method is defined by the implementing child.
   *
   * @param apiVersion The version of the api used to get the message.
   * @param message The message that was being transformed when the error occurred.
   * @param exception The exception that was thrown while transforming the message.
   * @throws IOException If there was an issue writing the error out
   * @throws ProcessingException If there was a problem processing the claim
   */
  default void writeError(
      String apiVersion, TMessage message, DataTransformer.TransformationException exception)
      throws IOException, ProcessingException {
    throw new UnsupportedOperationException();
  }

  /**
   * Checks if the error limit has been exceeded.
   *
   * @throws ProcessingException If the error limit was reached.
   */
  void checkErrorCount() throws ProcessingException;

  /**
   * Write all the objects to the data store and return the number of objects actually written.
   * Objects must be processed in the same order as they appear within the Iterable. Some Sinks can
   * support transactional batch processing (all or none) but others might default to processing one
   * object at a time and can successfully process some portion of the batch before an exception is
   * thrown. An exception is always thrown for errors. The count returned is just the most recent
   * unreported processed count and for asynchronous sinks can reflect values from previously
   * submitted batches.
   *
   * <p>Implementations MUST provide a non-default implementation of either writeObject() or
   * writeBatch() or both. The default implementations call one another so implementing only one of
   * the two provides a usable implementation for the other.
   *
   * @param dataVersion value for the apiSource column of the claim record
   * @param objects zero or more objects to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeMessages(String dataVersion, List<TMessage> objects) throws ProcessingException {
    int processedCount = 0;
    for (TMessage object : objects) {
      try {
        processedCount += writeMessage(dataVersion, object);
      } catch (ProcessingException ex) {
        throw new ProcessingException(
            (Exception) ex.getCause(), processedCount + ex.getProcessedCount());
      } catch (Exception ex) {
        throw new ProcessingException(ex, processedCount);
      }
    }
    return processedCount;
  }

  /**
   * The primary key for the claim contained in the message. Used by callers to remove duplicates
   * from a collection of objects prior to calling writeMessages. Callers may log this value so it
   * must not contain any PII or PHI.
   *
   * @param object object to get a key from
   * @return a unique key to dedup objects of this type
   */
  String getClaimIdForMessage(TMessage object);

  /**
   * Extract the sequence number from the message object and return it.
   *
   * @param object object to get the sequence number from
   * @return the sequence number within the message object
   */
  long getSequenceNumberForObject(TMessage object);

  /**
   * Use the provided RDA API message object plus the API version string to produce an appropriate
   * entity object for writing to the database. This operation is provided by the sink because the
   * sink has to be aware of the specific types involved and also because that allows the message
   * transformation to be performed in worker threads rather than in the main thread.
   *
   * @param apiVersion appropriate string for the apiSource column of the claim table
   * @param message an RDA API message object of the correct type for this sync
   * @return an optional containing the appropriate entity object containing the data from the
   *     message if successfully converted, {@link Optional#empty()} otherwise
   * @throws IOException if there was an issue writing out a {@link MessageError}
   * @throws ProcessingException if there was an issue transforming the message
   */
  @Nonnull
  Optional<TClaim> transformMessage(String apiVersion, TMessage message)
      throws IOException, ProcessingException;

  /**
   * Write the specified collection of entity objects to the database. This write could happen in
   * the current thread or in a background thread depending on the sink implementation.
   *
   * @param objects collection of entity objects to be written to the database
   * @return number of claims processed since last call to writeClaims or getProcessedCount
   * @throws ProcessingException if any exceptions are thrown they are wrapped in a
   *     ProcessingException
   */
  int writeClaims(Collection<TClaim> objects) throws ProcessingException;

  /**
   * Return count of records processed since the most recent call to a write method or this method.
   * Calls to this method collect the current value and resets the counter. The sum of this method's
   * results plus all writeClaims returns should equal the total number of objects processed.
   * Synchronous sinks will return 0 from this method but asynchronous sinks will return non-zero
   * values sometimes as they track completion of background writes.
   *
   * @return unreported number of processed records
   * @throws ProcessingException if the operation fails
   */
  int getProcessedCount() throws ProcessingException;

  /**
   * Causes the sink to cleanly shut down any background tasks and wait for them to complete.
   *
   * @param waitTime maximum amount of time to wait for shutdown to complete
   * @throws ProcessingException if any exceptions are thrown they are wrapped in a
   *     ProcessingException
   */
  void shutdown(Duration waitTime) throws ProcessingException;
}
