package gov.cms.bfd.pipeline.sharedutils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

/**
 * MinioContainer is a testcontainer object for configuring minio to use as an S3 bucket;
 * MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_URL are system properties that can be passed in as
 * maven cmd-line parameters.
 */
public class MinioTestContainer {
  /** Singleton instance of MinioContainer. */
  private static MinioTestContainer myInstance = null;
  /** Port number for minio. */
  private static final int DEFAULT_PORT = 9000;
  /** minio image that will be loaded into testcontainers. */
  private static final String DEFAULT_IMAGE = "minio/minio";
  /** default tag name. */
  private static final String DEFAULT_TAG = "edge";
  /** directory within container to use. */
  private static final String DEFAULT_STORAGE_DIRECTORY = "/data";
  /** minio Health check URL. */
  private static final String HEALTH_ENDPOINT = "/minio/health/ready";
  /** access user name for S3. */
  private static final String MINIO_ACCESS_KEY =
      System.getProperty("s3.localUser", "bfdLocalS3Dev");
  /** access password for S3. */
  private static final String MINIO_SECRET_KEY =
      System.getProperty("s3.localPass", "bfdLocalS3Dev");
  /** minio host and port. */
  private static final String MINIO_URL =
      System.getProperty("s3.localAddress", "http://localhost:9000");
  /** GenericContainer from testcontaers. */
  public static GenericContainer minioContainer = null;

  /** construct a {@link GenericContainer} for the minio instance and start the container. */
  public static void startContainer() {
    if (minioContainer == null) {
      createContainer();
    }
    if (minioContainer.isCreated() && !minioContainer.isRunning()) {
      minioContainer.start();
    }
  }

  /** stops the minio {@link GenericContainer} container instance. */
  public static void stopContainer() {
    if (minioContainer != null && minioContainer.isRunning()) {
      minioContainer.stop();
      minioContainer = null;
    }
  }

  /** construct a {@link GenericContainer} for the minio instance. */
  private static void createContainer() {
    minioContainer =
        new GenericContainer(DEFAULT_IMAGE + ":" + DEFAULT_TAG)
            .withEnv("MINIO_ACCESS_KEY", MINIO_ACCESS_KEY)
            .withEnv("MINIO_SECRET_KEY", MINIO_SECRET_KEY)
            .withCommand("server", DEFAULT_STORAGE_DIRECTORY)
            .withExposedPorts(DEFAULT_PORT)
            .withNetworkAliases("minio-" + Base58.randomString(6))
            .waitingFor(
                new HttpWaitStrategy()
                    .forPath(HEALTH_ENDPOINT)
                    .forPort(DEFAULT_PORT)
                    .withStartupTimeout(Duration.ofSeconds(20)));
  }

  /**
   * Creates and returns a new s3 client via minio; uses {@link BasicAWSCredentials} to connect to
   * the minio client.
   *
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient() {
    AWSCredentials credentials = new BasicAWSCredentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY);
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
                MINIO_URL, SharedS3Utilities.REGION_DEFAULT.name()))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }
}
