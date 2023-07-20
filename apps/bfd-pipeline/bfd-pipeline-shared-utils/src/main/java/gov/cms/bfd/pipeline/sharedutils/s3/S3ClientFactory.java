package gov.cms.bfd.pipeline.sharedutils.s3;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public interface S3ClientFactory {
  S3Client createS3Client(Region awsS3Region);

  S3AsyncClient createS3AsyncClient(Region awsS3Region);
}
