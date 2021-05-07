package gov.cms.bfd.server.launcher;

import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugAttachMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugEnableMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
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
    ProcessBuilder appRunBuilder =
        ServerProcess.createAppProcessBuilder(
            ServerTestUtils.getSampleWar(), new JvmDebugOptions(JvmDebugEnableMode.DISABLED));
    appRunBuilder.environment().clear();
    appRunBuilder.redirectErrorStream(true);
    Process appProcess = appRunBuilder.start();

    // Read the app's output.
    ServerProcess.ProcessOutputConsumer appRunConsumer =
        new ServerProcess.ProcessOutputConsumer(appProcess);
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
   * @throws InterruptedException
   */
  @Test
  public void normalUsage() throws IOException, InterruptedException {
    ServerProcess serverProcess = null;
    try {
      // Launch the server.
      serverProcess =
          new ServerProcess(
              ServerTestUtils.getSampleWar(),
              new JvmDebugOptions(
                  JvmDebugEnableMode.DISABLED, JvmDebugAttachMode.WAIT_FOR_ATTACH, 8000));

      // Verify that a request works.
      try (CloseableHttpClient httpClient =
              ServerTestUtils.createHttpClient(Optional.of(ClientSslIdentity.TRUSTED));
          CloseableHttpResponse httpResponse =
              httpClient.execute(new HttpGet(serverProcess.getServerUri())); ) {
        Assert.assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        String httpResponseContent = EntityUtils.toString(httpResponse.getEntity());
        Assert.assertEquals("Johnny 5 is alive on HTTP!", httpResponseContent);
      }

      // Verify that the access log is working, as expected.
      try {
        TimeUnit.MILLISECONDS.sleep(
            100); // Needed in some configurations to resolve a race condition
      } catch (InterruptedException e) {
      }
      Path accessLog =
          ServerTestUtils.getLauncherProjectDirectory()
              .resolve("target")
              .resolve("server-work")
              .resolve("access.log");
      Assert.assertTrue(Files.isReadable(accessLog));
      Assert.assertTrue(Files.size(accessLog) > 0);
      Path accessLogJson =
          ServerTestUtils.getLauncherProjectDirectory()
              .resolve("target")
              .resolve("server-work")
              .resolve("access.json");
      Assert.assertTrue(Files.isReadable(accessLogJson));
      Assert.assertTrue(Files.size(accessLogJson) > 0);

      // Stop the application.
      serverProcess.close();

      /*
       * Verify that the application exited as expected. Per POSIX (by way of
       * http://unix.stackexchange.com/a/99143), applications that exit due to a signal should
       * return an exit code that is 128 + the signal number.
       */
      Assert.assertEquals(128 + SIGTERM, (int) serverProcess.getResultCode().get());
      Assert.assertTrue(
          "Application's housekeeping shutdown hook did not run: "
              + serverProcess.getProcessOutput(),
          serverProcess
              .getProcessOutput()
              .contains(DataServerLauncherApp.LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE));
    } finally {
      if (serverProcess != null) serverProcess.close();
    }
  }
}
