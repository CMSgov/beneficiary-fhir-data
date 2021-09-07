package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.regions.Regions;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.Server;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
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

  /**
   * Starts a RDA API server listening on localhost at a specific port. Configuration is controlled
   * by command line arguments. Each argument specifies one setting. Valid arguments are:
   *
   * <ul>
   *   <li>maxToSend:number sets the maximum number of objects to send in random streams
   *   <li>port:number sets the port for the server to listen on (default is 5003)
   *   <li>seed:number creates a random source using the number as the PRNG seed value
   *   <li>random creates a random source using current time as the PRNG seed
   *   <li>fissFile:filename creates a source that returns FissClaims contained in an NDJSON file
   *   <li>mcsFile:filename creates a source that returns McsClaims contained in an NDJSON file
   * </ul>
   */
  public static void main(String[] args) throws Exception {
    final Config config = new Config(args);
    LOGGER.info("Starting server on port {}.", config.getPort());
    Server server =
        RdaServer.startLocal(config.getPort(), config::createFissClaims, config::createMcsClaims);
    server.awaitTermination();
    LOGGER.info("server stopping.");
  }

  private static class Config {
    private final int port;
    private final long seed;
    private final int maxToSend;
    @Nullable private final File fissClaimFile;
    @Nullable private final File mcsClaimFile;
    @Nullable private final S3JsonMessageSources s3Sources;
    @Nullable private final String fissS3ObjectKey;
    @Nullable private final String mcsS3ObjectKey;

    private Config(String[] args) throws Exception {
      int port = 5003;
      long seed = System.currentTimeMillis();
      int maxToSend = 5_000;
      Regions s3Region = SharedS3Utilities.REGION_DEFAULT;
      S3JsonMessageSources s3Sources = null;
      File fissClaimFile = null;
      File mcsClaimFile = null;
      String fissS3ObjectKey = null;
      String mcsS3ObjectKey = null;
      for (String arg : args) {
        if (arg.startsWith("port:")) {
          port = Integer.parseInt(argValue(arg));
        } else if (arg.startsWith("maxToSend:")) {
          maxToSend = Integer.parseInt(argValue(arg));
        } else if (arg.startsWith("seed:") || arg.startsWith("random:")) {
          seed = Long.parseLong(argValue(arg));
        } else if (arg.startsWith("fissFile:")) {
          fissClaimFile = new File(argValue(arg));
        } else if (arg.startsWith("mcsFile:")) {
          mcsClaimFile = new File(argValue(arg));
        } else if (arg.startsWith("s3Region:")) {
          s3Region = Regions.fromName(argValue(arg));
          if (s3Sources != null) {
            throw new IOException("s3Region must be defined before s3Bucket");
          }
        } else if (arg.startsWith("s3Bucket:")) {
          s3Sources =
              new S3JsonMessageSources(SharedS3Utilities.createS3Client(s3Region), argValue(arg));
        } else if (arg.startsWith("fissS3Key:")) {
          fissS3ObjectKey = argValue(arg);
        } else if (arg.startsWith("mcsS3Key:")) {
          mcsS3ObjectKey = argValue(arg);
        } else {
          throw new IOException("invalid argument: " + arg);
        }
      }
      if (s3Sources == null && (fissS3ObjectKey != null || mcsS3ObjectKey != null)) {
        throw new IOException("either fissS3Key or mcsS3Key must be specified when using S3");
      }
      this.port = port;
      this.seed = seed;
      this.maxToSend = maxToSend;
      this.fissClaimFile = fissClaimFile;
      this.mcsClaimFile = mcsClaimFile;
      this.s3Sources = s3Sources;
      this.fissS3ObjectKey = fissS3ObjectKey;
      this.mcsS3ObjectKey = mcsS3ObjectKey;
    }

    private int getPort() {
      return port;
    }

    private MessageSource<FissClaimChange> createFissClaims(long sequenceNumber) throws Exception {
      if (fissClaimFile != null) {
        LOGGER.info(
            "serving FissClaims using JsonClaimSource with data from file {}",
            fissClaimFile.getAbsolutePath());
        return new JsonMessageSource<>(fissClaimFile, JsonMessageSource::parseFissClaimChange);
      } else if (fissS3ObjectKey != null && s3Sources != null) {
        LOGGER.info(
            "serving FissClaims using JsonClaimSource with data from S3 Key {}", fissS3ObjectKey);
        return s3Sources.readFissClaimChanges(fissS3ObjectKey);
      } else {
        LOGGER.info(
            "serving no more than {} FissClaims using RandomFissClaimSource with seed {}",
            maxToSend,
            seed);
        return new RandomFissClaimSource(seed, maxToSend).toClaimChanges().skip(sequenceNumber);
      }
    }

    private MessageSource<McsClaimChange> createMcsClaims(long sequenceNumber) throws Exception {
      if (mcsClaimFile != null) {
        LOGGER.info(
            "serving McsClaims using JsonClaimSource with data from file {}",
            mcsClaimFile.getAbsolutePath());
        return new JsonMessageSource<>(mcsClaimFile, JsonMessageSource::parseMcsClaimChange);
      } else if (mcsS3ObjectKey != null && s3Sources != null) {
        LOGGER.info(
            "serving McsClaims using JsonClaimSource with data from S3 Key {}", mcsS3ObjectKey);
        return s3Sources.readMcsClaimChanges(mcsS3ObjectKey);
      } else {
        LOGGER.info(
            "serving no more than {} McsClaims using RandomMcsClaimSource with seed {}",
            maxToSend,
            seed);
        return new RandomMcsClaimSource(seed, maxToSend).toClaimChanges().skip(sequenceNumber);
      }
    }

    /**
     * Removes the prefix from a command line argument.
     *
     * @param arg command line argument of the form "prefix:value"
     * @return the value portion of the value
     */
    private String argValue(String arg) {
      final int prefixEnd = arg.indexOf(":");
      if (prefixEnd < 0) {
        // the caller should have ensured we had a : in the argument
        throw new BadCodeMonkeyException();
      }
      return arg.substring(prefixEnd + 1);
    }
  }
}
