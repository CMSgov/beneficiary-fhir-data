package gov.cms.bfd.server.launcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Test;

/**
 * Integration tests for {@link DataServerLauncherApp}.
 *
 * <p>These tests require the application launcher capsule and WAR to be built and available in the
 * local projects' <code>target/</code> directories. Accordingly, they may not run correctly in
 * Eclipse: if the binaries aren't built yet, they'll just fail, but if older binaries exist
 * (because you haven't rebuilt them), it'll run using the old code, which probably isn't what you
 * want.
 */
public final class DataServerLauncherAppIT {
  /** The POSIX signal number for the <code>SIGTERM</code> signal. */
  private static final int SIGTERM = 15;

  private static final String LOG_MESSAGE_WAR_STARTED = "Johnny 5 is alive on SLF4J!";

  /**
   * Verifies that {@link DataServerLauncherApp} exits as expected when launched with no
   * configuration environment variables.
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void missingConfig() throws IOException, InterruptedException {
    // Start the app with no config env vars.
    ProcessBuilder appRunBuilder = createAppProcessBuilder();
    appRunBuilder.environment().clear();
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    // Read the app's output.
    ProcessOutputConsumer appRunConsumer = new ProcessOutputConsumer(appProcess);
    Thread appRunConsumerThread = new Thread(appRunConsumer);
    appRunConsumerThread.start();

    // Wait for it to exit with an error.
    appProcess.waitFor(1, TimeUnit.MINUTES);
    appRunConsumerThread.join();

    // Verify that the application exited as expected.
    Assert.assertEquals(DataServerLauncherApp.EXIT_CODE_BAD_CONFIG, appProcess.exitValue());
  }

  /**
   * Verifies that {@link DataServerLauncherApp} starts up as expected when properly configured
   *
   * @throws IOException (indicates a test error)
   * @throws InterruptedException (indicates a test error)
   */
  @Test
  public void normalUsage() throws IOException, InterruptedException {
    skipOnUnsupportedOs();

    Process appProcess = null;
    try {
      // Start the app.
      ProcessBuilder appRunBuilder = createAppProcessBuilder();
      appRunBuilder.redirectErrorStream(true);
      appProcess = appRunBuilder.start();

      // Read the app's output.
      ProcessOutputConsumer appRunConsumer =
          new ProcessOutputConsumer(appProcess, Optional.of(System.out));
      Thread appRunConsumerThread = new Thread(appRunConsumer);
      appRunConsumerThread.start();

      // Wait for it to start both Jetty and the app.
      try {
        Awaitility.await()
            .atMost(Duration.TEN_SECONDS)
            .until(() -> hasJettyStarted(appRunConsumer));
        Awaitility.await().atMost(Duration.TEN_SECONDS).until(() -> hasWarStarted(appRunConsumer));
      } catch (ConditionTimeoutException e) {
        // Add some additional logging detail.
        throw new ConditionTimeoutException(
            "App server output did not indicate successful startup within timeout. Output:\n"
                + appRunConsumer.getStdoutContents());
      }

      // Stop the application.
      sendSigterm(appProcess);
      appProcess.waitFor(1, TimeUnit.MINUTES);
      appRunConsumerThread.join();

      // Verify that the application exited as expected.
      verifyExitValueMatchesSignal(SIGTERM, appProcess);
      Assert.assertTrue(
          "Application's housekeeping shutdown hook did not run.",
          appRunConsumer
              .getStdoutContents()
              .toString()
              .contains(DataServerLauncherApp.LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE));
    } finally {
      if (appProcess != null) appProcess.destroyForcibly();
    }
  }

  /**
   * Throws an {@link AssumptionViolatedException} if the OS doesn't support
   * <strong>graceful</strong> shutdowns via {@link Process#destroy()}.
   */
  private static void skipOnUnsupportedOs() {
    /*
     * The only OS I know for sure that handles this correctly is Linux, because I've verified that
     * there. However, the following project seems to indicate that Linux really might be it:
     * https://github.com/zeroturnaround/zt-process-killer. Some further research indicates that
     * this could be supported on Windows for GUI apps, but not console apps. If this lack of OS
     * support ever proves to be a problem, the best thing to do would be to enhance our application
     * such that it listens on a particular port for shutdown requests, and handles them gracefully.
     */

    Assume.assumeTrue(
        "Unsupported OS for this test case.",
        Arrays.asList("Linux", "Mac OS X").contains(System.getProperty("os.name")));
  }

  /**
   * @param appRunConsumer the {@link ProcessOutputConsumer} whose output should be checked
   * @return <code>true</code> if the application output indicates that Jetty has started, <code>
   *     false</code> if not
   */
  private static boolean hasJettyStarted(ProcessOutputConsumer appRunConsumer) {
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
  private static boolean hasWarStarted(ProcessOutputConsumer appRunConsumer) {
    return appRunConsumer.getStdoutContents().toString().contains(LOG_MESSAGE_WAR_STARTED);
  }

  /**
   * Sends a <code>SIGTERM</code> to the specified {@link Process}, causing it to exit, but giving
   * it a chance to do so gracefully.
   *
   * @param process the {@link Process} to signal
   */
  private static void sendSigterm(Process process) {
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
   * Verifies that the specified {@link Process} has exited, due to the specified signal.
   *
   * @param signalNumber the POSIX signal number to check for
   * @param process the {@link Process} to check the {@link Process#exitValue()} of
   */
  private static void verifyExitValueMatchesSignal(int signalNumber, Process process) {
    /*
     * Per POSIX (by way of http://unix.stackexchange.com/a/99143), applications that exit due to a
     * signal should return an exit code that is 128 + the signal number.
     */
    Assert.assertEquals(128 + signalNumber, process.exitValue());
  }

  /** @return a {@link ProcessBuilder} that can be used to launch the application */
  private static ProcessBuilder createAppProcessBuilder() {
    String[] command = createCommandForCapsule();
    ProcessBuilder appRunBuilder = new ProcessBuilder(command);
    appRunBuilder.redirectErrorStream(true);

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
    appRunBuilder
        .environment()
        .put(
            AppConfiguration.ENV_VAR_KEY_WAR,
            AppConfigurationIT.getProjectDirectory()
                .resolve(
                    Paths.get("target", "sample", "bfd-server-launcher-sample-1.0.0-SNAPSHOT.war"))
                .toString());

    return appRunBuilder;
  }

  /**
   * @return the command array for {@link ProcessBuilder#ProcessBuilder(String...)} that will launch
   *     the application via its <code>.x</code> capsule executable
   */
  private static String[] createCommandForCapsule() {
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

      return new String[] {javaBin.toString(), "-jar", appJar.toAbsolutePath().toString()};
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Managing external processes is tricky: at the OS level, all processes' output is sent to a
   * buffer. If that buffer fills up (because you're not reading the output), the process will block
   * -- forever. To avoid that, it's best to always have a separate thread running that consumes a
   * process' output. This {@link ProcessOutputConsumer} is designed to allow for just that.
   */
  private static final class ProcessOutputConsumer implements Runnable {
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
        e.printStackTrace();
        throw new UncheckedIOException(e);
      }
    }

    /** @return a {@link StringBuffer} that contains the <code>STDOUT</code> contents so far */
    public StringBuffer getStdoutContents() {
      return stdoutContents;
    }
  }
}
