package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Interface for objects that retrieve objects from some source and pass them to a sink for
 * processing. All implementations are AutoCloseable since they will generally hold a network or
 * database connection.
 *
 * @param <T> the type of objects processed
 */
public interface RdaSource<T> extends AutoCloseable {
  /**
   * Retrieve some number of objects from the source and pass them to the sink for processing.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to receive batches of objects
   * @return total number of objects processed (sum of results from calls to sink)
   */
  int retrieveAndProcessObjects(int maxPerBatch, RdaSink<T> sink) throws ProcessingException;
}
