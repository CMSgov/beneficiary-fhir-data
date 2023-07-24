package gov.cms.bfd.pipeline.sharedutils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.net.URI;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * Value object containing some common settings required to initialize AWS SDK clients. All settings
 * are optional. Normally construction is via a builder object.
 */
@Builder
@Data
@AllArgsConstructor
public class AwsClientConfig {
  /** The AWS region to connect to. Defaults to {@link SharedS3Utilities#REGION_DEFAULT}. */
  @Builder.Default
  private final Optional<Region> region = Optional.of(SharedS3Utilities.REGION_DEFAULT);

  /** Alternative endpoint URI for service. Generally only used for testing with localstack. */
  @Builder.Default private final Optional<URI> endpointOverride = Optional.empty();

  /**
   * Access key for authenticating with service. Generally only used for testing with localstack.
   */
  @Builder.Default private final Optional<String> accessKey = Optional.empty();

  /**
   * Secret key for authenticating with service. Generally only used for testing with localstack.
   */
  @Builder.Default private final Optional<String> secretKey = Optional.empty();

  /**
   * Updates a {@link S3ClientBuilder} with settings from this config.
   *
   * @param builder the builder to update
   */
  public void configureS3Service(S3ClientBuilder builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (accessKey.isPresent() && secretKey.isPresent()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
    }
  }

  /**
   * Updates a {@link S3CrtAsyncClientBuilder} with settings from this config.
   *
   * @param builder the builder to update
   */
  @CanIgnoreReturnValue
  public void configureS3Service(S3CrtAsyncClientBuilder builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (accessKey.isPresent() && secretKey.isPresent()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
    }
  }

  /**
   * Determines if AWS credentials will be needed to communicate with AWS. Only true if we are using
   * real AWS endpoint and we have no access key defined.
   *
   * @return true if credential check will be useful given our configuration
   */
  public boolean isCredentialCheckUseful() {
    return endpointOverride.isEmpty() && accessKey.isEmpty();
  }
}
