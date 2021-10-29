package gov.cms.bfd.pipeline.rda.grpc.source;

import io.grpc.CallOptions;
import io.grpc.ManagedChannel;

/**
 * Interface for objects that call specific RPCs to open a stream of incoming data. Particular
 * implementations of this interface deal with issues such as: which RPC to call, how to construct
 * proper request parameters, and (potentially) how to map the incoming response objects into our
 * own internal objects. The latter could be done here or on the RdaSink that ultimately processes
 * the data. No requirement is made on where that mapping is performed.
 *
 * @param <TResponse> Type of objects returned by to the client by this caller. Generally either
 *     FissClaimChange or McsClaimChange in real code
 */
public interface GrpcStreamCaller<TResponse> {
  /**
   * Make the call to the service and return a blocking GrpcResponseStream that allows results to be
   * traversed like an Iterator but also allows the stream to be cancelled at any time. The starting
   * sequence number allows the caller to resume the stream at an arbitrary location. The sequence
   * numbers come back from the API server in change objects.
   *
   * @param channel an already open channel to the service being called
   * @param callOptions the CallOptions object to use for the API call
   * @param startingSequenceNumber specifies the sequence number to send to the RDA API server
   * @return a blocking GrpcResponseStream allowing iteration over stream results
   * @throws Exception any exception thrown calling the RPC or setting up the stream
   */
  GrpcResponseStream<TResponse> callService(
      ManagedChannel channel, CallOptions callOptions, long startingSequenceNumber)
      throws Exception;
}
