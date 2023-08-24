package gov.cms.bfd.server.war;

import gov.cms.bfd.server.launcher.AppConfiguration;
import gov.cms.bfd.server.launcher.DataServerLauncherApp;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

/**
 * Sets up and starts/stops the server for the end-to-end tests (or e2e tests masquerading as
 * integration tests).
 */
public class ServerExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServerExecutor.class);

  private static DataServerLauncherApp.ServerStuff serverStuff;

  /** Keeps track of the server port we're running the server on. */
  private static String testServerPort;

  /**
   * Starts the BFD server for tests. If already running, does nothing.
   *
   * @param dbUrl the db url
   * @param dbUsername the db username
   * @param dbPassword the db password
   * @return {@code true} if the server is running, if {@code false} the server failed to start up
   * @throws IOException if there is an issue setting up the server relating to accessing files
   */
  public static synchronized boolean startServer(String dbUrl, String dbUsername, String dbPassword)
      throws IOException {
    if (serverStuff == null) {
      LOGGER.info("Starting IT server with DB: {}", dbUrl);

      // Configure Java Util Logging (JUL) to route over SLF4J, instead.
      SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)
      SLF4JBridgeHandler.install();

      // Set up the paths we require for the server war dependencies
      String targetPath = "target";
      String workDirectory = "target/server-work";
      String warArtifactLocation = "target/test-war-directory";
      String serverPortsFile = workDirectory + "/server-ports.properties";
      // These two files are copied into server-work during build time by maven for convenience
      // from:
      // bfd-server/dev/ssl-stores
      String keyStore = workDirectory + "/server-keystore.pfx";
      String trustStore = workDirectory + "/server-truststore.pfx";

      // Validate the paths and properties needed to run the server war exist
      if (!validateRequiredServerSetup(
          warArtifactLocation, serverPortsFile, keyStore, trustStore)) {
        return false;
      }

      String portFileContents = Files.readString(Path.of(serverPortsFile)).trim();
      String serverPort = portFileContents.substring(portFileContents.indexOf('=') + 1);
      LOGGER.info("Configured server to run on HTTPS port {}.", serverPort);

      final var appSettings = new HashMap<String, String>();
      addServerSettings(appSettings, workDirectory, dbUrl, dbUsername, dbPassword);
      final var configLoader = ConfigLoader.builder().addSingle(appSettings::get).build();
      appSettings.put("BFD_PORT", serverPort);
      appSettings.put("BFD_KEYSTORE", keyStore);
      appSettings.put("BFD_TRUSTSTORE", trustStore);
      appSettings.put("BFD_WAR", warArtifactLocation);
      AppConfiguration appConfig = AppConfiguration.loadConfig(configLoader);
      serverStuff = DataServerLauncherApp.createServer(appConfig);
      serverStuff
          .getWebapp()
          .setAttribute(
              "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern", ".*/classes/.*");

      // install our config so it will be used directly by the application
      serverStuff
          .getWebapp()
          .setAttribute(SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME, configLoader);

      try {
        serverStuff.getServer().start();
      } catch (Exception ex) {
        throw new RuntimeException("Caught exception when starting server.", ex);
      }

      try {
        Awaitility.await()
            .atMost(2, TimeUnit.MINUTES)
            .until(
                () -> {
                  try (Socket ignored = new Socket("localhost", Integer.parseInt(serverPort))) {
                    return true;
                  } catch (Exception e) {
                    return false;
                  }
                });
      } catch (ConditionTimeoutException e) {
        throw new RuntimeException("Error: Server failed to start within 120 seconds.", e);
      }
      testServerPort = serverPort;
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
   */
  private static void addServerSettings(
      Map<String, String> appSettings,
      String workDirectory,
      String dbUrl,
      String dbUsername,
      String dbPassword) {
    // FUTURE: Inherit these from system properties? Which of these are valuable to pass?
    String v2Enabled = "true";
    String pacEnabled = "true";
    String pacOldMbiHashEnabled = "true";
    String pacClaimSourceTypes = "fiss,mcs";
    String includeFakeDrugCode = "true";
    String includeFakeOrgName = "true";
    Random rand = new Random();
    // Copied this from the startup script, but may not be needed
    //      String bfdServerId = String.valueOf(rand.nextInt(10240));

    //      args.add(String.format("-Dbfd-server-%s", bfdServerId));
    appSettings.put("bfdServer.pac.enabled", pacEnabled);
    appSettings.put("bfdServer.pac.oldMbiHash.enabled", pacOldMbiHashEnabled);
    appSettings.put("bfdServer.pac.claimSourceTypes", pacClaimSourceTypes);
    appSettings.put("bfdServer.db.url", dbUrl);
    appSettings.put("bfdServer.db.username", dbUsername);
    appSettings.put("bfdServer.db.password", dbPassword);
    appSettings.put("bfdServer.include.fake.drug.code", includeFakeDrugCode);
    appSettings.put("bfdServer.include.fake.org.name", includeFakeOrgName);
  }

  public static synchronized String getServerPort() {
    return testServerPort;
  }

  /**
   * Checks if the server is running.
   *
   * @return true if the server is running
   */
  public static synchronized boolean isRunning() {
    return serverStuff != null && serverStuff.getServer().isRunning();
  }

  /** Stops the server process. */
  public static synchronized void stopServer() {
    if (isRunning()) {
      try {
        serverStuff.getServer().stop();
        serverStuff.getServer().join();
      } catch (Exception ex) {
        LOGGER.error("Caught exception while stopping server: message={}", ex.getMessage(), ex);
        throw new RuntimeException(ex);
      }
      serverStuff = null;
      testServerPort = null;
      LOGGER.info("Stopped server.");
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
   * @param warArtifactLocation the war artifact location
   * @param serverPortsFileLocation the server ports file location
   * @param keyStoreLocation the key store location
   * @param trustStoreLocation the trust store location
   * @return false if required paths or properties dont exist
   */
  private static boolean validateRequiredServerSetup(
      String warArtifactLocation,
      String serverPortsFileLocation,
      String keyStoreLocation,
      String trustStoreLocation) {

    // Check required paths exist
    boolean targetIsDir = Files.isDirectory(Paths.get("target"));
    boolean serverWorkExists = Files.exists(Paths.get("target", "server-work"));
    boolean serverPortFileExists = Files.exists(Paths.get(serverPortsFileLocation));
    boolean keyStoreExists = Files.exists(Paths.get(keyStoreLocation));
    boolean trustStoreExists = Files.exists(Paths.get(trustStoreLocation));
    if (!targetIsDir
        || !serverWorkExists
        || !serverPortFileExists
        || !keyStoreExists
        || !trustStoreExists) {
      LOGGER.error("Could not setup server; could not find required path.");
      LOGGER.error("   found target: {}", targetIsDir);
      LOGGER.error("   found target/server-work: {}", serverWorkExists);
      LOGGER.error("   found server port file: {}", serverPortFileExists);
      LOGGER.error("   found keystore: {}", keyStoreExists);
      LOGGER.error("   found trust store: {}", trustStoreExists);
      return false;
    }

    if (!Files.exists(Paths.get(warArtifactLocation))) {
      LOGGER.error("Test setup could not find artifact war at: {}", warArtifactLocation);
      return false;
    }

    return true;
  }
}
