package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ApiVersion;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;

/**
 * Abstract base class for objects that call specific RPCs to open a stream of incoming data.
 * Particular implementations deal with issues such as: which RPC to call, how to construct proper
 * request parameters, and (potentially) how to map the incoming response objects into our own
 * internal objects. The latter could be done here or on the RdaSink that ultimately processes the
 * data. No requirement is made on where that mapping is performed.
 *
 * @param <TResponse> Type of objects returned by to the client by this caller. Generally either
 *     FissClaimChange or McsClaimChange in real code
 */
public abstract class GrpcStreamCaller<TResponse> {
  protected final Logger logger;

  protected GrpcStreamCaller(Logger logger) {
    this.logger = logger;
  }

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
  public abstract GrpcResponseStream<TResponse> callService(
      ManagedChannel channel, CallOptions callOptions, long startingSequenceNumber)
      throws Exception;

  /**
   * Make a call to the server's {@code getVersion()} service and return the version component.
   *
   * @param channel an already open channel to the server
   * @param callOptions the call options
   * @return version string from the server
   * @throws Exception if an IO issue occurs
   */
  public String callVersionService(ManagedChannel channel, CallOptions callOptions)
      throws Exception {
    Preconditions.checkNotNull(channel);
    logger.info("calling getVersion service");
    final MethodDescriptor<Empty, ApiVersion> method = RDAServiceGrpc.getGetVersionMethod();
    final ClientCall<Empty, ApiVersion> call = channel.newCall(method, callOptions);
    final Iterator<ApiVersion> apiResults =
        ClientCalls.blockingServerStreamingCall(call, Empty.getDefaultInstance());
    // Oddly enough the server returns a stream.  Rather than hard code an assumption of one
    // result object this loop ensures we fully consume the stream if multiple objects are
    // received for some reason.
    String answer = "";
    while (apiResults.hasNext()) {
      ApiVersion response = apiResults.next();
      logger.info(
          "getVersion service response: version='{}' commitId='{}' buildTime='{}'",
          response.getVersion(),
          response.getCommitId(),
          response.getBuildTime());
      if (!Strings.isNullOrEmpty(response.getVersion())) {
        if (!answer.isEmpty()) {
          throw new IOException("RDA API Server returned multiple non-empty version strings");
        }
        answer = response.getVersion();
      }
    }
    if (answer.isEmpty()) {
      throw new IOException("RDA API Server did not return a non-empty version string");
    }
    return answer;
  }
}
