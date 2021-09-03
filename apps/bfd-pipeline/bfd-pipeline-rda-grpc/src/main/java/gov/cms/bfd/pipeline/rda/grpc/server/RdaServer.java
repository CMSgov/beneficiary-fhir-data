package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.bfd.pipeline.rda.grpc.ThrowableConsumer;
import gov.cms.mpsm.rda.v1.ClaimChange;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RdaServer {
  /**
   * Creates a local RDA API server on a random port for testing.
   *
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startLocal(
      Supplier<MessageSource<ClaimChange>> fissSourceFactory,
      Supplier<MessageSource<ClaimChange>> mcsSourceFactory)
      throws IOException {
    return startLocal(0, fissSourceFactory, mcsSourceFactory);
  }

  /**
   * Creates a local RDA API server on a specific port for testing.
   *
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startLocal(
      int port,
      Supplier<MessageSource<ClaimChange>> fissSourceFactory,
      Supplier<MessageSource<ClaimChange>> mcsSourceFactory)
      throws IOException {
    return ServerBuilder.forPort(port)
        .addService(new RdaService(fissSourceFactory, mcsSourceFactory))
        .build()
        .start();
  }

  /**
   * Creates an in-process (no network connections involved) RDA API server for testing.
   *
   * @param name name used InProcessChannelBuilders to connect to the server
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startInProcess(
      String name,
      Supplier<MessageSource<ClaimChange>> fissSourceFactory,
      Supplier<MessageSource<ClaimChange>> mcsSourceFactory)
      throws IOException {
    return InProcessServerBuilder.forName(name)
        .addService(new RdaService(fissSourceFactory, mcsSourceFactory))
        .build()
        .start();
  }

  /**
   * Starts a server, runs a test with the server's port as a parameter, and then shuts down the
   * server once the test has finished running.
   *
   * @param fissClaimJson the FISS claims in JSON format, one per line
   * @param mcsClaimJson the MCS claims in JSON format, one per line
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithLocalServer(
      Supplier<MessageSource<ClaimChange>> fissClaims,
      Supplier<MessageSource<ClaimChange>> mcsClaims,
      ThrowableConsumer<Integer> test)
      throws Exception {
    final Server server = startLocal(fissClaims, mcsClaims);
    try {
      test.accept(server.getPort());
    } finally {
      server.shutdown();
      server.awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
