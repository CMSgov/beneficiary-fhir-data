package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cms.bfd.pipeline.ccw.rif.extract.ExtractionOptions;

/** Contains utility/helper methods for AWS S3 that can be used in application and test code. */
public final class S3Utilities {
  /** The default AWS {@link Region} to interact with. */
  public static final Regions REGION_DEFAULT = Regions.US_EAST_1;

  /**
   * @param options the {@link ExtractionOptions} to use
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(ExtractionOptions options) {
    S3MinioConfig minioConfig = S3MinioConfig.Singleton();

    // if (minioConfig.useMinio) {
    return createS3MinioClient(options.getS3Region(), minioConfig);
    // }

    // return createS3Client(options.getS3Region());
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(Regions awsS3Region) {
    S3MinioConfig minioConfig = S3MinioConfig.Singleton();

    if (minioConfig.useMinio) {
      return createS3MinioClient(awsS3Region, minioConfig);
    }
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(awsS3Region).build();
    return s3Client;
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @param minioConfig passes the minioConfig to use
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient(Regions awsS3Region, S3MinioConfig minioConfig) {
    // Uses BasicCredentials to connect to the minio client and gets the username,password, and
    // address from the minioconfig
    AWSCredentials credentials =
        new BasicAWSCredentials(minioConfig.minioUserName, minioConfig.minioPassword);

    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
                minioConfig.minioEndpointAddress, awsS3Region.name()))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}
