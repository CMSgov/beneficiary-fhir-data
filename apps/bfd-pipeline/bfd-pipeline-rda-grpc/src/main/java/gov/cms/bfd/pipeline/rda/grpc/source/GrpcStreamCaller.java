package gov.cms.bfd.pipeline.rda.grpc.source;

import io.grpc.ManagedChannel;

/**
 * Interface for objects that call streaming gRPC service calls..
 *
 * @param <TResponse> Type of objects returned by the streaming service.
 */
public interface GrpcStreamCaller<TResponse> {
  /**
   * Make the call to the service and return a blocking GrpcResponseStream that allows results to be
   * traversed like an Iterator but also allows the stream to be cancelled.
   *
   * @param channel an already open channel to the service being called
   * @return a blocking Iterator over stream results
   * @throws Exception any exception will terminate the stream
   */
  GrpcResponseStream<TResponse> callService(ManagedChannel channel) throws Exception;
}
