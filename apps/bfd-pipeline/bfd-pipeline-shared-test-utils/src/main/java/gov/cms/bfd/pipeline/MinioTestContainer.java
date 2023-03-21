package gov.cms.bfd.pipeline;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import gov.cms.bfd.pipeline.sharedutils.s3.S3MinioConfig;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

  /** Port number for minio. */
  private static final int DEFAULT_PORT = 9000;
  /** access user name for S3. */
  public static final String MINIO_ACCESS_KEY = System.getProperty("s3.localUser", "bfdLocalS3Dev");
  /** access password for S3. */
  public static final String MINIO_SECRET_KEY = System.getProperty("s3.localPass", "bfdLocalS3Dev");
  /** DockerComposeContainer from testcontainers. */
  public static DockerComposeContainer minioContainer = null;
  /** YAML file. */
  private static File yamlFile;

  /**
   * docker compose file contents; done this way as this class may be called upon from anywhere in
   * bfd-pipeline tests. All we care about is that minio is running.
   */
  private static final String dockerCompose =
      "version: '3.7'\n"
          + "services:\n"
          + "  minio-service:\n"
          + "    image: minio/minio:latest\n"
          + "    command: minio server --console-address :9001 /data\n"
          + "    ports:\n"
          + "      - \"9000:9000\"\n"
          + "      - \"9001:9001\"\n"
          + "    environment:\n"
          + "      MINIO_ROOT_USER: bfdLocalS3Dev\n"
          + "      MINIO_ROOT_PASSWORD: bfdLocalS3Dev";

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
   * Instantiates a new {@link MinioTestContainer} by leveraging testconatins to spin-up a docker
   * minio image using a docker compose resource file.
   */
  private MinioTestContainer() {
    String dockerFileName = System.getProperty("java.io.tmpdir") + "/docker-compose.yml";
    try {
      FileOutputStream fos = new FileOutputStream(dockerFileName);
      fos.write(dockerCompose.getBytes());
      fos.close();
      yamlFile = new File(dockerFileName);

      minioContainer =
          new DockerComposeContainer<>(yamlFile).withExposedService("minio-service", DEFAULT_PORT);
      minioContainer.start();

      S3MinioConfig.Singleton()
          .setConfig(MINIO_ACCESS_KEY, MINIO_SECRET_KEY, "http://localhost:" + DEFAULT_PORT, true);
      // yamlFile.delete();
      LOGGER.info("MinioTestsContainer started....used Docker file: " + dockerFileName);
    } catch (IOException e) {
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
