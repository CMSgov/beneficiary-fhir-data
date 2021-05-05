package gov.cms.bfd.server.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Represents a BFD Server Launcher App process, for use in ITs. */
public final class ServerProcess implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerProcess.class);

  private static final String LOG_MESSAGE_WAR_STARTED = "Johnny 5 is alive on SLF4J!";

  private Process appProcess;
  private ProcessOutputConsumer appRunConsumer;
  private Thread appRunConsumerThread;
  private URI serverUri;
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
    this.appRunConsumer = new ProcessOutputConsumer(appProcess, Optional.empty());
    this.appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Wait for it to start both Jetty and the app.
    try {
      Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> hasJettyStarted(appRunConsumer));
      Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> hasWarStarted(appRunConsumer));
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
    Assert.assertTrue(
        String.format(
            "Unable to find server start message (/%s/) in log:\n%s",
            serverUriPattern.pattern(), logText),
        serverUriMatcher.find());
    try {
      this.serverUri = new URI(serverUriMatcher.group(1));
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Unable to parse server URI: " + serverUriMatcher.group(1));
    }
  }

  /** @return the local {@link URI} that the server is accessible at */
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

  /** @see java.lang.AutoCloseable#close() */
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
   * @return the server process' result code if it's stopped, or {@link Optional#empty()} if it has
   *     not
   */
  public Optional<Integer> getResultCode() {
    return exitValue;
  }

  /**
   * @param warPath the {@link Path} to the WAR file to run with the server
   * @param jvmDebugOptions the {@link JvmDebugOptions} to use
   * @return a {@link ProcessBuilder} that can be used to launch the application
   */
  static ProcessBuilder createAppProcessBuilder(Path warPath, JvmDebugOptions jvmDebugOptions) {
    String[] command = createCommandForCapsule(jvmDebugOptions);
    LOGGER.debug("About to launch server with command: {}", Arrays.toString(command));
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_HOST, "127.0.0.1");
    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_PORT, "0");
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_KEYSTORE,
            AppConfigurationIT.getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-keystore.jks"))
                .toString());
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_TRUSTSTORE,
            AppConfigurationIT.getProjectDirectory()
                .resolve(Paths.get("..", "dev", "ssl-stores", "server-truststore.jks"))
                .toString());
    appRunBuilder.environment().put(AppConfiguration.ENV_VAR_KEY_WAR, warPath.toString());

    return appRunBuilder;
  }

  /**
   * @param jvmDebugOptions the {@link JvmDebugOptions} to use
   * @return the command array for {@link ProcessBuilder#ProcessBuilder(String...)} that will launch
   *     the application via its <code>.x</code> capsule executable
   */
  static String[] createCommandForCapsule(JvmDebugOptions jvmDebugOptions) {
    try {
      Path javaBinDir = Paths.get(System.getProperty("java.home")).resolve("bin");
      Path javaBin = javaBinDir.resolve("java");

      Path buildTargetDir = Paths.get(".", "target");
      Path appJar =
          Files.list(buildTargetDir)
              .filter(f -> f.getFileName().toString().startsWith("bfd-server-launcher-"))
              .filter(f -> f.getFileName().toString().endsWith("-capsule-fat.jar"))
              .findFirst()
              .get();

      List<List<String>> commandTokens =
          Arrays.asList(
              Arrays.asList(javaBin.toString()),
              Arrays.asList(jvmDebugOptions.buildJvmOptions()),
              Arrays.asList("-jar", appJar.toAbsolutePath().toString()));
      return commandTokens.stream().flatMap(List::stream).toArray(String[]::new);
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
    /*
     * We have to use reflection and external commands here to work around this ridiculous JDK bug:
     * https://bugs.openjdk.java.net/browse/JDK-5101298.
     */
    if (process.getClass().getName().equals("java.lang.UNIXProcess")) {
      try {
        Field pidField = process.getClass().getDeclaredField("pid");
        pidField.setAccessible(true);

        int processPid = pidField.getInt(process);

        ProcessBuilder killBuilder = new ProcessBuilder("/bin/kill", "-s", "TERM", "" + processPid);
        int killBuilderExitCode = killBuilder.start().waitFor();
        if (killBuilderExitCode != 0) process.destroy();
      } catch (NoSuchFieldException
          | SecurityException
          | IllegalArgumentException
          | IllegalAccessException
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
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that Jetty has started, <code>
   *     false</code> if not
   */
  static boolean hasJettyStarted(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer
        .getStdoutContents()
        .toString()
        .contains(DataServerLauncherApp.LOG_MESSAGE_STARTED_JETTY);
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that the sample WAR application
   *     has started, <code>false</code> if not
   */
  static boolean hasWarStarted(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer.getStdoutContents().toString().contains(LOG_MESSAGE_WAR_STARTED);
  }

  /**
   * Managing external processes is tricky: at the OS level, all processes' output is sent to a
   * buffer. If that buffer fills up (because you're not reading the output), the process will block
   * -- forever. To avoid that, it's best to always have a separate thread running that consumes a
   * process' output. This {@link ProcessOutputConsumer} is designed to allow for just that.
   */
  public static final class ProcessOutputConsumer implements Runnable {
    private final BufferedReader stdoutReader;
    private final StringBuffer stdoutContents;
    private final Optional<PrintStream> teeTarget;

    /**
     * Constructs a new {@link ProcessOutputConsumer} instance.
     *
     * @param process the {@link ProcessOutputConsumer} whose output should be consumed
     */
    public ProcessOutputConsumer(Process process) {
      this(process, Optional.empty());
    }

    /**
     * Constructs a new {@link ProcessOutputConsumer} instance.
     *
     * @param process the {@link ProcessOutputConsumer} whose output should be consumed
     * @param out
     */
    public ProcessOutputConsumer(Process process, Optional<PrintStream> teeTarget) {
      /*
       * Note: we're only grabbing STDOUT, because we're assuming that STDERR has been piped
       * to/merged with it. If that's not the case, you'd need a separate thread consuming that
       * stream, too.
       */

      InputStream stdout = process.getInputStream();
      this.stdoutReader = new BufferedReader(new InputStreamReader(stdout));
      this.stdoutContents = new StringBuffer();
      this.teeTarget = teeTarget;
    }

    /** @see java.lang.Runnable#run() */
    @Override
    public void run() {
      /*
       * Note: This will naturally stop once the process exits (due to the null check below).
       */

      try {
        String line;
        while ((line = stdoutReader.readLine()) != null) {
          stdoutContents.append(line);
          stdoutContents.append('\n');

          if (teeTarget.isPresent()) teeTarget.get().println(line);
        }
      } catch (IOException e) {
        LOGGER.warn("Error reading server process output.", e);
        throw new UncheckedIOException(e);
      }
    }

    /** @return a {@link StringBuffer} that contains the <code>STDOUT</code> contents so far */
    public StringBuffer getStdoutContents() {
      return stdoutContents;
    }
  }

  /** Models the <code>suspend=<y/n></code> option is JVM debug settings. */
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
    private final JvmDebugEnableMode debugEnableMode;
    private final JvmDebugAttachMode debugAttachMode;
    private final Integer port;

    /**
     * Constructs a new {@link JvmDebugOptions} instance.
     *
     * @param debugEnableMode whether or not to enable debugging (must be
     *        {@link JvmDebugEnableMode#DISABLED for this particular constructor)
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

    /** @return the JVM launch options represented by this {@link JvmDebugOptions}' settings */
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
