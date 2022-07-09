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
          .add("(\\S+)") // Response Header X-Request-ID
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
            .add("LEyn5pEykWydcbZR")
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
            .add("LEyn5pEykWydcbZR")
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
            .add("LEyn5pEykWydcbZR")
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
            .add("LEyn5pEykWydcbZR")
            .toString();

    Pattern p = Pattern.compile(accessLogPattern);

    assertTrue(p.matcher(goodLine1).matches());
    assertTrue(p.matcher(goodline2).matches());
    assertFalse(p.matcher(badLine).matches());
    assertFalse(p.matcher(badLine2).matches());
  }

  /**
   * Creates a regex pattern for all resource provider methods to ensure bene_id is logged in mdc in
   * our access json entries
   */
  public String createBeneIdMdcPattern(String endpoint, String query, Boolean isQuery) {
    // Duration that distinguishes various search and read methods
    String duration =
        (isQuery) ? ".*(\\{\\\"jpa_query)" : ".*(http_access.response.duration_milliseconds)";

    String mdcPattern =
        new StringBuilder(duration)
            .append(".*((/[a-z]+[1-2]{1}/[a-z]+/" + endpoint + "))") // API URL
            .append(".*((by=" + query + "))") // Query parameter
            .append(".*((bene_id\\\":\\\"567834\\\"))") // mdc bene_id
            .append(",\\\".*") // remaining log
            .toString();

    return mdcPattern;
  };

  /**
   * Verifies the regex for valdiating our access json entries adequately logs bene_id for all
   * resource provider read and search methods
   */
  @Test
  public void checkBeneIdMdcEntries() {
    // Access json entries for each endpoint
    String coverageV1Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:22.563+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-27\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"3494646\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"5\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:22GMT\",")
            .append("\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"3\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"http_access.request.operation\":\"/v1/fhir/Coverage(by=id)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"NhpWcq15oZOao8kn\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v1/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:22GMT\",\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v1/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.query_string\":null,\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"737\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String coverageV2Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:05.499+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-16\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"5020959\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"14\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:05GMT\",")
            .append("\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"5\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"http_access.request.operation\":\"/v2/fhir/Coverage(by=id)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"9TImlX9Utn7G17mV\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:05GMT\",\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.query_string\":\"_format=json\",\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"926\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String coverageV3Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:05.499+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-16\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"5020959\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"14\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:05GMT\",")
            .append("\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"5\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"http_access.request.operation\":\"/v3/fhir/Coverage(by=id)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"9TImlX9Utn7G17mV\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:05GMT\",\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/Coverage/part-a-567834\",")
            .append(
                "\"http_access.request.query_string\":\"_format=json\",\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"926\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String eobV1Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:01.234+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-28\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"8\",")
            .append(
                "\"jpa_query.eob_by_id.duration_nanoseconds\":\"3224257\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:01GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"http_access.request.operation\":\"/v1/fhir/ExplanationOfBenefit(IncludeTaxNumbers=false,by=id)\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"GCDiA1WmOavIJqC5\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v1/fhir/ExplanationOfBenefit/carrier-9991831999\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:01GMT\",\"jpa_query.eob_by_id.record_count\":\"1\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v1/fhir/ExplanationOfBenefit/carrier-9991831999\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v1/fhir/ExplanationOfBenefit/carrier-9991831999\",")
            .append(
                "\"http_access.request.query_string\":\"_format=json\",\"jpa_query.eob_by_id.duration_milliseconds\":\"3\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"2365\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String eobV2Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:09.728+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-18\",")
            .append(
                "\"mdc\":{\"http_access.response.duration_milliseconds\":\"9\",\"jpa_query.eob_by_id.duration_nanoseconds\":\"3370875\",")
            .append(
                "\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:09GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"http_access.request.operation\":\"/v2/fhir/ExplanationOfBenefit(IncludeTaxNumbers=false,by=id)\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"eq8vutI701HtzSBV\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v2/fhir/ExplanationOfBenefit/snf-777777777\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:09GMT\",\"jpa_query.eob_by_id.record_count\":\"1\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v2/fhir/ExplanationOfBenefit/snf-777777777\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/ExplanationOfBenefit/snf-777777777\",")
            .append(
                "\"http_access.request.query_string\":\"_format=json\",\"jpa_query.eob_by_id.duration_milliseconds\":\"3\",")
            .append(
                "\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"3103\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV1Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:26.097+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-21\",")
            .append(
                "\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"2399520\",\"http_access.response.duration_milliseconds\":\"4\",")
            .append(
                "\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append(
                "\"http_access.response.header.Date\":\"Fri,08Jul202200:02:26GMT\",\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"2\",\"")
            .append(
                "http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/Patient(IncludeAddressFields=true,IncludeIdentifiers=(),IncludeTaxNumbers=false,by=id)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"CnqhZwTUg7pWnE0b\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v1/fhir/Patient/567834\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"http_access.request.header.IncludeIdentifiers\":\"[]\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:25GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Patient/567834\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.header.IncludeTaxNumbers\":\"false\",")
            .append(
                "\"http_access.request.uri\":\"/v1/fhir/Patient/567834\",\"http_access.request.query_string\":null,")
            .append("\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"700\",")
            .append(
                "\"http_access.request.header.IncludeAddressFields\":\"true\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV2Read =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:37.189+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-16\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"2482045\",")
            .append("\"http_access.response.duration_milliseconds\":\"4\",")
            .append(
                "\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append(
                "\"http_access.response.header.Date\":\"Fri,08Jul202200:02:37GMT\",\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"2\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/Patient(IncludeAddressFields=false,IncludeIdentifiers=(),IncludeTaxNumbers=false,_lastUpdated=false,by=id)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append(
                "\"http_access.response.header.X-Request-ID\":\"RZBMq0uOloNyaG4a\",\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:37GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Patient\",\"http_access.request.header.Connection\":\"keep-alive\",")
            .append(
                "\"http_access.request.uri\":\"/v2/fhir/Patient\",\"http_access.request.query_string\":\"_id=%7C567834&_count=1\",")
            .append("\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"966\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String coverageV1Search =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:22.762+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-18\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"3025850\",")
            .append("\"http_access.response.duration_milliseconds\":\"6\",")
            .append("\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:22GMT\",")
            .append("\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"3\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/Coverage(_lastUpdated=false,by=beneficiary,pageSize=*)\",")
            .append("\"jpa_query.bene_by_id.include_.record_count\":\"1\",")
            .append("\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"QTqbv4jyJ3WsjmEV\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append("\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:22GMT\",")
            .append("\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Coverage\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v1/fhir/Coverage\",")
            .append(
                "\"http_access.request.query_string\":\"beneficiary=Patient%2F567834\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"2218\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String coverageV2Search =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:05.740+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-23\",")
            .append("\"mdc\":{\"jpa_query.bene_by_id.include_.duration_nanoseconds\":\"5254609\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"20\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:05GMT\",")
            .append("\"jpa_query.bene_by_id.include_.duration_milliseconds\":\"5\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/Coverage(_lastUpdated=false,by=beneficiary,pageSize=*)\",")
            .append(
                "\"jpa_query.bene_by_id.include_.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"m6FtOvlFcX6oDcxI\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:05GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append("\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Coverage\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/Coverage\",")
            .append(
                "\"http_access.request.query_string\":\"beneficiary=Patient%2F567834&_format=json\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"2439\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String eobV1Search =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:00.614+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-17\",")
            .append(
                "\"mdc\":{\"http_access.response.duration_milliseconds\":\"1339\",\"jpa_query.eobs_by_bene_id.snf.duration_milliseconds\":\"5\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_nanoseconds\":\"4810782\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.carrier.record_count\":\"1\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:01:59GMT\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_nanoseconds\":\"4333045\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+json;q=1.0,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds\":\"6607269\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds\":\"4783818\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.hospice.record_count\":\"1\",\"jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds\":\"6\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_milliseconds\":\"4\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/ExplanationOfBenefit(IncludeTaxNumbers=false,_lastUpdated=false,by=patient,pageSize=*,service-date=false,types=*)\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.inpatient.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.outpatient.record_count\":\"1\",\"http_access.response.header.X-Request-ID\":\"R2eGwAqzDsJb3SIE\",")
            .append("\"jpa_query.eobs_by_bene_id.pde.duration_nanoseconds\":\"3083980\",")
            .append("\"jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds\":\"6107201\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"jpa_query.eobs_by_bene_id.dme.record_count\":\"1\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.pde.duration_milliseconds\":\"3\",\"jpa_query.eobs_by_bene_id.carrier.duration_milliseconds\":\"6\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.duration_nanoseconds\":\"5676370\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:01:59GMT\",")
            .append("\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds\":\"6997826\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v1/fhir/ExplanationOfBenefit\",\"jpa_query.eobs_by_bene_id.snf.record_count\":\"1\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v1/fhir/ExplanationOfBenefit\",")
            .append(
                "\"http_access.request.query_string\":\"patient=Patient%2F567834&_format=json\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.pde.record_count\":\"1\",\"jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds\":\"6\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.hha.record_count\":\"1\",\"jpa_query.eobs_by_bene_id.hospice.duration_milliseconds\":\"4\",")
            .append(
                "\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"10474\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_milliseconds\":\"4\",")
            .append("\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String eobV2Search =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:28.439+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-16\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"37\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.duration_milliseconds\":\"2\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_nanoseconds\":\"1162095\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.record_count\":\"1\",")
            .append("\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append("\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:28GMT\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_nanoseconds\":\"1532328\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append("\"jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds\":\"3492289\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds\":\"1563536\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds\":\"3\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_milliseconds\":\"1\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/ExplanationOfBenefit(IncludeTaxNumbers=false,_lastUpdated=false,by=patient,pageSize=*,service-date=false,types=*)\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.inpatient.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"jpa_query.eobs_by_bene_id.outpatient.record_count\":\"1\",")
            .append("\"http_access.response.header.X-Request-ID\":\"HUTCrpQ5fMGKVRUl\",")
            .append("\"jpa_query.eobs_by_bene_id.pde.duration_nanoseconds\":\"735822\",")
            .append("\"jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds\":\"2451192\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"jpa_query.eobs_by_bene_id.dme.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.pde.duration_milliseconds\":\"0\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.duration_milliseconds\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.duration_nanoseconds\":\"2393982\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:28GMT\",")
            .append("\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds\":\"1524739\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v2/fhir/ExplanationOfBenefit\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.record_count\":\"1\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/ExplanationOfBenefit\",")
            .append(
                "\"http_access.request.query_string\":\"patient=Patient%2F567834\",\"jpa_query.eobs_by_bene_id.pde.record_count\":\"1\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds\":\"2\",\"jpa_query.eobs_by_bene_id.hha.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.duration_milliseconds\":\"1\",")
            .append(
                "\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"13222\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_milliseconds\":\"1\",")
            .append("\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String eobV3Search =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:28.439+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-16\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"37\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.duration_milliseconds\":\"2\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_nanoseconds\":\"1162095\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.record_count\":\"1\",")
            .append("\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append("\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:28GMT\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_nanoseconds\":\"1532328\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append("\"jpa_query.eobs_by_bene_id.inpatient.duration_nanoseconds\":\"3492289\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.duration_nanoseconds\":\"1563536\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.inpatient.duration_milliseconds\":\"3\",")
            .append("\"jpa_query.eobs_by_bene_id.hha.duration_milliseconds\":\"1\",")
            .append(
                "\"http_access.request.operation\":\"/v3/fhir/ExplanationOfBenefit(IncludeTaxNumbers=false,_lastUpdated=false,by=patient,pageSize=*,service-date=false,types=*)\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.inpatient.record_count\":\"1\",\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"jpa_query.eobs_by_bene_id.outpatient.record_count\":\"1\",")
            .append("\"http_access.response.header.X-Request-ID\":\"HUTCrpQ5fMGKVRUl\",")
            .append("\"jpa_query.eobs_by_bene_id.pde.duration_nanoseconds\":\"735822\",")
            .append("\"jpa_query.eobs_by_bene_id.outpatient.duration_nanoseconds\":\"2451192\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"jpa_query.eobs_by_bene_id.dme.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.pde.duration_milliseconds\":\"0\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.duration_milliseconds\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.duration_nanoseconds\":\"2393982\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:28GMT\",")
            .append("\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append("\"jpa_query.eobs_by_bene_id.carrier.duration_nanoseconds\":\"1524739\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v2/fhir/ExplanationOfBenefit\",")
            .append("\"jpa_query.eobs_by_bene_id.snf.record_count\":\"1\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v2/fhir/ExplanationOfBenefit\",")
            .append(
                "\"http_access.request.query_string\":\"patient=Patient%2F567834\",\"jpa_query.eobs_by_bene_id.pde.record_count\":\"1\",")
            .append(
                "\"jpa_query.eobs_by_bene_id.outpatient.duration_milliseconds\":\"2\",\"jpa_query.eobs_by_bene_id.hha.record_count\":\"1\",")
            .append("\"jpa_query.eobs_by_bene_id.hospice.duration_milliseconds\":\"1\",")
            .append(
                "\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"13222\",")
            .append("\"jpa_query.eobs_by_bene_id.dme.duration_milliseconds\":\"1\",")
            .append("\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV1SearchById =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:25.640+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-18\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"4\",")
            .append("\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append("\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:25GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/Patient(IncludeAddressFields=true,IncludeIdentifiers=(mbi),IncludeTaxNumbers=false,by=id)\",")
            .append("\"http_access.response.header.Content-Encoding\":\"gzip\",")
            .append("\"http_access.response.header.X-Request-ID\":\"NSU4Sy9GpNR7GuIA\",")
            .append(
                "\"http_access.response.header.Content-Location\":\"https://localhost:8992/v1/fhir/Patient/567834\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"http_access.request.header.IncludeIdentifiers\":\"[mbi]\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:25GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append("\"jpa_query.bene_by_id.include_mbi.duration_nanoseconds\":\"2825062\",")
            .append("\"jpa_query.bene_by_id.include_mbi.duration_milliseconds\":\"2\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Patient/567834\",")
            .append("\"http_access.request.header.Connection\":\"keep-alive\",")
            .append(
                "\"http_access.request.header.IncludeTaxNumbers\":\"false\",\"http_access.request.uri\":\"/v1/fhir/Patient/567834\",")
            .append("\"http_access.request.query_string\":null,\"bene_id\":\"567834\",")
            .append(
                "\"jpa_query.bene_by_id.include_mbi.record_count\":\"1\",\"http_access.response.output_size_in_bytes\":\"794\",")
            .append(
                "\"http_access.request.header.IncludeAddressFields\":\"true\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV1SearchByIdentifier =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:23.656+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-21\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"4\",")
            .append(
                "\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:23GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count\":\"1\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/Patient(IncludeAddressFields=false,IncludeIdentifiers=(),IncludeTaxNumbers=false,_lastUpdated=false,by=identifier)\",")
            .append(
                "\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds\":\"0\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"SCBMkXzFxAsn03yU\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append(
                "\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds\":\"748779\",")
            .append("\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count\":\"0\",")
            .append(
                "\"http_access.response.status\":\"200\",\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:23GMT\",")
            .append(
                "\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds\":\"2\",")
            .append("\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Patient\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.uri\":\"/v1/fhir/Patient\",")
            .append(
                "\"http_access.request.query_string\":\"identifier=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fidentifier%2Fmbi-hash%7C82273caf4d2c3b5a8340190ae3575950957ce469e593efd7736d60c3b39d253c\",")
            .append(
                "\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds\":\"2360121\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"770\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV1SearchByCoverageContract =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:26.843+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-21\",")
            .append(
                "\"mdc\":{\"jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds\":\"744638\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"7\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:26GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds\":\"2399404\",")
            .append("\"jpa_query.benes_by_year_month_part_d_contract_id.record_count\":\"1\",")
            .append(
                "\"http_access.request.operation\":\"/v1/fhir/Patient(IncludeAddressFields=false,IncludeIdentifiers=(mbi),IncludeTaxNumbers=false,by=coverageContractForYearMonth)\",")
            .append(
                "\"jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds\":\"2\",")
            .append("\"jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count\":\"1\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"eJwKtWixZCbufMgw\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR3.0.2/DSTU3)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds\":\"1\",")
            .append("\"http_access.request.header.IncludeIdentifiers\":\"[mbi]\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR3.0.2/DSTU3;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:26GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append("\"http_access.request.url\":\"https://localhost:8992/v1/fhir/Patient\",")
            .append(
                "\"jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds\":\"1024775\",")
            .append("\"jpa_query.bene_count_by_year_month_part_d_contract_id.record_count\":\"1\",")
            .append(
                "\"http_access.request.header.Connection\":\"keep-alive\",\"http_access.request.header.IncludeTaxNumbers\":\"false\",")
            .append("\"http_access.request.uri\":\"/v1/fhir/Patient\",")
            .append(
                "\"http_access.request.query_string\":\"_has%3ACoverage.extension=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Fptdcntrct01%7CS4607&_has%3ACoverage.rfrncyr=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Frfrnc_yr%7C2018&_count=1\",")
            .append(
                "\"jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds\":\"0\",\"bene_id\":\"567834\",")
            .append("\"http_access.response.output_size_in_bytes\":\"880\",")
            .append("\"http_access.request.header.IncludeAddressFields\":\"false\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV2SearchById =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:37.799+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-25\",")
            .append("\"mdc\":{\"http_access.response.duration_milliseconds\":\"5\",")
            .append(
                "\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",\"http_access.request.header.Accept-Charset\":\"utf-8\",")
            .append("\"http_access.response.header.Date\":\"Fri,08Jul202200:02:37GMT\",")
            .append("\"jpa_query.bene_by_id.include_false.duration_milliseconds\":\"2\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append("\"jpa_query.bene_by_id.include_false.duration_nanoseconds\":\"2636912\",")
            .append("\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/Patient(IncludeAddressFields=true,IncludeIdentifiers=(false),IncludeTaxNumbers=false,_lastUpdated=false,by=id)\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"vtlbnWIeAqUjlii3\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",")
            .append(
                "\"http_access.response.status\":\"200\",\"http_access.request.header.IncludeIdentifiers\":\"[false]\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:37GMT\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append("\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Patient\",")
            .append("\"jpa_query.bene_by_id.include_false.record_count\":\"1\",")
            .append("\"http_access.request.header.Connection\":\"keep-alive\",")
            .append(
                "\"http_access.request.header.IncludeTaxNumbers\":\"false\",\"http_access.request.uri\":\"/v2/fhir/Patient\",")
            .append(
                "\"http_access.request.query_string\":\"_id=%7C567834\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"1000\",\"http_access.request.header.IncludeAddressFields\":\"true\",")
            .append(
                "\"http_access.request.header.Accept-Encoding\":\"gzip\"},\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV2SearchByIdentifier =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:36.753+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-18\",")
            .append(
                "\"mdc\":{\"http_access.response.duration_milliseconds\":\"4\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:36GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\"")
            .append(
                ",\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.record_count\":\"1\",\"http_access.request.header.Host\":\"localhost:8992\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/Patient(IncludeAddressFields=false,IncludeIdentifiers=(),IncludeTaxNumbers=false,_lastUpdated=false,by=identifier)\",")
            .append(
                "\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_milliseconds\":\"0\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"oLGAQLg0c7KV2xyp\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",")
            .append(
                "\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.duration_nanoseconds\":\"643088\",")
            .append(
                "\"jpa_query.bene_by_mbi.mbis_from_beneficiarieshistory.record_count\":\"2\",\"http_access.response.status\":\"200\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append("\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:36GMT\",")
            .append(
                "\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_milliseconds\":\"2\",")
            .append(
                "\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",\"http_access.request.http_method\":\"GET\",")
            .append(
                "\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Patient\",\"http_access.request.header.Connection\":\"keep-alive\",")
            .append("\"http_access.request.uri\":\"/v2/fhir/Patient\",")
            .append(
                "\"http_access.request.query_string\":\"identifier=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fidentifier%2Fmbi-hash%7C82273caf4d2c3b5a8340190ae3575950957ce469e593efd7736d60c3b39d253c\",")
            .append(
                "\"jpa_query.bene_by_mbi.bene_by_mbi_or_id.include_.duration_nanoseconds\":\"2093961\",\"bene_id\":\"567834\",")
            .append(
                "\"http_access.response.output_size_in_bytes\":\"1003\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String patientV2SearchByCoverageContract =
        new StringBuilder(
                "{\"timestamp\":\"2022-07-08T00:02:36.644+0000\",\"level\":\"INFO\",\"thread\":\"qtp1440621772-21\",")
            .append(
                "\"mdc\":{\"jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_nanoseconds\":\"680659\",")
            .append(
                "\"http_access.response.duration_milliseconds\":\"9\",\"http_access.request.clientSSL.DN\":\"CN=client-local-dev\",")
            .append(
                "\"http_access.request.header.Accept-Charset\":\"utf-8\",\"http_access.response.header.Date\":\"Fri,08Jul202200:02:36GMT\",")
            .append(
                "\"http_access.request.header.Accept\":\"application/fhir+xml;q=1.0,application/fhir+json;q=1.0,application/xml+fhir;q=0.9,application/json+fhir;q=0.9\",")
            .append(
                "\"http_access.request.header.Host\":\"localhost:8992\",\"jpa_query.benes_by_year_month_part_d_contract_id.duration_nanoseconds\":\"3523745\",")
            .append("\"jpa_query.benes_by_year_month_part_d_contract_id.record_count\":\"1\",")
            .append(
                "\"http_access.request.operation\":\"/v2/fhir/Patient(IncludeAddressFields=true,IncludeIdentifiers=(mbi),IncludeTaxNumbers=false,by=coverageContractForYearMonth)\",")
            .append(
                "\"jpa_query.benes_by_year_month_part_d_contract_id.duration_milliseconds\":\"3\",\"jpa_query.bene_ids_by_year_month_part_d_contract_id.record_count\":\"1\",")
            .append(
                "\"http_access.response.header.Content-Encoding\":\"gzip\",\"http_access.response.header.X-Request-ID\":\"DhcWWeN9U79VXNZr\",")
            .append(
                "\"http_access.response.header.Content-Type\":\"application/fhir+json;charset=utf-8\",")
            .append(
                "\"http_access.response.header.X-Powered-By\":\"HAPIFHIR6.0.2RESTServer(FHIRServer;FHIR4.0.1/R4)\",\"http_access.response.status\":\"200\",")
            .append(
                "\"jpa_query.bene_count_by_year_month_part_d_contract_id.duration_milliseconds\":\"0\",\"http_access.request.header.IncludeIdentifiers\":\"[mbi]\",")
            .append(
                "\"http_access.request.header.User-Agent\":\"HAPI-FHIR/6.0.2(FHIRClient;FHIR4.0.1/R4;apache)\",")
            .append(
                "\"http_access.response.header.Last-Modified\":\"Fri,08Jul202200:02:36GMT\",\"http_access.request_type\":\"org.eclipse.jetty.server.Request\",")
            .append(
                "\"http_access.request.http_method\":\"GET\",\"http_access.request.url\":\"https://localhost:8992/v2/fhir/Patient\",")
            .append(
                "\"jpa_query.bene_count_by_year_month_part_d_contract_id.duration_nanoseconds\":\"936219\",")
            .append(
                "\"jpa_query.bene_count_by_year_month_part_d_contract_id.record_count\":\"1\",\"http_access.request.header.Connection\":\"keep-alive\",")
            .append(
                "\"http_access.request.header.IncludeTaxNumbers\":\"false\",\"http_access.request.uri\":\"/v2/fhir/Patient\",")
            .append(
                "\"http_access.request.query_string\":\"_has%3ACoverage.extension=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Fptdcntrct01%7CS4607&_has%3ACoverage.rfrncyr=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Frfrnc_yr%7C2018\",")
            .append(
                "\"jpa_query.bene_ids_by_year_month_part_d_contract_id.duration_milliseconds\":\"0\",\"bene_id\":\"567834\",\"http_access.response.output_size_in_bytes\":\"1058\",")
            .append(
                "\"http_access.request.header.IncludeAddressFields\":\"true\",\"http_access.request.header.Accept-Encoding\":\"gzip\"},")
            .append(
                "\"logger\":\"HTTP_ACCESS\",\"message\":\"responsecomplete\",\"context\":\"default\"}")
            .toString();

    String readCoveragePattern = createBeneIdMdcPattern("Coverage", "id", true);
    assertTrue(Pattern.compile(readCoveragePattern).matcher(coverageV1Read).matches());
    assertTrue(Pattern.compile(readCoveragePattern).matcher(coverageV2Read).matches());
    assertFalse(Pattern.compile(readCoveragePattern).matcher(coverageV3Read).matches());

    String readEobPattern = createBeneIdMdcPattern("ExplanationOfBenefit", "id", false);
    assertTrue(Pattern.compile(readEobPattern).matcher(eobV1Read).matches());
    assertTrue(Pattern.compile(readEobPattern).matcher(eobV2Read).matches());

    String readPatientPattern = createBeneIdMdcPattern("Patient", "id", true);
    assertTrue(Pattern.compile(readPatientPattern).matcher(patientV1Read).matches());
    assertTrue(Pattern.compile(readPatientPattern).matcher(patientV2Read).matches());

    String searchCoveragePattern = createBeneIdMdcPattern("Coverage", "beneficiary", true);
    assertTrue(Pattern.compile(searchCoveragePattern).matcher(coverageV1Search).matches());
    assertTrue(Pattern.compile(searchCoveragePattern).matcher(coverageV2Search).matches());

    String searchEobPattern = createBeneIdMdcPattern("ExplanationOfBenefit", "patient", false);
    assertTrue(Pattern.compile(searchEobPattern).matcher(eobV1Search).matches());
    assertTrue(Pattern.compile(searchEobPattern).matcher(eobV2Search).matches());
    assertFalse(Pattern.compile(readCoveragePattern).matcher(eobV3Search).matches());

    String searchPatientByIdPattern = createBeneIdMdcPattern("Patient", "id", false);
    assertTrue(Pattern.compile(searchPatientByIdPattern).matcher(patientV1SearchById).matches());
    assertTrue(Pattern.compile(searchPatientByIdPattern).matcher(patientV2SearchById).matches());

    String searchPatientByIdentifierPattern =
        createBeneIdMdcPattern("Patient", "identifier", false);
    assertTrue(
        Pattern.compile(searchPatientByIdentifierPattern)
            .matcher(patientV1SearchByIdentifier)
            .matches());
    assertTrue(
        Pattern.compile(searchPatientByIdentifierPattern)
            .matcher(patientV2SearchByIdentifier)
            .matches());

    String searchPatientByCoverageContract =
        createBeneIdMdcPattern("Patient", "coverageContractForYearMonth", true);
    assertTrue(
        Pattern.compile(searchPatientByCoverageContract)
            .matcher(patientV1SearchByCoverageContract)
            .matches());
    assertTrue(
        Pattern.compile(searchPatientByCoverageContract)
            .matcher(patientV2SearchByCoverageContract)
            .matches());
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
      assertTrue(
          Files.readString(accessLogJson)
              .contains(DataServerLauncherApp.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES));

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
