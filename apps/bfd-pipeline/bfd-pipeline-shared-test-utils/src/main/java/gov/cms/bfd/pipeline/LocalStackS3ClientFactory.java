package gov.cms.bfd.pipeline;

import gov.cms.bfd.pipeline.sharedutils.s3.S3ClientFactory;
import lombok.AllArgsConstructor;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

@AllArgsConstructor
public class LocalStackS3ClientFactory implements S3ClientFactory {
  private final LocalStackContainer localstack;

  @Override
  public S3Client createS3Client(Region ignored) {
    return S3Client.builder()
        .region(Region.of(localstack.getRegion()))
        .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .build();
  }

  @Override
  public S3AsyncClient createS3AsyncClient(Region ignored) {
    // Split the file download (automatically using the async client) if its >200 MB
    final long maxSize = 200L * 1024L * 1024L;
    return S3AsyncClient.crtBuilder()
        .minimumPartSizeInBytes(maxSize)
        .region(Region.of(localstack.getRegion()))
        .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SQS))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
        .build();
  }
}
