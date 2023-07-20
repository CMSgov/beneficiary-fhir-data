package gov.cms.bfd.pipeline;

import gov.cms.bfd.pipeline.sharedutils.s3.AwsS3ClientFactory;
import gov.cms.bfd.pipeline.sharedutils.s3.AwsServiceConfig;
import java.util.Optional;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.regions.Region;

public class LocalStackS3ClientFactory extends AwsS3ClientFactory {
  public LocalStackS3ClientFactory(LocalStackContainer localstack) {
    super(createServiceConfig(localstack));
  }

  public static AwsServiceConfig createServiceConfig(LocalStackContainer localstack) {
    return new AwsServiceConfig(
        Optional.of(Region.of(localstack.getRegion())),
        Optional.of(localstack.getEndpointOverride(LocalStackContainer.Service.S3)),
        Optional.of(localstack.getAccessKey()),
        Optional.of(localstack.getSecretKey()));
  }
}
