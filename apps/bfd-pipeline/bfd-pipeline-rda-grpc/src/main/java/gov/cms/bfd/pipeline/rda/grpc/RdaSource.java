package gov.cms.bfd.pipeline.rda.grpc;

/**
 * Interface for objects that retrieve objects from some source and pass them to a RdaSink for
 * processing. All implementations are AutoCloseable since they will generally hold a network or
 * database connection. Implementations of this class MUST provide orderly shutdown when their
 * thread receives an InterruptedException.
 *
 * @param <TMessage> RDA API message class
 * @param <TClaim> JPA entity class
 */
public interface RdaSource<TMessage, TClaim> extends AutoCloseable {
  /**
   * Perform a smoke test and return true if the test is successful.
   *
   * @param sink to process batches of objects
   * @return true if the test is successful
   * @throws Exception can be thrown during the test to indicate failure condition
   */
  default boolean performSmokeTest(RdaSink<TMessage, TClaim> sink) throws Exception {
    return true;
  }

  /**
   * Retrieve some number of objects from the source and pass them to the sink for processing.
   *
   * @param maxPerBatch maximum number of objects to collect into a batch before calling the sink
   * @param sink to process batches of objects
   * @return total number of objects processed (sum of results from calls to sink)
   * @throws ProcessingException wraps any error and includes number of records successfully
   *     processed before the error
   */
  int retrieveAndProcessObjects(int maxPerBatch, RdaSink<TMessage, TClaim> sink)
      throws ProcessingException;
}
