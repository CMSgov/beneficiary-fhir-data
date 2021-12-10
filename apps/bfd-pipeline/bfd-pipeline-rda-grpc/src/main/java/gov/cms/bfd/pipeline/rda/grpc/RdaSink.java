package gov.cms.bfd.pipeline.rda.grpc;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Interface for objects that process incoming objects. At least one of the methods must be
 * implemented. All implementations are AutoCloseable since they will generally hold a database
 * connection.
 *
 * <p>All implementations MUST be idempotent. If the same object is written multiple times the sink
 * must ensure that it does not create duplicates or overwrite newer information.
 *
 * @param <TMessage> the type of objects processed
 */
public interface RdaSink<TMessage, TClaim> extends AutoCloseable {
  /**
   * The pipeline job passes a starting sequence number to the RDA API call to get a stream of
   * change objects for processing. This method allows the sink to provide the next logical starting
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
   * Used by callers to remove duplicates from a collection of objects prior to calling writeBatch.
   *
   * @param object object to get a key from
   * @return a unique key to dedup objects of this type
   */
  String getDedupKeyForMessage(TMessage object);

  /**
   * Write the object to the data store and return the number of objects successfully written. The
   * count returned is just the most recent unreported processed count and for asynchronous sinks
   * can reflect values from previously submitted batches.
   *
   * <p>Implementations MUST provide a non-default implementation of either writeObject() or
   * writeBatch() or both. The default implementations call one another so implementing only one of
   * the two provides a usable implementation for the other.
   *
   * @param object single object to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeMessage(String dataVersion, TMessage object) throws ProcessingException {
    return writeMessages(dataVersion, Collections.singleton(object));
  }

  /**
   * Write all of the objects to the data store and return the number of objects actually written.
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
   * @param objects zero or more objects to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeMessages(String dataVersion, Collection<TMessage> objects)
      throws ProcessingException {
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

  @Nonnull
  TClaim transformMessage(String apiVersion, TMessage message);

  int writeClaims(String dataVersion, Collection<TClaim> objects) throws ProcessingException;

  /**
   * Return count of records processed since the most recent call to a write method or this method.
   * Resets the counter back to zero.
   *
   * @return unreported number of processed records
   */
  int getProcessedCount() throws ProcessingException;

  void shutdown(Duration waitTime) throws ProcessingException;

  void updateLastSequenceNumber(long lastSequenceNumber);

  long getSequenceNumberForObject(TMessage object);
}
