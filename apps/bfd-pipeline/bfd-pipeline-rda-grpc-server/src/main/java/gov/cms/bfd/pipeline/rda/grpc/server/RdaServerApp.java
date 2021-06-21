package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

/**
 * A stand-alone mock RDA API (version 0.2 MVP) server implementation. The server is intended for
 * testing purposes only and will not be used in production. Data served is specified on the command
 * line and comes from either random data or from a NDJSON file. The server always starts on port
 * 5003.
 */
public class RdaServerApp {
  public static void main(String[] args) throws Exception {
    String arg = args.length < 1 ? "random" : args[0];
    Supplier<FissClaimSource> sourceFactory = createSourceFactoryForCode(arg);

    System.out.println("Starting server.");
    Server server = RdaServer.startLocal(5003, sourceFactory);
    server.awaitTermination();
    System.out.println("server stopping.");
  }

  /**
   * Creates and returns a factory that can be used to construct a FissClaimSource on demand. The
   * string argument controls what type of data is returned (either random or from a NDJSON file).
   *
   * <ul>
   *   <li>seed:number creates a random source using the number as the PRNG seed value
   *   <li>random creates a random source using current time as the PRNG seed
   *   <li>file:filename creates a source that returns objects contained in an NDJSON file
   * </ul>
   *
   * @param arg random, seed:number, or file:filename
   * @return a Suppler that creates a new FissClaimSource object each time it's called
   * @throws IOException if the arg value is unrecognized
   */
  private static Supplier<FissClaimSource> createSourceFactoryForCode(String arg)
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
