package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.function.Supplier;

public class RdaServer {
  /**
   * Creates a local RDA API server on a random port for testing.
   *
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startLocal(Supplier<FissClaimSource> sourceFactory) throws IOException {
    return startLocal(0, sourceFactory);
  }

  /**
   * Creates a local RDA API server on a specific port for testing.
   *
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startLocal(int port, Supplier<FissClaimSource> sourceFactory)
      throws IOException {
    return ServerBuilder.forPort(port).addService(new RdaService(sourceFactory)).build().start();
  }

  /**
   * Creates an in-process (no network connections involved) RDA API server for testing.
   *
   * @param name name used InProcessChannelBuilders to connect to the server
   * @param sourceFactory used to create new FissClaimSource object for each request
   * @return a running RDA API Server object
   */
  public static Server startInProcess(String name, Supplier<FissClaimSource> sourceFactory)
      throws IOException {
    return InProcessServerBuilder.forName(name)
        .addService(new RdaService(sourceFactory))
        .build()
        .start();
  }
}
