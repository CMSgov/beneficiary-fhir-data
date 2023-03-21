package gov.cms.bfd.pipeline;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import gov.cms.bfd.pipeline.sharedutils.s3.S3MinioConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.io.File;
import java.io.FileOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;

/**
 * MinioContainer is a testcontainer object for configuring minio to use as an S3 bucket;
 * MINIO_ACCESS_KEY, MINIO_SECRET_KEY, and MINIO_URL are system properties that can be passed in as
 * maven cmd-line parameters.
 */
public class MinioTestContainer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MinioTestContainer.class);

  /** Singleton instance of MinioContainer. */
  private static MinioTestContainer myInstance = null;

  /** S3 access port number for minio. */
  private static final int DEFAULT_PORT = 9000;
  /** minio console Port number. */
  private static final int CONSOLE_PORT = DEFAULT_PORT + 1;
  /** access user name for S3. */
  public static final String MINIO_ACCESS_KEY = System.getProperty("s3.localUser", "bfdLocalS3Dev");
  /** access password for S3. */
  public static final String MINIO_SECRET_KEY = System.getProperty("s3.localPass", "bfdLocalS3Dev");
  /** YAML file. */
  private static File yamlFile;
  /** DockerComposeContainer from testcontainers. */
  public static DockerComposeContainer minioContainer = null;

  /**
   * Singleton method to ensure there is only one instance of the {@link MinioTestContainer} class.
   *
   * @return {@link MinioTestContainer}
   */
  public static MinioTestContainer getInstance() {
    if (myInstance == null) {
      myInstance = new MinioTestContainer();
    }
    return myInstance;
  }

  /**
   * Instantiates a new {@link MinioTestContainer} by leveraging testcontainers to spin-up a docker
   * minio image using a docker compose resource file that we create.
   */
  private MinioTestContainer() {
    String dockerFileName = System.getProperty("java.io.tmpdir") + "/docker-compose.yml";
    // build a docker compose file that we can feed to testcontainers; done this
    // way as MinioTestContainer may be called from anywhere so a static resource
    // file would require a search, etc.
    StringBuilder sb = new StringBuilder();
    sb.append("version: '3.7'\n")
        .append("services:\n")
        .append("  minio-service:\n")
        .append("    image: minio/minio:latest\n");
    sb.append("    command: minio server --console-address :")
        .append(Integer.toString(CONSOLE_PORT))
        .append(" /data\n");
    sb.append("      - \"")
        .append(Integer.toString(DEFAULT_PORT))
        .append(":")
        .append(Integer.toString(DEFAULT_PORT))
        .append("\"\n");
    sb.append("      - \"")
        .append(Integer.toString(CONSOLE_PORT))
        .append(":")
        .append(Integer.toString(CONSOLE_PORT))
        .append("\"\n");
    sb.append("    environment:\n")
        .append("      MINIO_ROOT_USER: ")
        .append(MINIO_ACCESS_KEY)
        .append("\n")
        .append("      MINIO_ROOT_PASSWORD: ")
        .append(MINIO_SECRET_KEY)
        .append("\n");

    try {
      FileOutputStream fos = new FileOutputStream(dockerFileName);
      fos.write(sb.toString().getBytes());
      fos.close();
      yamlFile = new File(dockerFileName);

      /*
       * create a MinIO container by leveraging testcontainers DockerComposeContainer functionality;
       * not the most stylistic way to go about this, but it is cheap/easy and at some point we'll
       * look to replacing MinIO with real-life mocking.
       */
      minioContainer =
          new DockerComposeContainer<>(yamlFile).withExposedService("minio-service", DEFAULT_PORT);
      minioContainer.start();

      /*
       * bit of a hack here; if the S3MinioConfig.useMinio is set to true, all S3-related utilities
       * will bypass any AWS-specific S3 functionality (like permissions, KMS, etc.) when creating
       * buckets or adding artifacts to S3 buckets. So we'll just set that status and current S3
       * utilities will know what to do.
       */
      S3MinioConfig.Singleton()
          .setConfig(MINIO_ACCESS_KEY, MINIO_SECRET_KEY, "http://localhost:" + DEFAULT_PORT, true);

      LOGGER.info("MinioTestsContainer started....used Docker file: " + dockerFileName);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** stops the minio {@link DockerComposeContainer} container instance. */
  public void stopContainer() {
    if (minioContainer != null) {
      LOGGER.info("MinioTestContainer closing....");
      minioContainer.stop();
    }
  }

  /**
   * Creates (as needed) a new {@link AmazonS3} minio client.
   *
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient() {
    return SharedS3Utilities.createS3MinioClient(Regions.AP_EAST_1, S3MinioConfig.Singleton());
  }
}
