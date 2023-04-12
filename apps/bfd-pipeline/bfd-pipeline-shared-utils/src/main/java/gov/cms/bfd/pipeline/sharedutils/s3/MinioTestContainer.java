package gov.cms.bfd.pipeline.sharedutils.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
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
public abstract class MinioTestContainer {
  private static final Logger LOGGER = LoggerFactory.getLogger(MinioTestContainer.class);

  /** Singleton instance of MinioContainer. */
  static final DockerComposeContainer<?> MINIO_CONTAINER;
  /** S3 access port number for minio. */
  private static final int DEFAULT_PORT = 9000;
  /** minio console Port number. */
  private static final int CONSOLE_PORT = DEFAULT_PORT + 1;
  /** access user name for S3. */
  public static final String MINIO_ACCESS_KEY = System.getProperty("s3.localUser", "bfdLocalS3Dev");
  /** access password for S3. */
  public static final String MINIO_SECRET_KEY = System.getProperty("s3.localPass", "bfdLocalS3Dev");
  /** MinIO host url. */
  public static final String MINIO_HOST_URL = "http://127.0.0.1:" + Integer.toString(DEFAULT_PORT);
  /** YAML file. */
  private static File yamlFile = null;
  /** keep our own version of MinioConfig. */
  private static final S3MinioConfig minioConfig;

  static {
    createYamlFile();
    LOGGER.info("starting MinioTestContainer in testcontainers");
    MINIO_CONTAINER =
        new DockerComposeContainer<>(yamlFile).withExposedService("minio-service", DEFAULT_PORT);
    MINIO_CONTAINER.start();
    String minioHostPort = MINIO_CONTAINER.getServiceHost("minio-service", DEFAULT_PORT);
    LOGGER.info("MinioTestContainer started...listening on: " + minioHostPort);

    minioConfig = S3MinioConfig.Singleton();
    minioConfig.setConfig(MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_HOST_URL, true);
  }

  /**
   * Creates (as needed) a new {@link AmazonS3} minio client.
   *
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient() {
    return SharedS3Utilities.createS3MinioClient(Regions.AP_EAST_1, minioConfig);
  }

  /** support manual stop of {@link DockerComposeContainer} testcontainer. */
  public static void forceStop() {
    MINIO_CONTAINER.stop();
  }

  /**
   * Creates a Docker compose file that will be used to create the MinIO Docker container.
   *
   * @throws IOException if file cannot be created.
   */
  private static void createYamlFile() {
    String dockerFileName = System.getProperty("java.io.tmpdir") + "/docker-compose.yml";
    // build a docker compose file that we can feed to testcontainers; done this
    // way as MinioTestContainer may be called from anywhere so a static resource
    // file would require a search, etc.
    StringBuilder sb = new StringBuilder();
    sb.append("version: '3.7'\n");
    sb.append("services:\n");
    sb.append("  minio-service:\n");
    sb.append("    image: minio/minio:latest\n");
    sb.append("    ports:\n");
    sb.append("      - '")
        .append(Integer.toString(DEFAULT_PORT))
        .append(":")
        .append(Integer.toString(DEFAULT_PORT))
        .append("'\n");
    sb.append("      - '")
        .append(Integer.toString(CONSOLE_PORT))
        .append(":")
        .append(Integer.toString(CONSOLE_PORT))
        .append("'\n");
    sb.append("    volumes:\n");
    sb.append("      - minio_storage:/data\n");
    sb.append("    environment:\n");
    sb.append("      MINIO_ROOT_USER: ").append(MINIO_ACCESS_KEY).append("\n");
    sb.append("      MINIO_ROOT_PASSWORD: ").append(MINIO_SECRET_KEY).append("\n");
    sb.append("      MINIO_SERVER_URL: ").append(MINIO_HOST_URL).append("\n");
    sb.append("    command: server --console-address :'")
        .append(Integer.toString(CONSOLE_PORT))
        .append("' /data\n");
    sb.append("    healthcheck:\n");
    sb.append("      test: ['CMD', 'curl', '-f', '")
        .append(MINIO_HOST_URL)
        .append("/minio/health/live']\n");
    sb.append("      interval: 20s\n");
    sb.append("      timeout: 10s\n");
    sb.append("      retries: 3\n");
    sb.append("volumes:\n");
    sb.append("  minio_storage: {}\n");

    try {
      FileOutputStream fos = new FileOutputStream(dockerFileName);
      fos.write(sb.toString().getBytes());
      fos.close();
      yamlFile = new File(dockerFileName);
      LOGGER.info("Create MinIO YAML file:  " + yamlFile.getAbsolutePath());
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }
}
