package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.function.Supplier;

public class RdaServer {
  public static Server startLocal(Supplier<FissClaimSource> sourceFactory) throws IOException {
    return startLocal(0, sourceFactory);
  }

  public static Server startLocal(int port, Supplier<FissClaimSource> sourceFactory)
      throws IOException {
    return ServerBuilder.forPort(port).addService(new RdaService(sourceFactory)).build().start();
  }

  public static Server startInProcess(String name, Supplier<FissClaimSource> sourceFactory)
      throws IOException {
    return InProcessServerBuilder.forName(name)
        .addService(new RdaService(sourceFactory))
        .build()
        .start();
  }
}
