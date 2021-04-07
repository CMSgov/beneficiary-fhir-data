package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.Iterator;

/**
 * Iterface for objects that handle streaming service calls and map their results to
 * PreAdjudicatedClaim objects.
 *
 * @param <T> Type of objects returned by the streaming service.
 */
public interface GrpcStreamCaller<T> {
  /**
   * Creates a stub for use in the callService() method. Generally this will be a blocking stub.
   *
   * @param channel communication channel created in advance by the caller
   * @throws Exception any exception will terminate the stream
   */
  void createStub(ManagedChannel channel) throws Exception;

  /**
   * Make the call to the service and return a blocking Iterator over the results.
   *
   * @param maxRunTime maximum remaining runtime for the service call.
   * @return a blocking Iterator over stream results
   * @throws Exception any exception will terminate the stream
   */
  Iterator<T> callService(Duration maxRunTime) throws Exception;

  /**
   * Convert the service result object into a PreAdjudicatedClaim object.
   *
   * @param result object returned by the service
   * @return PreAdjudicatedClaim constructed from the result object
   * @throws Exception any exception will terminate the stream
   */
  PreAdjudicatedClaim convertResultToClaim(T result) throws Exception;
}
