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
import com.google.common.base.Strings;
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

    if (!Strings.isNullOrEmpty(System.getProperty("s3.local"))) {
      return createS3MinioClient(options.getS3Region());
    }

    return createS3Client(options.getS3Region());
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(Regions awsS3Region) {

    if (!Strings.isNullOrEmpty(System.getProperty("s3.local"))) {
      return createS3MinioClient(awsS3Region);
    }

    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(awsS3Region).build();
    return s3Client;
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient(Regions awsS3Region) {
    // default username
    String minioUserName = "bfdLocalS3Dev";
    // default password
    String minioPassword = "bfdLocalS3Dev";

    if (!Strings.isNullOrEmpty(System.getProperty("s3.localUser"))) {
      minioUserName = System.getProperty("s3.localUser");
    }

    if (!Strings.isNullOrEmpty(System.getProperty("s3.localPass"))) {
      minioPassword = System.getProperty("s3.localPass");
    }

    // Default minioEndpoint address
    String minioEndpointAddress = "http://localhost:9000";

    if (!Strings.isNullOrEmpty(System.getProperty("s3.localAddress"))) {
      minioEndpointAddress = System.getProperty("s3.localAddress");
    }

    AWSCredentials credentials = new BasicAWSCredentials(minioUserName, minioPassword);

    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(minioEndpointAddress, awsS3Region.name()))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}
