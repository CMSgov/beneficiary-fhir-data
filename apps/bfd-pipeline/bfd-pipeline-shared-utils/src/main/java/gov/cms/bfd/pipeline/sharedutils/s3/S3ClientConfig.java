package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.net.URI;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * Value object containing some common settings required to initialize AWS S3 clients. All settings
 * are optional. Normally construction is via a builder object. The builder class and object have
 * custom names to differentiate from the builder in the parent class. This class is necessary
 * because {@link S3CrtAsyncClientBuilder} does not implement the same interface as most other S3
 * clients.
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class S3ClientConfig extends AwsClientConfig {
  /**
   * Passed to {@link S3CrtAsyncClientBuilder#minimumPartSizeInBytes} to cause large files to be
   * downloaded in parts of 200 mb. Default value used when no alternative value has been provided.
   */
  static final long DEFAULT_MINIMUM_PART_SIZE_FOR_DOWNLOAD = 8 * 1024L * 1024L;

  /**
   * Passed to {@link S3CrtAsyncClientBuilder#minimumPartSizeInBytes} to cause large files to be
   * downloaded in parts.
   */
  private final long minimumPartSizeForDownload;

  /**
   * Initializes an instance. Any variable can be null. Region defaults to {@link #REGION_DEFAULT}.
   *
   * @param region an AWS {@link Region}
   * @param endpointOverride alternative URI for accessing AWS services (used with localstack)
   * @param accessKey optional access key
   * @param secretKey optional secret key
   * @param minimumPartSizeForDownload optional minimum part size
   */
  @Builder(builderClassName = "S3Builder", builderMethodName = "s3Builder")
  private S3ClientConfig(
      @Nullable Region region,
      @Nullable URI endpointOverride,
      @Nullable String accessKey,
      @Nullable String secretKey,
      @Nullable Long minimumPartSizeForDownload) {
    super(region, endpointOverride, accessKey, secretKey);
    this.minimumPartSizeForDownload =
        minimumPartSizeForDownload != null
            ? minimumPartSizeForDownload
            : DEFAULT_MINIMUM_PART_SIZE_FOR_DOWNLOAD;
  }

  /**
   * Updates a {@link S3CrtAsyncClientBuilder} with settings from this config.
   *
   * @param builder the builder to update
   */
  @CanIgnoreReturnValue
  public void configureS3ServiceForAsyncS3(S3CrtAsyncClientBuilder builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (accessKey.isPresent() && secretKey.isPresent()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
    }
    builder.minimumPartSizeInBytes(minimumPartSizeForDownload);
  }

  /**
   * This implementation only provides length of access and secret keys to prevent leaking sensitive
   * info to logs.
   *
   * <p>{@inheritDoc}
   *
   * @return string representation of object
   */
  @Override
  public String toString() {
    return "S3ClientConfig{"
        + "region="
        + region
        + ", endpointOverride="
        + endpointOverride
        + ", accessKeyLength="
        + accessKey.map(String::length)
        + ", secretKeyLength="
        + secretKey.map(String::length)
        + ", minimumPartSizeForDownload="
        + minimumPartSizeForDownload
        + '}';
  }
}
