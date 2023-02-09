package gov.cms.bfd.pipeline.sharedutils.s3;

/**
 * S3MinioConfig is an object for configuring minio to use as a S3 bucket for local development
 * Username, password, and minioaddress can be passed in as maven parameters.
 */
public class S3MinioConfig {
  /** The username for this simulated bucket. */
  public String minioUserName;
  /** The password for this simulated bucket. */
  public String minioPassword;
  /** The endpoint address for this simulated bucket. */
  public String minioEndpointAddress;
  /** Read from the system properties to determine if minio should be used. */
  public Boolean useMinio;

  /** Singleton instance of minio. */
  private static S3MinioConfig single_instance = null;

  /** Instantiates a new S3 minio config. */
  private S3MinioConfig() {
    minioUserName = System.getProperty("s3.localUser", "bfdLocalS3Dev");
    minioPassword = System.getProperty("s3.localPass", "bfdLocalS3Dev");
    minioEndpointAddress = System.getProperty("s3.localAddress", "http://localhost:9000");
    useMinio = Boolean.parseBoolean(System.getProperty("s3.local"));
  }

  /**
   * Singleton method makes sure there is one instance and one instance only of the S3MinioConfig
   * Class.
   *
   * @return the s3 minio config
   */
  public static synchronized S3MinioConfig Singleton() {
    if (single_instance == null) {
      single_instance = new S3MinioConfig();
    }
    return single_instance;
  }
}
