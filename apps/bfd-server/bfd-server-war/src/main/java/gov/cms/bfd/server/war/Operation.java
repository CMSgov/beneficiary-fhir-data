package gov.cms.bfd.server.war;

import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.PingServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;
import com.newrelic.api.agent.NewRelic;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider;
import org.slf4j.MDC;

/**
 * Models the canonical "operations" supported by this application, such that each meaningfully
 * distinct operation can be assigned a unique canonical name, for use in monitoring systems.
 *
 * <p>Also handles the publishing of those canonical names to the places they need to be published,
 * via the {@link #publishOperationName()} method.
 */
public final class Operation {
  private final Endpoint endpoint;
  private final SortedMap<String, String> options;

  /**
   * Constructs a new {@link Operation}.
   *
   * @param endpoint the {@link Endpoint} constant for the HTTP endpoint/handler that will process
   *     the request
   */
  public Operation(Endpoint endpoint) {
    this.endpoint = endpoint;
    this.options = new TreeMap<String, String>();
  }

  /** @return the canonical name for the HTTP request represented by this {@link Operation} */
  private String getCanonicalName() {
    return String.format(
        "%s%s",
        endpoint.getCanonicalName(), options.toString().replace('{', '(').replace('}', ')'));
  }

  /**
   * Sets an {@link Operation} option: a mode, query parameter, HTTP header, etc. that meaningfully
   * impacts the behavior of the operation such that {@link Operation}s with different values for it
   * should be tracked separately in our monitoring tools.
   *
   * @param key the key/name of the option (case-sensitive)
   * @param value the value of the option (case-sensitive), note that any square brackets will be
   *     replaced by parentheses
   */
  public void setOption(String key, String value) {
    this.options.put(key, value.replace('[', '(').replace(']', ')'));
  }

  /**
   * Publish the {@link #getCanonicalName()} value to the logging {@link MDC} and to {@link
   * NewRelic} as the transaction name.
   */
  public void publishOperationName() {
    String canonicalName = getCanonicalName();

    // Ensure that the operation name lands in our access logs.
    MDC.put(RequestResponseLoggingFilter.computeMdcRequestKey("operation"), canonicalName);

    // If we got a known operation name, publish it to New Relic as the "transaction name",
    // otherwise stick with New Relic's default transaction name.
    if (endpoint != Endpoint.OTHER) NewRelic.setTransactionName(null, canonicalName);
  }

  /** Enumerates the known HTTP endpoints/handlers, for use in logging and monitoring. */
  public enum Endpoint {
    /** Handled by {@link HealthCheckServlet}. */
    HEALTHCHECK("/metrics/healthcheck"),

    /** Handled by {@link MetricsServlet}. */
    METRICS("/metrics/metrics"),

    /** Handled by {@link PingServlet}. */
    PING("/metrics/ping"),

    /** Handled by {@link ThreadDumpServlet}. */
    THREADS("/metrics/threads"),

    /** Handled by {@link ServerCapabilityStatementProvider}. */
    V1_METADATA("/v1/fhir/metadata"),

    /** Handled by {@link PatientResourceProvider}. */
    V1_PATIENT("/v1/fhir/Patient"),

    /** Handled by {@link CoverageResourceProvider}. */
    V1_COVERAGE("/v1/fhir/Coverage"),

    /** Handled by {@link ExplanationOfBenefitResourceProvider}. */
    V1_EOB("/v1/fhir/ExplanationOfBenefit"),

    /** Handled by {@link R4PatientResourceProvider}. */
    V2_PATIENT("/v2/fhir/Patient"),

    /** Handled by {@link R4CoverageResourceProvider}. */
    V2_COVERAGE("/v2/fhir/Coverage"),

    /** Handled by {@link ExplanationOfBenefitResourceProvider}. */
    V2_EOB("/v2/fhir/ExplanationOfBenefit"),

    /** Some other, unknown HTTP endpoint/operation. */
    OTHER(null);

    private final String requestHttpUri;

    /**
     * Enum constant constructor.
     *
     * @param requestHttpUri a URI path that represents all invocations of this {@link Endpoint}, or
     *     <code>null</code> for unknown {@link Endpoint}s
     */
    private Endpoint(String requestHttpUri) {
      this.requestHttpUri = requestHttpUri;
    }

    /**
     * @return the canonical name for the HTTP endpoint/handler represented by this {@link Endpoint}
     */
    public String getCanonicalName() {
      return requestHttpUri;
    }

    /**
     * @param httpServletRequest the {@link HttpServletRequest} to find a match for
     * @return the {@link Endpoint} that matches the specified {@link HttpServletRequest}, or {@link
     *     Endpoint#OTHER} if no exact match could be found
     */
    public static Endpoint matchByHttpUri(HttpServletRequest httpServletRequest) {
      for (Endpoint endpoint : values()) {
        if (endpoint.requestHttpUri == null) continue;

        if (httpServletRequest.getRequestURI().startsWith(endpoint.requestHttpUri)) return endpoint;
      }

      return OTHER;
    }
  }
}
