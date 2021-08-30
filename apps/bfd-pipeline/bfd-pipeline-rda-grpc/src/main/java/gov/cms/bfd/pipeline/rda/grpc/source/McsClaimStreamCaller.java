package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.pipeline.rda.grpc.RdaChange.MIN_SEQUENCE_NUM;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GrpcStreamCaller implementation that calls the RDA McsClaim service. At this stage in RDA API
 * development there is no way to resume a stream from a given point in time so every time the
 * service is called it sends all of its values.
 */
public class McsClaimStreamCaller implements GrpcStreamCaller<RdaChange<PreAdjMcsClaim>> {
  static final Logger LOGGER = LoggerFactory.getLogger(McsClaimStreamCaller.class);

  private final McsClaimTransformer transformer;

  public McsClaimStreamCaller(McsClaimTransformer transformer) {
    this.transformer = transformer;
  }

  /**
   * Calls the getMcsClaims RPC. The Iterator from the RPC call is wrapped with a transforming
   * Iterator that converts the API McsClaim objects into database PreAdjMcsClaim entity objects.
   *
   * @param channel an already open channel to the service being called
   * @return a blocking GrpcResponseStream of PreAdjMcsClaim entity objects
   * @throws Exception passes through any gRPC framework exceptions
   */
  @Override
  public GrpcResponseStream<RdaChange<PreAdjMcsClaim>> callService(ManagedChannel channel)
      throws Exception {
    LOGGER.info("calling service");
    Preconditions.checkNotNull(channel);
    final ClaimRequest request = ClaimRequest.newBuilder().setSince(MIN_SEQUENCE_NUM).build();
    final MethodDescriptor<ClaimRequest, McsClaimChange> method =
        RDAServiceGrpc.getGetMcsClaimsMethod();
    final ClientCall<ClaimRequest, McsClaimChange> call =
        channel.newCall(method, CallOptions.DEFAULT);
    final Iterator<McsClaimChange> apiResults =
        ClientCalls.blockingServerStreamingCall(call, request);
    final Iterator<RdaChange<PreAdjMcsClaim>> transformedResults =
        Iterators.transform(apiResults, transformer::transformClaim);
    return new GrpcResponseStream<>(call, transformedResults);
  }
}
