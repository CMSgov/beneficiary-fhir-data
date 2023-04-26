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
 * Sets up and starts/stops the server for the end-to-end tests (or e2e tests masquerading as
 * integration tests).
 */
public class ServerExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerExecutor.class);

  /**
   * The BFD server process handle which the server runs within. We keep it around for cleanup when
   * the tests are done.
   */
  private static Process serverProcess;

  /** Keeps track of the server's STDOUT capture hook, to aid in debugging. */
  private static ProcessOutputConsumer appRunConsumer;

  /**
   * Starts the BFD server for tests. If already running, does nothing.
   *
   * @param dbUrl the db url
   * @param dbUsername the db username
   * @param dbPassword the db password
   * @return {@code true} if the server is running, if {@code false} the server failed to start up
   * @throws IOException if there is an issue setting up the server relating to accessing files
   */
  public static boolean startServer(String dbUrl, String dbUsername, String dbPassword)
      throws IOException {
    if (serverProcess == null) {
      LOGGER.info("Starting IT server with DB: {}", dbUrl);

      // Set up the paths we require for the server war dependencies
      String javaHome = System.getProperty("java.home", "");
      String targetPath = "target";
      String workDirectory = "target/server-work";
      String warArtifactLocation =
          findFirstPathMatchWithFilenameEnd(targetPath, ".war", 1).toString();
      String serverLauncher =
          findFirstPathMatchWithFilenameEnd(workDirectory, ".jar", 2).toString();
      String serverPortsFile = workDirectory + "/server-ports.properties";
      // These two files are copied into server-work during build time by maven for convenience
      // from:
      // bfd-server/dev/ssl-stores
      String keyStore = workDirectory + "/server-keystore.jks";
      String trustStore = workDirectory + "/server-truststore.jks";

      // Validate the paths and properties needed to run the server war exist
      if (!validateRequiredServerSetup(
          javaHome, warArtifactLocation, serverLauncher, serverPortsFile, keyStore, trustStore)) {
        return false;
      }

      String portFileContents = Files.readString(Path.of(serverPortsFile)).trim();
      String serverPort = portFileContents.substring(portFileContents.indexOf('=') + 1);
      LOGGER.info("Configured server to run on HTTPS port {}.", serverPort);

      ProcessBuilder appRunBuilder =
          new ProcessBuilder(
              createCommandForServerLauncherApp(workDirectory, dbUrl, dbUsername, dbPassword));
      appRunBuilder.environment().put("BFD_PORT", serverPort);
      appRunBuilder.environment().put("BFD_KEYSTORE", keyStore);
      appRunBuilder.environment().put("BFD_TRUSTSTORE", trustStore);
      appRunBuilder.environment().put("BFD_WAR", warArtifactLocation);
      appRunBuilder.environment().put("BFD_JAVA_HOME", javaHome);
      serverProcess = appRunBuilder.start();

      appRunConsumer = new ProcessOutputConsumer(serverProcess);
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Await start/finish of application startup by grepping the stdOut for keywords
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
   * Create a command for the server launcher script to be run with.
   *
   * @param workDirectory the server-work directory
   * @param dbUrl the db url
   * @param dbUsername the db username
   * @param dbPassword the db password
   * @return the command array for the server app
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

      String gcLog = workDirectory + "/gc.log";
      String maxHeapArg = System.getProperty("its.bfdServer.jvmargs", "-Xmx4g");
      String containerImageType =
          System.getProperty("its.testcontainer.db.image", TEST_CONTAINER_DATABASE_IMAGE_DEFAULT);
      // FUTURE: Inherit these from system properties? Which of these are valuable to pass?
      String v2Enabled = "true";
      String pacEnabled = "true";
      String pacOldMbiHashEnabled = "true";
      String pacClaimSourceTypes = "fiss,mcs";
      String includeFakeDrugCode = "true";
      String includeFakeOrgName = "true";
      Random rand = new Random();
      // Copied this from the startup script, but may not be needed
      String bfdServerId = String.valueOf(rand.nextInt(10240));

      List<String> args = new ArrayList<>();
      args.add(scriptPath.toAbsolutePath().toString());
      args.add(maxHeapArg);
      args.add(String.format("-Xlog:gc*:%s:time,level,tags", gcLog));
      args.add(String.format("-Dbfd-server-%s", bfdServerId));
      args.add(String.format("-DbfdServer.pac.enabled=%s", pacEnabled));
      args.add(String.format("-DbfdServer.pac.oldMbiHash.enabled=%s", pacOldMbiHashEnabled));
      args.add(String.format("-DbfdServer.pac.claimSourceTypes=%s", pacClaimSourceTypes));
      args.add(String.format("-DbfdServer.db.url=%s", dbUrl));
      args.add(String.format("-DbfdServer.db.username=%s", dbUsername));
      args.add(String.format("-DbfdServer.db.password=%s", dbPassword));
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
      // If one wishes to see what the server did, this will log the server process STDOut
      LOGGER.debug("Server STDOUT: {}", appRunConsumer.getStdoutContents());
    } else {
      LOGGER.warn("Tried to destroy server process but was not running.");
    }
  }

  /**
   * Find the first file's Path within the starting directory, traversing X depth into the
   * directory, which ends with the given value.
   *
   * @param startingDirectory the starting directory to search in
   * @param filenameEndsWith the filename ending to seearch for; includes extension
   * @param depth the depth (number of directories down) to search
   * @return the path of the found file
   * @throws IOException if the filename ending matched no files within the path and depth provided
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
   * Validate required server setup variables and paths exist.
   *
   * @param javaHome the java home to use for the server launch
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
      LOGGER.error("   found target: {}", targetIsDir);
      LOGGER.error("   found target/server-work: {}", serverWorkExists);
      LOGGER.error("   found launcher directory: {}", launcherDirExists);
      LOGGER.error("   found server port file: {}", serverPortFileExists);
      LOGGER.error("   found keystore: {}", keyStoreExists);
      LOGGER.error("   found trust store: {}", trustStoreExists);
      return false;
    }

    if (!Files.exists(Paths.get(javaHome + "/bin/java"))) {
      LOGGER.error("Test setup could not find java at: {}/bin/java", javaHome);
      return false;
    }

    if (!Files.exists(Paths.get(warArtifactLocation))) {
      LOGGER.error("Test setup could not find artifact war at: {}", warArtifactLocation);
      return false;
    }

    return true;
  }

  /**
   * Gets the standard out for the running server, useful for debugging.
   *
   * @return the standard out for the E2E server
   */
  public static String getServerStdOut() {
    if (appRunConsumer != null) {
      return appRunConsumer.getStdoutContents();
    }
    return "<Server not running>";
  }
}
