package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.mpsm.rda.v1.EmptyRequest;
import gov.cms.mpsm.rda.v1.FissClaim;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.stub.ClientCalls;
import java.util.Iterator;

/**
 * GrpcStreamCaller implementation that calls the RDA FissClaim service. At this stage in RDA API
 * development there is no way to resume a stream from a given point in time so every time the
 * service is called it sends all of its values.
 */
public class FissClaimStreamCaller implements GrpcStreamCaller<PreAdjFissClaim> {
  private final FissClaimTransformer transformer;

  public FissClaimStreamCaller(FissClaimTransformer transformer) {
    this.transformer = transformer;
  }

  /**
   * Calls the getFissClaims RPC. The Iterator from the RPC call is wrapped with a transforming
   * Iterator that converts the API FissClaim objects into database PreAdjFissClaim entity objects.
   *
   * @param channel an already open channel to the service being called
   * @return a blocking GrpcResponseStream of PreAdjFissClaim entity objects
   * @throws Exception passes through any gRPC framework exceptions
   */
  @Override
  public GrpcResponseStream<PreAdjFissClaim> callService(ManagedChannel channel) throws Exception {
    Preconditions.checkNotNull(channel);
    final EmptyRequest request = EmptyRequest.newBuilder().build();
    ClientCall<EmptyRequest, FissClaim> call =
        channel.newCall(RDAServiceGrpc.getGetFissClaimsMethod(), CallOptions.DEFAULT);
    final Iterator<FissClaim> apiResults = ClientCalls.blockingServerStreamingCall(call, request);
    final Iterator<PreAdjFissClaim> transformedResults =
        Iterators.transform(apiResults, transformer::transformClaim);
    return new GrpcResponseStream<>(call, transformedResults);
  }
}
