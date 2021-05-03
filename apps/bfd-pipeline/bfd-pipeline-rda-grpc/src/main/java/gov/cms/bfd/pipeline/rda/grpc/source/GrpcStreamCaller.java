package gov.cms.bfd.pipeline.rda.grpc.source;

import io.grpc.ManagedChannel;
import java.util.Iterator;

/**
 * Interface for objects that handle streaming service calls and map their results to
 * PreAdjudicatedClaim objects.
 *
 * @param <T> Type of objects returned by the streaming service.
 */
public interface GrpcStreamCaller<T> {
  /**
   * Make the call to the service and return a blocking Iterator over the results.
   *
   * @return a blocking Iterator over stream results
   * @throws Exception any exception will terminate the stream
   */
  Iterator<T> callService() throws Exception;

  /**
   * An interface for factory objects that can create implementations tied to a specific channel.
   * This will be called by GrpcRdaSource when it needs to make a new call to the gRPC service.
   *
   * @param <T> type of objects returned by the streaming service.
   */
  @FunctionalInterface
  interface Factory<T> {
    GrpcStreamCaller<T> createCaller(ManagedChannel channel) throws Exception;
  }
}
