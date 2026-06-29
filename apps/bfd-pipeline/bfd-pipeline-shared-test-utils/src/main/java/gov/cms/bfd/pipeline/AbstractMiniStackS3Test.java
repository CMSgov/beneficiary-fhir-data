package gov.cms.bfd.pipeline;

import gov.cms.bfd.AbstractMiniStackTest;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.regions.Region;

/**
 * Base class for tests that need to use a shared MiniStack container for S3 testing. Refer to <a
 * href="https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">MiniStack
 * docs</a> for explanation of the singleton container pattern.
 */
public abstract class AbstractMiniStackS3Test extends AbstractMiniStackTest {
  /** Configuration settings to connect to MiniStack container. */
  protected S3ClientConfig s3ClientConfig;

  /** Factory to create clients connected to MiniStack container. */
  protected S3ClientFactory s3ClientFactory;

  /** A {@link S3Dao} connected to the MiniStack container for use in test methods. */
  protected S3Dao s3Dao;

  /** Initializes S3 related fields and resets the database before each test runs. */
  @BeforeEach
  void initializeS3FieldsAndResetDatabase() {
    try {
      s3ClientConfig =
          S3ClientConfig.s3Builder()
              .region(Region.of(miniStack.getRegion()))
              .endpointOverride(new URI(miniStack.getEndpoint()))
              .accessKey(miniStack.getAccessKey())
              .secretKey(miniStack.getSecretKey())
              .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
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
