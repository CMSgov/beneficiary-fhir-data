package gov.cms.bfd.pipeline;

import gov.cms.bfd.AbstractLocalStackTest;
import gov.cms.bfd.pipeline.sharedutils.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Base class for tests that need to use a shared localstack container for S3 testing. Refer to <a
 * href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">localstack
 * docs</a> for explanation of the singleton container pattern.
 */
public abstract class AbstractLocalStackS3Test extends AbstractLocalStackTest {
  /** Configuration settings to connect to localstack container. */
  protected S3ClientConfig s3ClientConfig;
  /** Factory to create clients connected to localstack container. */
  protected S3ClientFactory s3ClientFactory;
  /** A client connected to the localstack container for use in test methods. */
  protected S3Client s3Client;

  /** Initializes S3 related fields before each test runs. */
  @BeforeEach
  void initializeS3Fields() {
    s3ClientConfig =
        new S3ClientConfig(
            Region.of(localstack.getRegion()),
            localstack.getEndpointOverride(LocalStackContainer.Service.S3),
            localstack.getAccessKey(),
            localstack.getSecretKey());
    s3ClientFactory = new AwsS3ClientFactory(s3ClientConfig);
    s3Client = s3ClientFactory.createS3Client();
  }
}
