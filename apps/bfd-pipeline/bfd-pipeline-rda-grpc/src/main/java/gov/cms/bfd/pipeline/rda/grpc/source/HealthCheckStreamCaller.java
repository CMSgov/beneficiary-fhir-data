package gov.cms.bfd.pipeline.rda.grpc.source;

import com.nava.health.v1.HealthCheckRequest;
import com.nava.health.v1.HealthCheckResponse;
import com.nava.health.v1.HealthGrpc;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import io.grpc.ManagedChannel;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/** GrpcStreamCaller implementation that calls the RDA HealthCheck service. */
public class HealthCheckStreamCaller implements GrpcStreamCaller<HealthCheckResponse> {
  private final String service;
  private final HealthGrpc.HealthBlockingStub stub;

  public HealthCheckStreamCaller(String service, ManagedChannel channel) {
    this.service = service;
    stub = HealthGrpc.newBlockingStub(channel);
  }

  @Override
  public Iterator<HealthCheckResponse> callService(Duration maxRunTime) throws Exception {
    final HealthCheckRequest request = HealthCheckRequest.newBuilder().setService(service).build();
    return stub.withDeadlineAfter(maxRunTime.toMillis(), TimeUnit.MILLISECONDS).watch(request);
  }

  @Override
  public PreAdjudicatedClaim convertResultToClaim(HealthCheckResponse ignored) throws Exception {
    return new PreAdjudicatedClaim();
  }

  public static GrpcStreamCaller.Factory<HealthCheckResponse> createFactory(String service) {
    return channel -> new HealthCheckStreamCaller(service, channel);
  }
}
