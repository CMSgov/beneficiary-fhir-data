package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.net.URI;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class AwsServiceConfig {
  @Builder.Default
  private final Optional<Region> region = Optional.of(SharedS3Utilities.REGION_DEFAULT);

  @Builder.Default private final Optional<URI> endpointOverride = Optional.empty();
  @Builder.Default private final Optional<String> accessKey = Optional.empty();
  @Builder.Default private final Optional<String> secretKey = Optional.empty();

  public void configureS3Service(S3ClientBuilder builder) {
    region.ifPresent(builder::region);
    endpointOverride.ifPresent(builder::endpointOverride);
    if (accessKey.isPresent() && secretKey.isPresent()) {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(
              AwsBasicCredentials.create(accessKey.get(), secretKey.get())));
    }
  }

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
}
