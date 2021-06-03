package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class RdaServer {
  public static void main(String[] args) throws Exception {
    String arg = args.length < 1 ? "random" : args[0];
    Supplier<FissClaimSource> sourceFactory = createSourceFactoryForCode(arg);

    System.out.println("Starting server.");
    Server server = startLocal(5003, sourceFactory);
    server.awaitTermination();
    System.out.println("server stopping.");
  }

  public static Supplier<FissClaimSource> createSourceFactoryForCode(String arg)
      throws IOException {
    Supplier<FissClaimSource> sourceFactory;
    if (arg.startsWith("seed:")) {
      sourceFactory = () -> new RandomFissClaimSource(Long.parseLong(arg.substring(5)), 100_000);
    } else if (arg.equals("random")) {
      sourceFactory = () -> new RandomFissClaimSource(System.currentTimeMillis(), 100_000);
    } else if (arg.startsWith("file:")) {
      sourceFactory = () -> new JsonFissClaimSource(new File(arg.substring(5)));
    } else {
      throw new IOException("invalid argument: " + arg);
    }
    return sourceFactory;
  }

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
