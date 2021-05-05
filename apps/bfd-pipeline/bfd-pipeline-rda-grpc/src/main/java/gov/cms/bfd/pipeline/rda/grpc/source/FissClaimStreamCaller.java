package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
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
public class FissClaimStreamCaller implements GrpcStreamCaller<FissClaim> {
  @Override
  public GrpcResponseStream<FissClaim> callService(ManagedChannel channel) throws Exception {
    Preconditions.checkNotNull(channel);
    final EmptyRequest request = EmptyRequest.newBuilder().build();
    ClientCall<EmptyRequest, FissClaim> call =
        channel.newCall(RDAServiceGrpc.getGetFissClaimsMethod(), CallOptions.DEFAULT);
    final Iterator<FissClaim> results = ClientCalls.blockingServerStreamingCall(call, request);
    return new GrpcResponseStream<>(call, results);
  }
}
