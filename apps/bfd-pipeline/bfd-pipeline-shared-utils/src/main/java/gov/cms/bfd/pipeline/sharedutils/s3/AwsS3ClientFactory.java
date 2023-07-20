package gov.cms.bfd.pipeline.sharedutils.s3;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

@AllArgsConstructor
public class AwsS3ClientFactory implements S3ClientFactory {
  private final AwsServiceConfig awsServiceConfig;

  @Override
  public S3Client createS3Client() {
    final S3ClientBuilder builder = S3Client.builder();
    builder.defaultsMode(DefaultsMode.STANDARD);
    awsServiceConfig.configureS3Service(builder);
    return builder.build();
  }

  @Override
  public S3AsyncClient createS3AsyncClient() {
    final S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();
    awsServiceConfig.configureS3Service(builder);
    // Split the file download (automatically using the async client) if its >200 MB
    builder.minimumPartSizeInBytes(200L * 1024L * 1024L);
    return builder.build();
  }
}
