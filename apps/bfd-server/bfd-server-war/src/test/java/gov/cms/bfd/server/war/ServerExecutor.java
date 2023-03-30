package gov.cms.bfd.server.war;

import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_IMAGE_DEFAULT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import gov.cms.bfd.ProcessOutputConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

/**
 * Sets up and starts/stops the server for the integration tests.
 *
 * <p>IDEALLY our ITs would not spin the entire server up; this can/should be used in high-level
 * end-to-end tests. THe ITs really should be mocking all external resources and testing the
 * integration of internal classes, while our end-to-end tests create test doubles and spin a server
 * up like this. Those tests may belong in server-launcher or somewhere else.
 */
public class ServerExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerExecutor.class);

  /** The Server process handle. */
  private static Process serverProcess;

  /** Keeps track of the STDOUT of the process for debugging. */
  private static ProcessOutputConsumer appRunConsumer;

  /**
   * Starts the BFD server for IT tests. If already running, does nothing.
   *
   * @param dbUrl the db url
   * @param dbUsername the db username
   * @param dbPass the db pass
   * @return if the server is running
   * @throws IOException if there is an issue setting up the server
   */
  public static boolean startServer(String dbUrl, String dbUsername, String dbPass)
      throws IOException {
    if (serverProcess == null) {
      LOGGER.info("Starting IT server with DB: {}", dbUrl);
      String javaHome = System.getProperty("java.home", "");
      String targetPath = "target";
      String workDirectory = "target/server-work";
      String warArtifactLocation =
          findFirstPathMatchWithFilenameEnd(targetPath, ".war", 1).toString();
      String serverLauncher =
          findFirstPathMatchWithFilenameEnd(workDirectory, ".jar", 2).toString();
      String serverPortsFile = workDirectory + "/server-ports.properties";
      String keyStore = workDirectory + "/server-keystore.jks";
      String trustStore = workDirectory + "/server-truststore.jks";

      // Validate the paths and properties needed exist
      if (!validateRequiredServerSetup(
          javaHome, warArtifactLocation, serverLauncher, serverPortsFile, keyStore, trustStore)) {
        return false;
      }

      String portFileContents = Files.readString(Path.of(serverPortsFile)).trim();
      String serverPort = portFileContents.substring(portFileContents.indexOf('=') + 1);
      LOGGER.info("Configured server to run on HTTPS port {}.", serverPort);

      ProcessBuilder appRunBuilder =
          new ProcessBuilder(
              createCommandForServerLauncherApp(workDirectory, dbUrl, dbUsername, dbPass));
      appRunBuilder.environment().put("BFD_PORT", serverPort);
      appRunBuilder.environment().put("BFD_KEYSTORE", keyStore);
      appRunBuilder.environment().put("BFD_TRUSTSTORE", trustStore);
      appRunBuilder.environment().put("BFD_WAR", warArtifactLocation);
      appRunBuilder.environment().put("BFD_JAVA_HOME", javaHome);
      serverProcess = appRunBuilder.start();

      appRunConsumer = new ProcessOutputConsumer(serverProcess);
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Await start/finish of application
      String failureMessage = "Failed startup of context";
      String successMessage = "Started Jetty.";
      try {
        Awaitility.await()
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () ->
                    appRunConsumer.getStdoutContents().contains(successMessage)
                        || appRunConsumer.getStdoutContents().contains(failureMessage));
      } catch (ConditionTimeoutException e) {
        throw new RuntimeException(
            "Error: Server failed to start within 120 seconds. STDOUT:\n"
                + appRunConsumer.getStdoutContents(),
            e);
      }
      // Fail fast if we didn't start the server correctly
      assertFalse(
          "Server failed to start due to an error. STDOUT: " + appRunConsumer.getStdoutContents(),
          appRunConsumer.getStdoutContents().contains(failureMessage));
      assertTrue(
          "Did not find the server start message in STDOUT: " + appRunConsumer.getStdoutContents(),
          appRunConsumer.getStdoutContents().contains(successMessage));
    }
    // do nothing if we've already got a server started

    return true;
  }

  /**
   * Create command for the server launcher script to be run.
   *
   * @param workDirectory the work directory
   * @param dbUrl the db url
   * @param dbUsername the db username
   * @param dbPassword the db password
   * @return the command array for the migrator app
   */
  private static String[] createCommandForServerLauncherApp(
      String workDirectory, String dbUrl, String dbUsername, String dbPassword) {
    try {
      Path assemblyDirectory =
          Files.list(Paths.get(workDirectory))
              .filter(f -> f.getFileName().toString().startsWith("bfd-server-launcher-"))
              .findFirst()
              .orElse(Path.of(""));
      Path scriptPath = findFirstPathMatchWithFilenameEnd(assemblyDirectory.toString(), ".sh", 1);

      // TODO/FUTURE: Inherit these from system properties with defaults? The script did
      String gcLog = workDirectory + "/gc.log";
      String maxHeapArg = System.getProperty("its.bfdServer.jvmargs", "-Xmx4g");
      String containerImageType =
          System.getProperty("its.testcontainer.db.image", TEST_CONTAINER_DATABASE_IMAGE_DEFAULT);
      String v2Enabled = "true";
      String pacEnabled = "true";
      String pacOldMbiHashEnabled = "true";
      String pacClaimSourceTypes = "fiss,mcs";
      String includeFakeDrugCode = "true";
      String includeFakeOrgName = "true";
      Random rand = new Random();
      String bfdServerId = "" + rand.nextInt(10240);

      List<String> args = new ArrayList<>();
      args.add(scriptPath.toAbsolutePath().toString());
      args.add(maxHeapArg);
      args.add(String.format("-Xlog:gc*:%s:time,level,tags", gcLog));
      args.add(String.format("-Dbfd-server-%s", bfdServerId));
      args.add(String.format("-DbfdServer.v2.enabled=%s", v2Enabled));
      args.add(String.format("-DbfdServer.pac.enabled=%s", pacEnabled));
      args.add(String.format("-DbfdServer.pac.oldMbiHash.enabled=%s", pacOldMbiHashEnabled));
      args.add(String.format("-DbfdServer.pac.claimSourceTypes=%s", pacClaimSourceTypes));
      args.add(String.format("-DbfdServer.db.url=%s", dbUrl));
      args.add(String.format("-DbfdServer.db.username=%s", dbUsername));
      args.add(String.format("-DbfdServer.db.password=%s", dbPassword));
      args.add(String.format("-Dits.db.url=%s", dbUrl));
      args.add(String.format("-Dits.db.username=%s", dbUsername));
      args.add(String.format("-Dits.db.password=%s", dbPassword));
      args.add("-DbfdServer.db.schema.apply=true");
      args.add(String.format("-DbfdServer.include.fake.drug.code=%s", includeFakeDrugCode));
      args.add(String.format("-DbfdServer.include.fake.org.name=%s", includeFakeOrgName));
      args.add(String.format("-Dits.testcontainer.db.image=%s", containerImageType));
      return args.toArray(new String[0]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Checks if the server is running.
   *
   * @return true if the server is running
   */
  public static boolean isRunning() {
    return serverProcess != null && serverProcess.isAlive();
  }

  /** Stops the server process. */
  public static void stopServer() {
    if (serverProcess != null && serverProcess.isAlive()) {
      serverProcess.destroy();
      LOGGER.info("Destroyed server process.");
      // TODO: Remove or change to DEBUG
      LOGGER.info("Server STDOUT: {}", appRunConsumer.getStdoutContents());
    } else {
      LOGGER.warn("Tried to destroy server process but was not running.");
    }
  }

  /**
   * Find first path match with filename end path.
   *
   * @param startingDirectory the starting directory
   * @param filenameEndsWith the filename ends with
   * @param depth the depth
   * @return the path
   * @throws IOException the io exception
   */
  private static Path findFirstPathMatchWithFilenameEnd(
      String startingDirectory, String filenameEndsWith, int depth) throws IOException {

    List<Path> findPaths;
    try (Stream<Path> pathStream =
        Files.find(
            Paths.get(startingDirectory),
            depth,
            (p, basicFileAttributes) -> p.getFileName().toString().endsWith(filenameEndsWith))) {
      findPaths = pathStream.collect(Collectors.toList());
    }

    if (findPaths.stream().findFirst().isPresent()) {
      return findPaths.stream().findFirst().get();
    } else {
      throw new IOException(
          "Unable to find path ending with '"
              + filenameEndsWith
              + "' within : "
              + startingDirectory);
    }
  }

  /**
   * Validate required server setup variables and paths.
   *
   * @param javaHome the java home
   * @param warArtifactLocation the war artifact location
   * @param serverLauncherLocation the server launcher location
   * @param serverPortsFileLocation the server ports file location
   * @param keyStoreLocation the key store location
   * @param trustStoreLocation the trust store location
   * @return false if required paths or properties dont exist
   */
  private static boolean validateRequiredServerSetup(
      String javaHome,
      String warArtifactLocation,
      String serverLauncherLocation,
      String serverPortsFileLocation,
      String keyStoreLocation,
      String trustStoreLocation) {

    // Check required paths exist
    boolean targetIsDir = Files.isDirectory(Paths.get("target"));
    boolean serverWorkExists = Files.exists(Paths.get("target", "server-work"));
    boolean launcherDirExists = Files.exists(Paths.get(serverLauncherLocation));
    boolean serverPortFileExists = Files.exists(Paths.get(serverPortsFileLocation));
    boolean keyStoreExists = Files.exists(Paths.get(keyStoreLocation));
    boolean trustStoreExists = Files.exists(Paths.get(trustStoreLocation));
    if (!targetIsDir
        || !serverWorkExists
        || !launcherDirExists
        || !serverPortFileExists
        || !keyStoreExists
        || !trustStoreExists) {
      LOGGER.error("Could not setup server; could not find required path.");
      LOGGER.error("   target: {}", targetIsDir);
      LOGGER.error("   target/server-work: {}", serverWorkExists);
      LOGGER.error("   launcher directory: {}", launcherDirExists);
      LOGGER.error("   server port file: {}", serverPortFileExists);
      LOGGER.error("   keystore: {}", keyStoreExists);
      LOGGER.error("   trust store: {}", trustStoreExists);
      return false;
    }

    if (!Files.exists(Paths.get(javaHome + "/bin/java"))) {
      // if java path couldnt be found, blow up
      LOGGER.error("Test setup could not find java at: " + javaHome + "/bin/java");
      return false;
    }

    if (!Files.exists(Paths.get(warArtifactLocation))) {
      LOGGER.error("Test setup could not find artifact war at: " + warArtifactLocation);
      return false;
    }

    return true;
  }
}
