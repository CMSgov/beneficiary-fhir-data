package gov.cms.bfd.pipeline.sharedutils.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.net.URI;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

/** Simple tests to ensure {@link S3ClientConfig} builder applies values as expected. */
class S3ClientConfigTest {
  /** Ensure default values are used when no overrides are provided to the builder. */
  @Test
  void testDefaultValues() {
    S3ClientConfig config = S3ClientConfig.s3Builder().build();
    assertEquals(AwsClientConfig.awsBuilder().build(), config.getAwsClientConfig());
    assertEquals(
        S3ClientConfig.DEFAULT_MINIMUM_PART_SIZE_FOR_DOWNLOAD,
        config.getMinimumPartSizeForDownload());
  }

  /** Ensure override values are used when they are provided to the builder. */
  @Test
  void testOverrideValues() {
    final URI endpointOverride = URI.create("https://localhost:4556");
    final var awsClientConfig =
        AwsClientConfig.awsBuilder()
            .region(Region.US_WEST_1)
            .endpointOverride(endpointOverride)
            .accessKey("access")
            .secretKey("secret")
            .build();

    S3ClientConfig config =
        S3ClientConfig.s3Builder()
            .region(Region.US_WEST_1)
            .endpointOverride(endpointOverride)
            .accessKey("access")
            .secretKey("secret")
            .minimumPartSizeForDownload(1000L)
            .build();
    assertEquals(awsClientConfig, config.getAwsClientConfig());
    assertEquals(1000L, config.getMinimumPartSizeForDownload());
  }

  /** Ensure {@link AwsClientConfig} is used when provided to the builder. */
  @Test
  void testSpecificAwsClientConfig() {
    final URI endpointOverride = URI.create("https://localhost:4556");
    final var awsClientConfig =
        AwsClientConfig.awsBuilder()
            .region(Region.US_WEST_1)
            .endpointOverride(endpointOverride)
            .accessKey("access")
            .secretKey("secret")
            .build();

    S3ClientConfig config = S3ClientConfig.s3Builder().awsClientConfig(awsClientConfig).build();
    assertSame(awsClientConfig, config.getAwsClientConfig());
    assertEquals(
        S3ClientConfig.DEFAULT_MINIMUM_PART_SIZE_FOR_DOWNLOAD,
        config.getMinimumPartSizeForDownload());
  }
}
