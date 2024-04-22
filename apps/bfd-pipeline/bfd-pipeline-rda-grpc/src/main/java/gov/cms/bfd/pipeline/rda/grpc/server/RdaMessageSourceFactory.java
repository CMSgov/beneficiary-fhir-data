package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.rda.grpc.server.RdaService.RDA_PROTO_VERSION;
import static java.lang.String.format;

import com.google.common.base.Strings;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import jakarta.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Interface for objects that can provide information required by {@link RdaService} to generate
 * responses to clients.
 */
public interface RdaMessageSourceFactory extends AutoCloseable {
  /**
   * Called by {@link RdaService#getVersion} to get the version for client.
   *
   * @return appropriate version
   */
  RdaService.Version getVersion();

  /**
   * Called by {@link RdaService#getFissClaims} to get a source of FISS claims to send to client.
   *
   * @param startingSequenceNumber first sequence number to send to the client
   * @return appropriate claim source
   * @throws Exception pass through any thrown while creating the claim source
   */
  MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber)
      throws Exception;

  /**
   * Called by {@link RdaService#getMcsClaims} to get a source of MCS claims to send to client.
   *
   * @param startingSequenceNumber first sequence number to send to the client
   * @return appropriate claim source
   * @throws Exception pass through any thrown while creating the claim source
   */
  MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber)
      throws Exception;

  /**
   * Object that can produce a particular instance of {@link RdaMessageSourceFactory} on demand
   * based on a flexible set of possible sources of data.
   */
  @Builder
  @Slf4j
  class Config {
    /** The {@link RandomClaimGeneratorConfig} to use for random claim generation. */
    @Builder.Default
    private final RandomClaimGeneratorConfig randomClaimConfig =
        RandomClaimGeneratorConfig.builder().build();

    /** Used to create {@link S3ClientFactory} when necessary. */
    @Builder.Default
    private final S3ClientConfig s3ClientConfig = S3ClientConfig.s3Builder().build();

    /** NDJSON fiss claim data for the RDA Server. */
    @Nullable private final CharSource fissClaimJson;

    /** NDJSON mcs claim data for the RDI Server. */
    @Nullable private final CharSource mcsClaimJson;

    /** Name of our S3 bucket. */
    @Nullable private final String s3Bucket;

    /** Optional directory name within our S3 bucket. */
    @Nullable private final String s3Directory;

    /** Name of a local directory in which to store cached files from S3. */
    @Nullable private final String s3CacheDirectory;

    /** Optional hard coded version. */
    @Nullable private final RdaService.Version version;

    /**
     * If positive this causes all generated {@link MessageSource}s to be wrapped in {@link
     * ExceptionMessageSource} with {@see ExceptionMessageSource#countBeforeThrow} set to this
     * value.
     */
    int throwExceptionAfterCount;

    /**
     * Creates an instance based on which set of configuration values have been provided when
     * building this config. Possible instances are (in priority and based on which options were
     * provided): {@link RdaJsonMessageSourceFactory} using provided NDJSON data, {@link
     * RdaS3JsonMessageSourceFactory} using an S3 bucket, or {@link RdaRandomMessageSourceFactory}
     * if no other options applied. Optionally (if {@link #throwExceptionAfterCount} is positive)
     * wraps factory in a {@link RdaExceptionMessageSourceFactory}.
     *
     * @return the instance
     * @throws Exception pass through any exceptions
     */
    public RdaMessageSourceFactory createMessageSourceFactory() throws Exception {
      RdaMessageSourceFactory factory;
      if (fissClaimJson != null || mcsClaimJson != null) {
        factory = createJsonMessageSourceFactory();
      } else if (s3Bucket != null) {
        factory = createS3MessageSourceFactory();
      } else {
        factory = createRandomMessageSourceFactory();
      }
      if (throwExceptionAfterCount > 0) {
        factory = new RdaExceptionMessageSourceFactory(factory, throwExceptionAfterCount);
      }
      return factory;
    }

    /**
     * Creates {@link RdaJsonMessageSourceFactory} using provided NDJSON data sources.
     *
     * @return the instance
     */
    private RdaMessageSourceFactory createJsonMessageSourceFactory() {
      final RdaService.Version version =
          this.version != null ? this.version : RdaService.Version.builder().build();
      CharSource fissJson = fissClaimJson != null ? fissClaimJson : CharSource.empty();
      CharSource mcsJson = mcsClaimJson != null ? mcsClaimJson : CharSource.empty();
      log.info(
          "serving claims using {} with data from files",
          RdaJsonMessageSourceFactory.class.getSimpleName());
      return new RdaJsonMessageSourceFactory(version, fissJson, mcsJson);
    }

    /**
     * Creates {@link RdaS3JsonMessageSourceFactory} using provided S3 bucket information.
     *
     * @return the instance
     */
    private RdaMessageSourceFactory createS3MessageSourceFactory() throws Exception {
      final RdaService.Version version =
          this.version != null
              ? this.version
              : RdaService.Version.builder()
                  .version(format("S3:%d:%s", System.currentTimeMillis(), RDA_PROTO_VERSION))
                  .build();
      final String directory = s3Directory == null ? "" : s3Directory;
      final boolean useTempDirectoryForCache = Strings.isNullOrEmpty(s3CacheDirectory);
      final Path cacheDirectory =
          useTempDirectoryForCache
              ? java.nio.file.Files.createTempDirectory("s3cache")
              : Path.of(s3CacheDirectory);
      final S3ClientFactory s3ClientFactory = new AwsS3ClientFactory(s3ClientConfig);
      final S3DirectoryDao s3Dao =
          new S3DirectoryDao(
              s3ClientFactory.createS3Dao(),
              s3Bucket,
              directory,
              cacheDirectory,
              useTempDirectoryForCache,
              false);
      log.info(
          "serving claims using {} with data from S3 bucket {}",
          RdaS3JsonMessageSourceFactory.class.getSimpleName(),
          s3Dao.getS3BucketName());
      return new RdaS3JsonMessageSourceFactory(version, s3Dao);
    }

    /**
     * Creates {@link RdaRandomMessageSourceFactory}. Called when no other option applied.
     *
     * @return the instance
     */
    private RdaMessageSourceFactory createRandomMessageSourceFactory() {
      final RdaService.Version version =
          this.version != null
              ? this.version
              : RdaService.Version.builder()
                  .version(format("Random:%d:%s", randomClaimConfig.getSeed(), RDA_PROTO_VERSION))
                  .build();
      log.info(
          "serving no more than {} claims using {} with seed {}",
          randomClaimConfig.getMaxToSend(),
          RdaRandomMessageSourceFactory.class.getSimpleName(),
          randomClaimConfig.getSeed());
      return new RdaRandomMessageSourceFactory(version, randomClaimConfig);
    }

    /** This static class allows us to add methods to the builder generated by lombok. */
    // Lombok uses this class for the builder but IDEA doesn't seem to realize that.
    @SuppressWarnings("unused")
    public static class ConfigBuilder {
      /**
       * Optionally add a {@link CharSource} constructed by combining the provided JSON strings as a
       * source of FISS claim data.
       *
       * @param jsonChanges JSON for {@link FissClaimChange} objects
       * @return this builder
       */
      public ConfigBuilder fissClaimJsonList(List<String> jsonChanges) {
        return fissClaimJson(CharSource.wrap(String.join("\n", jsonChanges)));
      }

      /**
       * Optionally add a {@link CharSource} constructed by combining the provided JSON strings as a
       * source of MCS claim data.
       *
       * @param jsonChanges JSON for {@link McsClaimChange} objects
       * @return this builder
       */
      public ConfigBuilder mcsClaimJsonList(List<String> jsonChanges) {
        return mcsClaimJson(CharSource.wrap(String.join("\n", jsonChanges)));
      }

      /**
       * Optionally add a UTF-8 encoded {@link File} as a source of FISS claim data. The argument
       * can be null so that this can be called when a file may or may not be available.
       *
       * @param ndjsonFile null or a valid {@link File} containing ndjson data
       * @return this builder
       */
      public ConfigBuilder fissClaimJsonFile(@Nullable File ndjsonFile) {
        if (ndjsonFile != null) {
          fissClaimJson(Files.asCharSource(ndjsonFile, StandardCharsets.UTF_8));
        }
        return this;
      }

      /**
       * Optionally add a UTF-8 encoded {@link File} as a source of MCS claim data. The argument can
       * be null so that this can be called when a file may or may not be available.
       *
       * @param ndjsonFile null or a valid {@link File} containing ndjson data
       * @return this builder
       */
      public ConfigBuilder mcsClaimJsonFile(@Nullable File ndjsonFile) {
        if (ndjsonFile != null) {
          mcsClaimJson(Files.asCharSource(ndjsonFile, StandardCharsets.UTF_8));
        }
        return this;
      }
    }
  }
}
