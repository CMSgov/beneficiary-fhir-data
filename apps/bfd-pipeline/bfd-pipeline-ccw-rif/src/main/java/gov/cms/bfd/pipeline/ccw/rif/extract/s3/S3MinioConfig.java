package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

public class S3MinioConfig {

  public String minioUserName;
  public String minioPassword;
  public String minioEndpointAddress;
  public Boolean useMinio;

  private static S3MinioConfig single_instance = null;

  // Constructor of this class
  // Here private constructor is is used to
  // restricted to this class itself
  private S3MinioConfig() {
    minioUserName = System.getProperty("s3.localUser", "bfdLocalS3Dev");
    minioPassword = System.getProperty("s3.localPass", "bfdLocalS3Dev");
    minioEndpointAddress = System.getProperty("s3.localAddress", "http://localhost:9000");
    useMinio = Boolean.parseBoolean(System.getProperty("s3.local", "false"));
  }

  // Method
  // Static method to create instance of Singleton class
  synchronized public static S3MinioConfig Singleton() {
    // To ensure only one instance is created
    if (single_instance == null) {
      single_instance = new S3MinioConfig();
    }
    return single_instance;
  }
}
