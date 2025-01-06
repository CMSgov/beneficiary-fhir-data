package gov.cms.bfd.pipeline;

import gov.cms.bfd.AbstractLocalStackTest;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.regions.Region;

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

  /** A {@link S3Dao} connected to the localstack container for use in test methods. */
  protected S3Dao s3Dao;

  /** Initializes S3 related fields before each test runs. */
  @BeforeEach
  void initializeS3Fields() {
    s3ClientConfig =
        S3ClientConfig.s3Builder()
            .region(Region.of(localstack.getRegion()))
            .endpointOverride(localstack.getEndpoint())
            .accessKey(localstack.getAccessKey())
            .secretKey(localstack.getSecretKey())
            .build();
    s3ClientFactory = new AwsS3ClientFactory(s3ClientConfig);
    s3Dao = s3ClientFactory.createS3Dao();
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /** Closes the {@link #s3Dao}. */
  @AfterEach
  void closeS3Dao() {
    s3Dao.close();
  }
}
