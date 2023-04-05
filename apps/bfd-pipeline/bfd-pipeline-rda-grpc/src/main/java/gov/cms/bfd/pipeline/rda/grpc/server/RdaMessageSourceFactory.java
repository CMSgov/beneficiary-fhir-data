package gov.cms.bfd.pipeline.rda.grpc.server;

import static gov.cms.bfd.pipeline.rda.grpc.server.RdaService.RDA_PROTO_VERSION;
import static java.lang.String.format;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.Serializable;
import java.nio.file.Path;
import javax.annotation.Nullable;
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
  class Config implements Serializable {
    /** The {@link RandomClaimGeneratorConfig} to use for random claim generation. */
    @Builder.Default
    private final RandomClaimGeneratorConfig randomClaimConfig =
        RandomClaimGeneratorConfig.builder().build();
    /** The maximum number of claims to be returned when operating in {@code Random} mode. */
    private final int randomMaxClaims;
    /** The fiss claim data for the RDA Server. */
    @Nullable private final ByteSource fissClaimJson;
    /** The mcs claim data for the RDI Server. */
    @Nullable private final ByteSource mcsClaimJson;
    /** AWS region containing our S3 bucket. */
    @Nullable private final Regions s3Region;
    /** Name of our S3 bucket. */
    @Nullable private final String s3Bucket;
    /** Optional directory name within our S3 bucket. */
    @Nullable private final String s3Directory;
    /** Name of a directory in which to store cached files from S3. */
    @Nullable private final String s3CacheDirectory;
    /** Optional hard coded version. */
    @Nullable private final RdaService.Version version;
    /** Optional hard coded factory for creating FISS claim message sources. */
    @Nullable private final MessageSource.Factory<FissClaimChange> fissSourceFactory;
    /** Optional hard coded factory for creating MCS claim message sources. */
    @Nullable private final MessageSource.Factory<McsClaimChange> mcsSourceFactory;

    /**
     * Creates an instance based on which set of configuration values have been provided when
     * building this config. Possible instances are (in priority and based on which options were
     * provided): {@link RdaBasicMessageSourceFactory} using hard coded values, {@link
     * RdaJsonMessageSourceFactory} using provided NDJSON data, {@link
     * RdaS3JsonMessageSourceFactory} using an S3 bucket, or {@link RdaRandomMessageSourceFactory}
     * if no other options applied.
     *
     * @return the instance
     * @throws Exception pass through any exceptions
     */
    public RdaMessageSourceFactory createMessageSourceFactory() throws Exception {
      if (fissSourceFactory != null || mcsSourceFactory != null) {
        return createBasicMessageSourceFactory();
      } else if (fissClaimJson != null || mcsClaimJson != null) {
        return createJsonMessageSourceFactory();
      } else if (s3Bucket != null) {
        return createS3MessageSourceFactory();
      } else {
        return createRandomMessageSourceFactory();
      }
    }

    /**
     * Creates {@link RdaBasicMessageSourceFactory} using hard coded version and/or factories.
     *
     * @return the instance
     */
    private RdaMessageSourceFactory createBasicMessageSourceFactory() {
      final RdaService.Version version =
          this.version != null ? this.version : RdaService.Version.builder().build();
      final MessageSource.Factory<FissClaimChange> fissFactory =
          this.fissSourceFactory != null ? this.fissSourceFactory : EmptyMessageSource.factory();
      final MessageSource.Factory<McsClaimChange> mcsFactory =
          this.mcsSourceFactory != null ? this.mcsSourceFactory : EmptyMessageSource.factory();
      return new RdaBasicMessageSourceFactory(version, fissFactory, mcsFactory);
    }

    /**
     * Creates {@link RdaJsonMessageSourceFactory} using provided NDJSON data sources.
     *
     * @return the instance
     */
    private RdaMessageSourceFactory createJsonMessageSourceFactory() {
      final RdaService.Version version =
          this.version != null ? this.version : RdaService.Version.builder().build();
      ByteSource fissJson = fissClaimJson != null ? fissClaimJson : ByteSource.empty();
      ByteSource mcsJson = mcsClaimJson != null ? mcsClaimJson : ByteSource.empty();
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
      final Regions region = s3Region == null ? SharedS3Utilities.REGION_DEFAULT : s3Region;
      final AmazonS3 s3Client = SharedS3Utilities.createS3Client(region);
      final String directory = s3Directory == null ? "" : s3Directory;
      final Path cacheDirectory =
          Strings.isNullOrEmpty(s3CacheDirectory)
              ? java.nio.file.Files.createTempDirectory("s3cache")
              : Path.of(s3CacheDirectory);
      final S3DirectoryDao s3Dao =
          new S3DirectoryDao(s3Client, s3Bucket, directory, cacheDirectory);
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
          randomMaxClaims,
          RdaRandomMessageSourceFactory.class.getSimpleName(),
          randomClaimConfig.getSeed());
      return new RdaRandomMessageSourceFactory(version, randomClaimConfig, randomMaxClaims);
    }
  }
}
