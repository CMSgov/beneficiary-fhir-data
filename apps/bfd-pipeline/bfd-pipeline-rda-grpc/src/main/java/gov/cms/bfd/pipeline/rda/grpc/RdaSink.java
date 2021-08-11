package gov.cms.bfd.pipeline.rda.grpc;

import java.util.Collection;
import java.util.Collections;

/**
 * Interface for objects that process incoming objects. At least one of the methods must be
 * implemented. All implementations are AutoCloseable since they will generally hold a database
 * connection.
 *
 * <p>All implementations MUST be idempotent. If the same object is written multiple times the sink
 * must ensure that it does not create duplicates or overwrite newer information.
 *
 * @param <T> the type of objects processed
 */
public interface RdaSink<T> extends AutoCloseable {
  /**
   * Write the object to the data store and return the number of objects successfully written.
   *
   * <p>Implementations MUST provide a non-default implementation of either writeObject() or
   * writeBatch() or both. The default implementations call one another so implementing only one of
   * the two provides a usable implementation for the other.
   *
   * @param object single object to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeObject(T object) throws ProcessingException {
    return writeBatch(Collections.singleton(object));
  }

  /**
   * Write all of the objects to the data store and return the number of objects actually written.
   * Objects must be processed in the same order as they appear within the Iterable. Some Sinks can
   * support transactional batch processing (all or none) but others might default to processing one
   * object at a time and can successfully process some portion of the batch before an exception is
   * thrown. An exception is always thrown for errors.
   *
   * <p>Implementations MUST provide a non-default implementation of either writeObject() or
   * writeBatch() or both. The default implementations call one another so implementing only one of
   * the two provides a usable implementation for the other.
   *
   * @param objects zero or more objects to be written to the data store
   * @return number of objects successfully processed
   * @throws ProcessingException if the operation fails
   */
  default int writeBatch(Collection<T> objects) throws ProcessingException {
    int processedCount = 0;
    for (T object : objects) {
      try {
        processedCount += writeObject(object);
      } catch (ProcessingException ex) {
        throw new ProcessingException(
            (Exception) ex.getCause(), processedCount + ex.getProcessedCount());
      } catch (Exception ex) {
        throw new ProcessingException(ex, processedCount);
      }
    }
    return processedCount;
  }
}
