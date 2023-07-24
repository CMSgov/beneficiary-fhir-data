package gov.cms.bfd.pipeline;

import gov.cms.bfd.pipeline.sharedutils.S3ClientConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.regions.Region;

/**
 * Implementation of {@link S3ClientFactory} that creates real S3 clients based on a {@link
 * S3ClientConfig} extracted from a {@link LocalStackContainer}. Used subclassing here rather than
 * adding an overloaded constructor in {@link AwsS3ClientFactory} because localstack is a test only
 * dependency and this class needs to live in a different project.
 */
public class LocalStackS3ClientFactory extends AwsS3ClientFactory {
  /**
   * Initializes an instance using config derived from the provided {@link LocalStackContainer}.
   *
   * @param localstack source of settings
   */
  public LocalStackS3ClientFactory(LocalStackContainer localstack) {
    super(createS3ClientConfig(localstack));
  }

  /**
   * Creates an {@link S3ClientConfig} with values extracted from a {@link LocalStackContainer}.
   *
   * @param localstack source of settings
   * @return the config
   */
  public static S3ClientConfig createS3ClientConfig(LocalStackContainer localstack) {
    return new S3ClientConfig(
        Region.of(localstack.getRegion()),
        localstack.getEndpointOverride(LocalStackContainer.Service.S3),
        localstack.getAccessKey(),
        localstack.getSecretKey());
  }
}
