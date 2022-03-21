package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/**
 * GrpcStreamCaller implementation that calls the RDA McsClaim service. At this stage in RDA API
 * development there is no way to resume a stream from a given point in time so every time the
 * service is called it sends all of its values.
 */
public class McsClaimStreamCaller extends GrpcStreamCaller<McsClaimChange> {
  public McsClaimStreamCaller() {
    super(LoggerFactory.getLogger(McsClaimStreamCaller.class));
  }

  /**
   * Calls the getMcsClaims RPC and returns a blocking {@link GrpcResponseStream} of the results.
   *
   * @param channel an already open channel to the service being called
   * @param startingSequenceNumber specifies the sequence number to send to the RDA API server
   * @return a blocking GrpcResponseStream of {@link McsClaimChange} entity objects
   * @throws Exception passes through any gRPC framework exceptions
   */
  @Override
  public GrpcResponseStream<McsClaimChange> callService(
      ManagedChannel channel, CallOptions callOptions, long startingSequenceNumber)
      throws Exception {
    logger.info("calling service");
    Preconditions.checkNotNull(channel);
    final ClaimRequest request = ClaimRequest.newBuilder().setSince(startingSequenceNumber).build();
    final MethodDescriptor<ClaimRequest, McsClaimChange> method =
        RDAServiceGrpc.getGetMcsClaimsMethod();
    final ClientCall<ClaimRequest, McsClaimChange> call = channel.newCall(method, callOptions);
    final Iterator<McsClaimChange> apiResults =
        ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, apiResults);
  }
}
