package gov.cms.bfd.pipeline.rda.grpc.server;

import io.grpc.Server;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A stand-alone mock RDA API (version 0.2 MVP) server implementation. The server is intended for
 * testing purposes only and will not be used in production. Data served is specified on the command
 * line and comes from either random data or from a NDJSON file. The server always starts on port
 * 5003.
 */
public class RdaServerApp {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaServerApp.class);
  private static final int MAX_TO_SEND = 5_000;

  public static void main(String[] args) throws Exception {
    String arg = args.length < 1 ? "random" : args[0];
    Supplier<FissClaimSource> sourceFactory = createSourceFactoryForCode(arg);

    LOGGER.info("Starting server.");
    Server server = RdaServer.startLocal(5003, sourceFactory);
    server.awaitTermination();
    LOGGER.info("server stopping.");
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
      final long seed = Long.parseLong(arg.substring(5));
      sourceFactory =
          () -> {
            LOGGER.info("serving data using RandomFissClaimSource with seed {}", seed);
            return new RandomFissClaimSource(seed, MAX_TO_SEND);
          };
    } else if (arg.equals("random")) {
      final long seed = System.currentTimeMillis();
      sourceFactory =
          () -> {
            LOGGER.info("serving data using RandomFissClaimSource with seed {}", seed);
            return new RandomFissClaimSource(seed, MAX_TO_SEND);
          };
    } else if (arg.startsWith("file:")) {
      final File file = new File(arg.substring(5));
      sourceFactory =
          () -> {
            LOGGER.info(
                "serving data using JsonFissClaimSource with data from file {}",
                file.getAbsolutePath());
            return new JsonFissClaimSource(file);
          };
    } else {
      throw new IOException("invalid argument: " + arg);
    }
    return sourceFactory;
  }
}
