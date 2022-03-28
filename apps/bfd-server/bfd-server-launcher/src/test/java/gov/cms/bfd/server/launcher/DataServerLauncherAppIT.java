package gov.cms.bfd.server.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugAttachMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugEnableMode;
import gov.cms.bfd.server.launcher.ServerProcess.JvmDebugOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DataServerLauncherApp}.
 *
 * <p>These tests require the application launcher assembly and WAR to be built and available in the
 * local projects' <code>target/</code> directories. Accordingly, they may not run correctly in
 * Eclipse: if the binaries aren't built yet, they'll just fail, but if older binaries exist
 * (because you haven't rebuilt them), it'll run using the old code, which probably isn't what you
 * want.
 */
public final class DataServerLauncherAppIT {
  /** The POSIX signal number for the <code>SIGTERM</code> signal. */
  private static final int SIGTERM = 15;
  /** Regex for access log entries */
  private static final String accessLogPattern =
      new StringJoiner(" ")
          .add("^(\\S+)") // Address or Hostname
          .add("\\S+") // Dash symbol (-) separator
          .add("\"([^\"]*)\"") // Remote user info
          .add("\\[([^\\]]+)\\]") // Request timestamp
          .add("\"([A-Z]+) ([^ \"]+) HTTP\\/[0-9.]+\"") // First line of request
          .add("\"([^ \"]+)\"") // Request query string
          .add("([0-9]{3})") // Response status
          .add("([0-9]+|-)") // Bytes transferred
          .add("([0-9]+|-)") // Time taken to serve request
          .add("(\\S+)") // BlueButton-OriginalQueryId
          .add("([0-9]+|-)") // BlueButton-OriginalQueryCounter
          .add("\\[([^\\]]+)\\]") // BlueButton-OriginalQueryTimestamp
          .add("([0-9]+|-)") // BlueButton-DeveloperId
          .add("\"([^\"]*)\"") // BlueButton-Developer
          .add("([0-9]+|-)") // BlueButton-ApplicationId
          .add("\"([^\"]*)\"") // BlueButton-Application
          .add("([0-9]+|-)") // BlueButton-UserId
          .add("\"([^\"]*)\"") // BlueButton-User
          .add("(\\S+)") // BlueButton-BeneficiaryId
          .toString();

  /**
   * Verifies the regex for valdiating our access log entries adequately avoids edge cases that
   * could break our alerts, which depend on logs
   */
  @Test
  public void checkAccessLogFormat() {
    // Access log entry for local environment
    String goodLine1 =
        new StringJoiner(" ")
            .add("127.0.0.1")
            .add("-")
            .add("\"CN=client-local-dev\"")
            .add("[07/Mar/2022:18:43:15 +0000]")
            .add("\"GET / HTTP/1.1\"")
            .add("\"?null\"")
            .add("200 26 22000")
            .add("- - [-] -")
            .add("\"-\" - \"-\" - \"-\" -\"")
            .toString();

    // Access log entry for test/prod sbx environments
    String goodline2 =
        new StringJoiner(" ")
            .add("10.252.14.216")
            .add("-")
            .add(
                "\"EMAILADDRESS=gomer.pyle@adhocteam.us, CN=BlueButton Root CA, OU=BlueButton on FHIR API Root CA, O=Centers for Medicare and Medicaid Services, L=Baltimore, ST=Maryland, C=US\"")
            .add("[01/Oct/2021:23:10:01 -0400]")
            .add(
                "\"GET /v1/fhir/Coverage/?startIndex=0&_count=10&_format=application%2Fjson%2Bfhir&beneficiary=Patient%2F587940319 HTTP/1.1\"")
            .add(
                "\"?startIndex=0&_count=10&_format=application%2Fjson%2Bfhir&beneficiary=Patient%2F587940319\"")
            .add("200 2103 23")
            .add("3b3e2b30-232f-11ec-9b9f-0a006c0cb407 1 [2021-10-02 03:10:01.104125] 11770")
            .add("\"-\" 32 \"Evidation on behalf of Heartline\" 79696 \"-\" patientId:587940319")
            .toString();

    // Invalid log entry with request timestamp enclosed by double brackets
    String badLine =
        new StringJoiner(" ")
            .add("127.0.0.1")
            .add("-")
            .add("\"CN=client-local-dev\"")
            .add("[[07/Mar/2022:18:43:15 +0000]]")
            .add("\"GET / HTTP/1.1\"")
            .add("\"?null\"")
            .add("200 26 22000")
            .add("- - [-] -")
            .add("\"-\" - \"-\" - \"-\" -\"")
            .toString();

    // Invalid log entry with HTTP status code having more than 3 digits
    String badLine2 =
        new StringJoiner(" ")
            .add("127.0.0.1")
            .add("-")
            .add("\"CN=client-local-dev\"")
            .add("[07/Mar/2022:18:43:15 +0000]")
            .add("\"GET / HTTP/1.1\"")
            .add("\"?null\"")
            .add("2004 26 22000")
            .add("- - [-] -")
            .add("\"-\" - \"-\" - \"-\" -\"")
            .toString();

    Pattern p = Pattern.compile(accessLogPattern);

    assertTrue(p.matcher(goodLine1).matches());
    assertTrue(p.matcher(goodline2).matches());
    assertFalse(p.matcher(badLine).matches());
    assertFalse(p.matcher(badLine2).matches());
  }

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
    String javaHome = System.getenv("JAVA_HOME");
    appRunBuilder.environment().clear();
    appRunBuilder.environment().put("JAVA_HOME", javaHome);
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
    assertEquals(DataServerLauncherApp.EXIT_CODE_BAD_CONFIG, appProcess.exitValue());
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
        assertEquals(200, httpResponse.getStatusLine().getStatusCode());

        String httpResponseContent = EntityUtils.toString(httpResponse.getEntity());
        assertEquals("Johnny 5 is alive on HTTP!", httpResponseContent);
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
      assertTrue(Files.isReadable(accessLog));
      assertTrue(Files.size(accessLog) > 0);

      // Check that the access log lines follow the desired regex pattern
      List<String> lines = Files.readAllLines(accessLog);

      Pattern p = Pattern.compile(accessLogPattern);

      lines.forEach(
          (line) -> {
            Matcher m = p.matcher(line);
            assertTrue(m.matches());
          });

      Path accessLogJson =
          ServerTestUtils.getLauncherProjectDirectory()
              .resolve("target")
              .resolve("server-work")
              .resolve("access.json");
      assertTrue(Files.isReadable(accessLogJson));
      assertTrue(Files.size(accessLogJson) > 0);

      // Stop the application.
      serverProcess.close();

      /*
       * Verify that the application exited as expected. Per POSIX (by way of
       * http://unix.stackexchange.com/a/99143), applications that exit due to a signal should
       * return an exit code that is 128 + the signal number.
       */
      assertEquals(128 + SIGTERM, (int) serverProcess.getResultCode().get());
      assertTrue(
          serverProcess
              .getProcessOutput()
              .contains(DataServerLauncherApp.LOG_MESSAGE_SHUTDOWN_HOOK_COMPLETE),
          "Application's housekeeping shutdown hook did not run: "
              + serverProcess.getProcessOutput());
    } finally {
      if (serverProcess != null) serverProcess.close();
    }
  }
}
