package gov.cms.bfd.pipeline.rda.grpc.source;

import io.grpc.ManagedChannel;

/**
 * Interface for objects that call specific RPCs to open a stream of incoming data. Particular
 * implementations of this interface deal with issues such as: which RPC to call, how to construct
 * proper request parameters, and (potentially) how to map the incoming response objects into our
 * own internal objects. The latter could be done here or on the RdaSink that ultimately processes
 * the data. No requirement is made on where that mapping is performed.
 *
 * @param <TResponse> Type of objects returned by to the client by this caller.
 */
public interface GrpcStreamCaller<TResponse> {
  /**
   * Make the call to the service and return a blocking GrpcResponseStream that allows results to be
   * traversed like an Iterator but also allows the stream to be cancelled at any time.
   *
   * @param channel an already open channel to the service being called
   * @return a blocking GrpcResponseStream allowing iteration over stream results
   * @throws Exception any exception thrown calling the RPC or setting up the stream
   */
  GrpcResponseStream<TResponse> callService(ManagedChannel channel) throws Exception;
}
