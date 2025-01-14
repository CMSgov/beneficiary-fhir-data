package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/** GrpcStreamCaller implementation that calls the RDA McsClaim service. */
public class McsClaimStreamCaller extends GrpcStreamCaller<McsClaimChange> {
  /** Instantiates a new Mcs claim stream caller. */
  public McsClaimStreamCaller() {
    super(LoggerFactory.getLogger(McsClaimStreamCaller.class));
  }

  /**
   * Calls the getMcsClaims RPC using the provided {@link ManagedChannel} and {@link CallOptions}
   * and returns a {@link GrpcResponseStream} that can be used to receive the results in a blocking
   * manner.
   *
   * @param channel an already open channel to the service being called
   * @param callOptions additional {@link CallOptions} for the RPC call.
   * @param startingSequenceNumber specifies the sequence number to send to the RDA API server
   * @return a blocking GrpcResponseStream of {@link McsClaimChange} objects
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

  @Override
  public ClaimSequenceNumberRange callSequenceNumberRangeService(
      ManagedChannel channel, CallOptions callOptions) {
    final MethodDescriptor<Empty, ClaimSequenceNumberRange> method =
        RDAServiceGrpc.getGetMcsClaimsSequenceNumberRangeMethod();
    final ClientCall<Empty, ClaimSequenceNumberRange> call = channel.newCall(method, callOptions);
    return ClientCalls.blockingUnaryCall(call, Empty.getDefaultInstance());
  }
}
