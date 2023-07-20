package gov.cms.bfd.pipeline.sharedutils.s3;

import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public class AwsS3ClientFactory implements S3ClientFactory {
  @Override
  public S3Client createS3Client(Region awsS3Region) {
    return S3Client.builder().defaultsMode(DefaultsMode.STANDARD).region(awsS3Region).build();
  }

  @Override
  public S3AsyncClient createS3AsyncClient(Region awsS3Region) {
    // Split the file download (automatically using the async client) if its >200 MB
    final long maxSize = 200L * 1024L * 1024L;
    return S3AsyncClient.crtBuilder().minimumPartSizeInBytes(maxSize).region(awsS3Region).build();
  }
}
