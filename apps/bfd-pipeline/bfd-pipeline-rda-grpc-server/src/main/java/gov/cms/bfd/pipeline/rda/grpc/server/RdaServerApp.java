package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

public class RdaServerApp {
  public static void main(String[] args) throws Exception {
    String arg = args.length < 1 ? "random" : args[0];
    Supplier<FissClaimSource> sourceFactory = createSourceFactoryForCode(arg);

    System.out.println("Starting server.");
    Server server = RdaServer.startLocal(5003, sourceFactory);
    server.awaitTermination();
    System.out.println("server stopping.");
  }

  public static Supplier<FissClaimSource> createSourceFactoryForCode(String arg)
      throws IOException {
    Supplier<FissClaimSource> sourceFactory;
    if (arg.startsWith("seed:")) {
      sourceFactory = () -> new RandomFissClaimSource(Long.parseLong(arg.substring(5)), 10_000);
    } else if (arg.equals("random")) {
      sourceFactory = () -> new RandomFissClaimSource(System.currentTimeMillis(), 10_000);
    } else if (arg.startsWith("file:")) {
      sourceFactory = () -> new JsonFissClaimSource(new File(arg.substring(5)));
    } else {
      throw new IOException("invalid argument: " + arg);
    }
    return sourceFactory;
  }
}
