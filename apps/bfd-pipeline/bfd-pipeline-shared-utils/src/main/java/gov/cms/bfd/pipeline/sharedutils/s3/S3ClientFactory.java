package gov.cms.bfd.pipeline.sharedutils.s3;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public interface S3ClientFactory {
  S3Client createS3Client();

  S3AsyncClient createS3AsyncClient();
}
