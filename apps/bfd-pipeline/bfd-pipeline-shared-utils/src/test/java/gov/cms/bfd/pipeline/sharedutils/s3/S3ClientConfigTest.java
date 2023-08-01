package gov.cms.bfd.pipeline.sharedutils.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

/** Simple tests to ensure {@link S3ClientConfig} builder applies values as expected. */
class S3ClientConfigTest {
  /** Ensure default values are used when no overrides are provided to the builder. */
  @Test
  void testDefaultValues() {
    S3ClientConfig config = S3ClientConfig.s3Builder().build();
    assertEquals(Optional.of(AwsClientConfig.REGION_DEFAULT), config.getRegion());
    assertEquals(Optional.empty(), config.getEndpointOverride());
    assertEquals(Optional.empty(), config.getAccessKey());
    assertEquals(Optional.empty(), config.getSecretKey());
    assertEquals(
        S3ClientConfig.DEFAULT_MINIMUM_PART_SIZE_FOR_DOWNLOAD,
        config.getMinimumPartSizeForDownload());
  }

  /** Ensure override values are used when they are provided to the builder. */
  @Test
  void testOverrideValues() {
    final URI endpointOverride = URI.create("https://localhost:4556");
    S3ClientConfig config =
        S3ClientConfig.s3Builder()
            .region(Region.US_WEST_1)
            .endpointOverride(endpointOverride)
            .accessKey("access")
            .secretKey("secret")
            .minimumPartSizeForDownload(1000L)
            .build();
    assertEquals(Optional.of(Region.US_WEST_1), config.getRegion());
    assertEquals(Optional.of(endpointOverride), config.getEndpointOverride());
    assertEquals(Optional.of("access"), config.getAccessKey());
    assertEquals(Optional.of("secret"), config.getSecretKey());
    assertEquals(1000L, config.getMinimumPartSizeForDownload());
  }
}
