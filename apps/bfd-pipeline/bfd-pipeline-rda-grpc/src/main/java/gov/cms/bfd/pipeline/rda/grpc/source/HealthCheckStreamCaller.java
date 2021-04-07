package gov.cms.bfd.pipeline.rda.grpc.source;

import com.nava.health.v1.HealthCheckRequest;
import com.nava.health.v1.HealthCheckResponse;
import com.nava.health.v1.HealthGrpc;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/** GrpcStreamCaller implementation that calls the RDA HealthCheck service. */
public class HealthCheckStreamCaller implements GrpcStreamCaller<HealthCheckResponse> {
  private final String service;

  private HealthGrpc.HealthBlockingStub stub;

  public HealthCheckStreamCaller(String service) {
    this.service = service;
  }

  @Override
  public void createStub(ManagedChannel channel) throws Exception {
    stub = HealthGrpc.newBlockingStub(channel);
  }

  @Override
  public Iterator<HealthCheckResponse> callService(Duration maxRunTime) throws Exception {
    final HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(service).build();
    return stub.withDeadline(Deadline.after(maxRunTime.toMillis(), TimeUnit.MILLISECONDS))
        .watch(request);
  }

  @Override
  public PreAdjudicatedClaim convertResultToClaim(HealthCheckResponse ignored) throws Exception {
    return new PreAdjudicatedClaim();
  }
}
