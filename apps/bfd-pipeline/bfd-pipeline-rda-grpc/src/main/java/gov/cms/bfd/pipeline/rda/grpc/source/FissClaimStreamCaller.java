package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.mpsm.rda.v1.EmptyRequest;
import gov.cms.mpsm.rda.v1.FissClaim;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.ManagedChannel;
import java.util.Iterator;

/** GrpcStreamCaller implementation that calls the RDA HealthCheck service. */
public class FissClaimStreamCaller implements GrpcStreamCaller<FissClaim> {
  private final RDAServiceGrpc.RDAServiceBlockingStub stub;

  public FissClaimStreamCaller(ManagedChannel channel) {
    stub = RDAServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public Iterator<FissClaim> callService() throws Exception {
    final EmptyRequest request = EmptyRequest.newBuilder().build();
    return stub.getFissClaims(request);
  }

  public static GrpcStreamCaller.Factory<FissClaim> createFactory() {
    return FissClaimStreamCaller::new;
  }
}
