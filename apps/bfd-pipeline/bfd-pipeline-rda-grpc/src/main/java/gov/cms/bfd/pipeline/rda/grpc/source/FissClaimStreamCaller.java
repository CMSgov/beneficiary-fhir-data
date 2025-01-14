package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.util.Iterator;
import org.slf4j.LoggerFactory;

/** GrpcStreamCaller implementation that calls the RDA FissClaim service. */
public class FissClaimStreamCaller extends GrpcStreamCaller<FissClaimChange> {

  /** Instantiates a new Fiss claim stream caller. */
  public FissClaimStreamCaller() {
    super(LoggerFactory.getLogger(FissClaimStreamCaller.class));
  }

  /**
   * Calls the getFissClaims RPC using the provided {@link ManagedChannel} and {@link CallOptions}
   * and returns a {@link GrpcResponseStream} that can be used to receive the results in a blocking
   * manner.
   *
   * @param channel an already open channel to the service being called
   * @param callOptions additional {@link CallOptions} for the RPC call.
   * @param startingSequenceNumber specifies the sequence number to send to the RDA API server
   * @return a blocking GrpcResponseStream of {@link FissClaimChange} objects
   * @throws Exception passes through any gRPC framework exceptions
   */
  @Override
  public GrpcResponseStream<FissClaimChange> callService(
      ManagedChannel channel, CallOptions callOptions, long startingSequenceNumber)
      throws Exception {
    logger.info("calling service");
    Preconditions.checkNotNull(channel);
    final ClaimRequest request = ClaimRequest.newBuilder().setSince(startingSequenceNumber).build();
    final MethodDescriptor<ClaimRequest, FissClaimChange> method =
        RDAServiceGrpc.getGetFissClaimsMethod();
    final ClientCall<ClaimRequest, FissClaimChange> call = channel.newCall(method, callOptions);
    final Iterator<FissClaimChange> apiResults =
        ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, apiResults);
  }

  @Override
  public ClaimSequenceNumberRange callSequenceNumberRangeService(
      ManagedChannel channel, CallOptions callOptions) {
    final MethodDescriptor<Empty, ClaimSequenceNumberRange> method =
        RDAServiceGrpc.getGetFissClaimsSequenceNumberRangeMethod();
    final ClientCall<Empty, ClaimSequenceNumberRange> call = channel.newCall(method, callOptions);
    return ClientCalls.blockingUnaryCall(call, Empty.getDefaultInstance());
  }
}
