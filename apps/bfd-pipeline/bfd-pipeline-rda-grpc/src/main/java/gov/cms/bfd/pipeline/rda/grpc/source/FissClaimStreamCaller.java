package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.mpsm.rda.v1.EmptyRequest;
import gov.cms.mpsm.rda.v1.FissClaim;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/** GrpcStreamCaller implementation that calls the RDA HealthCheck service. */
public class FissClaimStreamCaller implements GrpcStreamCaller<FissClaim> {
  private final RDAServiceGrpc.RDAServiceBlockingStub stub;

  public FissClaimStreamCaller(ManagedChannel channel) {
    stub = RDAServiceGrpc.newBlockingStub(channel);
  }

  @Override
  public Iterator<FissClaim> callService(Duration maxRunTime) throws Exception {
    final EmptyRequest request = EmptyRequest.newBuilder().build();
    return stub.withDeadlineAfter(maxRunTime.toMillis(), TimeUnit.MILLISECONDS)
        .getFissClaims(request);
  }

  @Override
  public PreAdjudicatedClaim convertResultToClaim(FissClaim ignored) throws Exception {
    return new PreAdjudicatedClaim();
  }

  public static GrpcStreamCaller.Factory<FissClaim> createFactory() {
    return FissClaimStreamCaller::new;
  }
}
