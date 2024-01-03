package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.ProcessOutputConsumer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents a BFD Server Launcher App process, for use in ITs. */
public final class ServerProcess implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerProcess.class);

  /** The log message for when the war was started. */
  private static final String LOG_MESSAGE_WAR_STARTED = "Johnny 5 is alive on SLF4J!";

  /** The App process under test. */
  private Process appProcess;

  /** Consumes the app stdout for validation. */
  private ProcessOutputConsumer appRunConsumer;

  /** The thread for running the {@link #appRunConsumer}. */
  private Thread appRunConsumerThread;

  /** The server uri to use. */
  private URI serverUri;

  /** The process output value. */
  private Optional<Integer> exitValue;

  /**
   * Constructs a new {@link ServerProcess}, launching a new BFD Server Launcher App process.
   *
   * @param warPath the {@link Path} to the WAR file to run with the server
   * @param jvmDebugOptions the {@link JvmDebugOptions} to use
   */
  public ServerProcess(Path warPath, JvmDebugOptions jvmDebugOptions) {
    // Start the app.
    ProcessBuilder appRunBuilder = createAppProcessBuilder(warPath, jvmDebugOptions);
    appRunBuilder.redirectErrorStream(true);
    try {
      this.appProcess = appRunBuilder.start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // Read the app's output.
    this.appRunConsumer = new ProcessOutputConsumer(appProcess);
    this.appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Wait for it to start both Jetty and the app.
    try {
      Awaitility.await().atMost(Durations.TEN_SECONDS).until(() -> hasJettyStarted(appRunConsumer));
      Awaitility.await().atMost(Durations.TEN_SECONDS).until(() -> hasWarStarted(appRunConsumer));
    } catch (ConditionTimeoutException e) {
      // Add some additional logging detail.
      throw new ConditionTimeoutException(
          "App server output did not indicate successful startup within timeout. Output:\n"
              + appRunConsumer.getStdoutContents());
    }

    // Parse the server's URI out from the log.
    Pattern serverUriPattern =
        Pattern.compile(
            String.format(
                "%s.*'(.*)'", DataServerLauncherApp.LOG_MESSAGE_STARTED_JETTY.replace(".", "\\.")),
            Pattern.MULTILINE);
    String logText = appRunConsumer.getStdoutContents().toString();
    Matcher serverUriMatcher = serverUriPattern.matcher(logText);
    assertTrue(
        serverUriMatcher.find(),
        String.format(
            "Unable to find server start message (/%s/) in log:\n%s",
            serverUriPattern.pattern(), logText));
    try {
      this.serverUri = new URI(serverUriMatcher.group(1));
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Unable to parse server URI: " + serverUriMatcher.group(1));
    }
  }

  /**
   * Gets the {@link #serverUri}.
   *
   * @return the local {@link URI} that the server is accessible at
   */
  public URI getServerUri() {
    return serverUri;
  }

  /**
   * Note: this method is inherently subject to race conditions, unless/until {@link #close()} has
   * been called. If you're calling it before then, you must always check its results in a loop with
   * some reasonable timeout.
   *
   * @return the server process' output to date
   */
  public String getProcessOutput() {
    return appRunConsumer.getStdoutContents().toString();
  }

  /** {@inheritDoc} */
  @Override
  public void close() {
    if (appProcess != null) {
      // Stop the application nicely.
      sendSigterm(appProcess);
      boolean appProcessExited = false;
      try {
        appProcessExited = appProcess.waitFor(1, TimeUnit.MINUTES);
      } catch (InterruptedException e) {
        // Do nothing; we already handle this below.
      }

      // If needed, kill the process more forcefully.
      if (!appProcessExited) appProcess.destroyForcibly();

      // Grab the process' exit code.
      this.exitValue = Optional.of(appProcess.exitValue());

      // Wait for the output consumer to complete.
      try {
        appRunConsumerThread.join();
      } catch (InterruptedException e) {
        // Do nothing: there's nothing we CAN do to recover from this.
      }

      // Clean up.
      appProcess = null;
    }
  }

  /**
   * Gets the {@link #exitValue}.
   *
   * @return the server process' result code if it's stopped, or {@link Optional#empty()} if it has
   *     not
   */
  public Optional<Integer> getResultCode() {
    return exitValue;
  }

  /**
   * Sets up a {@link ProcessBuilder} that can run the app for testing.
   *
   * @param warPath the {@link Path} to the WAR file to run with the server
   * @param jvmDebugOptions the {@link JvmDebugOptions} to use
   * @return a {@link ProcessBuilder} that can be used to launch the application
   */
  static ProcessBuilder createAppProcessBuilder(Path warPath, JvmDebugOptions jvmDebugOptions) {
    String[] command = createCommandForLauncher(jvmDebugOptions);
    LOGGER.debug("About to launch server with command: {}", Arrays.toString(command));
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

    appRunBuilder.environment().put(AppConfiguration.SSM_PATH_HOST, "127.0.0.1");
    appRunBuilder.environment().put(AppConfiguration.SSM_PATH_PORT, "0");
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.SSM_PATH_KEYSTORE,
            AppConfigurationIT.getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-keystore.pfx"))
                .toString());
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.SSM_PATH_TRUSTSTORE,
            AppConfigurationIT.getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-truststore.pfx"))
                .toString());
    appRunBuilder.environment().put(AppConfiguration.SSM_PATH_WAR, warPath.toString());

    return appRunBuilder;
  }

  /**
   * Creates the command to launch the server.
   *
   * @param jvmDebugOptions the {@link JvmDebugOptions} to use
   * @return the command array for {@link ProcessBuilder#ProcessBuilder(String...)} that will launch
   *     the application via its <code>.x</code> executable wrapper script
   */
  static String[] createCommandForLauncher(JvmDebugOptions jvmDebugOptions) {
    try {
      Path assemblyDirectory =
          Files.list(Paths.get(".", "target", "server-work"))
              .filter(f -> f.getFileName().toString().startsWith("bfd-server-launcher-"))
              .findFirst()
              .get();
      Path serverLauncherScript = assemblyDirectory.resolve("bfd-server-launcher.sh");

      return new String[] {serverLauncherScript.toAbsolutePath().toString()};
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends a <code>SIGTERM</code> to the specified {@link Process}, causing it to exit, but giving
   * it a chance to do so gracefully.
   *
   * @param process the {@link Process} to signal
   */
  static void sendSigterm(Process process) {
    if (process.getClass().getName().equals("java.lang.ProcessImpl")) {
      try {
        ProcessBuilder killBuilder =
            new ProcessBuilder("/bin/kill", "-s", "TERM", "" + process.pid());
        int killBuilderExitCode = killBuilder.start().waitFor();
        if (killBuilderExitCode != 0) process.destroy();
      } catch (SecurityException
          | IllegalArgumentException
          | InterruptedException
          | IOException e) {
        process.destroy();
        throw new RuntimeException(e);
      }
    } else {
      /*
       * Not sure if this bug exists on Windows or not (may cause test cases to fail, if it does,
       * because we wouldn't be able to read all of the processes' output after they're stopped). If
       * it does, we could follow up on the ideas here to add a similar platform-specific
       * workaround: https://stackoverflow.com/questions/140111/sending-an-arbitrary-
       * signal-in-windows.
       */
      process.destroy();
    }
  }

  /**
   * Checks if jetty has started by scanning the app server output for a specific message.
   *
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that Jetty has started, <code>
   *          false</code> if not
   */
  static boolean hasJettyStarted(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer
        .getStdoutContents()
        .toString()
        .contains(DataServerLauncherApp.LOG_MESSAGE_STARTED_JETTY);
  }

  /**
   * Checks if the war app has started by scanning the app server output for a specific message.
   *
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that the sample WAR application
   *     has started, <code>false</code> if not
   */
  static boolean hasWarStarted(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer.getStdoutContents().toString().contains(LOG_MESSAGE_WAR_STARTED);
  }

  /** Models the <code>suspend=y/n</code> option is JVM debug settings. */
  public static enum JvmDebugAttachMode {
    /** The JVM will wait at launch for a debugger to be attached, before proceeding. */
    WAIT_FOR_ATTACH,

    /**
     * The JVM will not wait at launch for a debugger to be attached; it will proceed and allow
     * debug connections when they're attempted.
     */
    ATTACH_LATER;
  }

  /** Models whether or not to enable JVM debugging. */
  public static enum JvmDebugEnableMode {
    /** Debugging will not be enabled for the JVM application at all. */
    DISABLED,

    /** Debugging will be enabled for the JVM application. */
    ENABLED;
  }

  /** Models the various JVM debug settings and options. */
  public static final class JvmDebugOptions {
    /** If debug mode is on. */
    private final JvmDebugEnableMode debugEnableMode;

    /** If debug mode is attached. */
    private final JvmDebugAttachMode debugAttachMode;

    /** The port to attach on. */
    private final Integer port;

    /**
     * Constructs a new {@link JvmDebugOptions} instance.
     *
     * @param debugEnableMode whether or not to enable debugging (must be {@link
     *     JvmDebugEnableMode#DISABLED} for this particular constructor)
     */
    public JvmDebugOptions(JvmDebugEnableMode debugEnableMode) {
      // Use the other constructor if you want debugging enabled.
      if (debugEnableMode != JvmDebugEnableMode.DISABLED) throw new IllegalArgumentException();

      this.debugEnableMode = JvmDebugEnableMode.DISABLED;
      this.debugAttachMode = null;
      this.port = null;
    }

    /**
     * Constructs a new {@link JvmDebugOptions} instance.
     *
     * @param debugEnableMode whether or not to enable debugging
     * @param debugAttachMode whether or not to wait for a debugger to be attached at JVM launch
     * @param port the port number to listen for debugger connections on
     */
    public JvmDebugOptions(
        JvmDebugEnableMode debugEnableMode, JvmDebugAttachMode debugAttachMode, Integer port) {
      this.debugEnableMode = debugEnableMode;
      this.debugAttachMode = debugAttachMode;
      this.port = port;

      if (debugEnableMode == JvmDebugEnableMode.ENABLED && (port == null) || port < 1)
        throw new IllegalArgumentException();
    }

    /**
     * Builds the jvm options.
     *
     * @return the JVM launch options represented by this {@link JvmDebugOptions}' settings
     */
    public String[] buildJvmOptions() {
      if (debugEnableMode == JvmDebugEnableMode.DISABLED) return new String[] {};

      return new String[] {
        "-Xdebug",
        String.format(
            "-Xrunjdwp:transport=dt_socket,server=y,suspend=%s,address=%d",
            this.debugAttachMode == JvmDebugAttachMode.WAIT_FOR_ATTACH ? "y" : "n", port)
      };
    }
  }
}
