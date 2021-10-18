package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.bfd.pipeline.rda.grpc.ThrowableConsumer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RdaServer {
  /**
   * Creates a local RDA API server on a random port for testing.
   *
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @return a running RDA API Server object
   */
  public static Server startLocal(
      MessageSource.Factory<FissClaimChange> fissSourceFactory,
      MessageSource.Factory<McsClaimChange> mcsSourceFactory)
      throws IOException {
    return startLocal(0, fissSourceFactory, mcsSourceFactory);
  }

  /**
   * Creates a local RDA API server on a specific port for testing.
   *
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @return a running RDA API Server object
   */
  public static Server startLocal(
      int port,
      MessageSource.Factory<FissClaimChange> fissSourceFactory,
      MessageSource.Factory<McsClaimChange> mcsSourceFactory)
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
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @return a running RDA API Server object
   */
  public static Server startInProcess(
      String name,
      MessageSource.Factory<FissClaimChange> fissSourceFactory,
      MessageSource.Factory<McsClaimChange> mcsSourceFactory)
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
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithLocalServer(
      MessageSource.Factory<FissClaimChange> fissSourceFactory,
      MessageSource.Factory<McsClaimChange> mcsSourceFactory,
      ThrowableConsumer<Integer> test)
      throws Exception {
    final Server server = startLocal(fissSourceFactory, mcsSourceFactory);
    try {
      test.accept(server.getPort());
    } finally {
      server.shutdown();
      server.awaitTermination(3, TimeUnit.MINUTES);
    }
  }

  /**
   * Starts a server, runs a test with a ManagedChannel to the server as a parameter, and then shuts
   * down the server once the test has finished running. InProcess servers have less overhead than
   * Local servers but still exercise most of the GRPC plumbing.
   *
   * @param fissSourceFactory factory to create a FissClaimChange MessageSource
   * @param mcsSourceFactory factory to create a McsClaimChange MessageSource
   * @param test the test to execute
   * @throws Exception any exception is passed through to the caller
   */
  public static void runWithInProcessServer(
      String serverName,
      MessageSource.Factory<FissClaimChange> fissSourceFactory,
      MessageSource.Factory<McsClaimChange> mcsSourceFactory,
      ThrowableConsumer<ManagedChannel> test)
      throws Exception {
    final Server server = startInProcess(serverName, fissSourceFactory, mcsSourceFactory);
    try {
      final ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
      try {
        test.accept(channel);
      } finally {
        channel.shutdown();
        channel.awaitTermination(3, TimeUnit.MINUTES);
      }
    } finally {
      server.shutdown();
      server.awaitTermination(3, TimeUnit.MINUTES);
    }
  }
}
