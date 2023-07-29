package gov.cms.bfd.pipeline.sharedutils.s3;

import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;

/**
 * Implementation of {@link S3ClientFactory} that creates real S3 clients based on a {@link
 * AwsClientConfig} provided in it constructor.
 */
@AllArgsConstructor
public class AwsS3ClientFactory implements S3ClientFactory {
  /**
   * Passed to {@link S3CrtAsyncClientBuilder#minimumPartSizeInBytes} to cause large files to be
   * downloaded in parts of 200 mb.
   */
  public static final long MINIMUM_PART_SIZE_FOR_DOWNLOAD = 200L * 1024L * 1024L;

  /** Used to configure the S3 client builders with basic connection settings. */
  private final S3ClientConfig s3ClientConfig;

  @Override
  public S3Client createS3Client() {
    final S3ClientBuilder builder = S3Client.builder();
    builder.defaultsMode(DefaultsMode.STANDARD);
    s3ClientConfig.configureAwsService(builder);
    return builder.build();
  }

  @Override
  public S3AsyncClient createS3AsyncClient() {
    final S3CrtAsyncClientBuilder builder = S3AsyncClient.crtBuilder();
    s3ClientConfig.configureS3ServiceForAsyncS3(builder);
    builder.minimumPartSizeInBytes(MINIMUM_PART_SIZE_FOR_DOWNLOAD);
    return builder.build();
  }
}
