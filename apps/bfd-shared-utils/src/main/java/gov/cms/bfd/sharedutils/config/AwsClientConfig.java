package gov.cms.bfd.sharedutils.config;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

/**
 * Value object containing some common settings required to initialize AWS SDK clients. All settings
 * are optional. Normally construction is via a builder object. The builder class and object have
 * custom names to differentiate from the builder in derived classes.
 */
@Getter
@EqualsAndHashCode
public final class AwsClientConfig {
  /** The default AWS {@link Region} to interact with. */
  public static final Region REGION_DEFAULT = Region.US_EAST_1;

  /** The AWS region to connect to. Defaults to {@link #REGION_DEFAULT}. */
  private final Optional<Region> region;

  /** Alternative endpoint URI for service. Generally only used for testing with localstack. */
  private final Optional<URI> endpointOverride;

  /**
   * Access key for authenticating with service. Generally only used for testing with localstack.
   */
  private final Optional<String> accessKey;

  /**
   * Secret key for authenticating with service. Generally only used for testing with localstack.
   */
  private final Optional<String> secretKey;

  /**
   * Initializes an instance. Any variable can be null. Region defaults to {@link #REGION_DEFAULT}.
   *
   * @param region an AWS {@link Region}
   * @param endpointOverride alternative URI for accessing AWS services (used with localstack)
   * @param accessKey optional access key
   * @param secretKey optional secret key
   */
  @Builder(builderClassName = "AwsBuilder", builderMethodName = "awsBuilder")
  public AwsClientConfig(
      @Nullable Region region,
      @Nullable URI endpointOverride,
      @Nullable String accessKey,
      @Nullable String secretKey) {
    this.region = region == null ? Optional.of(REGION_DEFAULT) : Optional.of(region);
    this.endpointOverride = Optional.ofNullable(endpointOverride);
    this.accessKey = Optional.ofNullable(accessKey);
    this.secretKey = Optional.ofNullable(secretKey);
  }

  /**
   * Updates a {@link AwsClientBuilder} with settings from this config.
   *
   * @param builder the builder to update
   */
  public void configureAwsService(AwsClientBuilder<?, ?> builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (endpointOverride.isPresent()) {
      // AWS services can be slow under colima in some environments.
      // This makes the client more tolerant of delays.
      builder.overrideConfiguration(
          configBuilder ->
              configBuilder
                  .retryPolicy(b -> b.numRetries(10))
                  .apiCallAttemptTimeout(Duration.ofSeconds(10)));
    }
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
    return "AwsClientConfig{"
        + "region="
        + region
        + ", endpointOverride="
        + endpointOverride
        + ", accessKeyLength="
        + accessKey.map(String::length)
        + ", secretKeyLength="
        + secretKey.map(String::length)
        + '}';
  }
}
