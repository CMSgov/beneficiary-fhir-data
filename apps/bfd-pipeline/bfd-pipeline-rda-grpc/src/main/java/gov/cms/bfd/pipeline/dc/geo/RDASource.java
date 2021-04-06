package gov.cms.bfd.pipeline.dc.geo;

import java.time.Duration;

/**
 * Interface for objects that retrieve objects from some source and pass them to a sink for
 * processing. All implementations are AutoCloseable since they will generally hold a network or
 * database connection.
 *
 * @param <T> the type of objects processed
 */
public interface RDASource<T> extends AutoCloseable {
  /**
   * Retrieve some number of objects from the source and pass them to the sink for processing.
   *
   * @param maxToProcess maximum number of objects to retrieve from the source
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param maxRunTime maximum amount of time to run before returning
   * @param sink sink to receive batches of objects
   * @return total number of objects processed (sum of results from calls to sink)
   */
  int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RDASink<T> sink)
      throws ProcessingException;
}
