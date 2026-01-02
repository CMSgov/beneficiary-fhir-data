package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResponseResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import io.dropwizard.metrics.servlets.PingServlet;
import io.dropwizard.metrics.servlets.ThreadDumpServlet;
import jakarta.servlet.http.HttpServletRequest;
import java.util.SortedMap;
import java.util.TreeMap;
import org.hl7.fhir.dstu3.hapi.rest.server.ServerCapabilityStatementProvider;

/**
 * Models the canonical "operations" supported by this application, such that each meaningfully
 * distinct operation can be assigned a unique canonical name, for use in monitoring systems.
 *
 * <p>Also handles the publishing of those canonical names to the places they need to be published,
 * via the {@link #publishOperationName()} method.
 */
public final class CanonicalOperation {
  /** The operational endpoint. */
  private final Endpoint endpoint;

  /**
   * A mode, query parameter, HTTP header, etc. that meaningfully impacts the behavior of the
   * operation such that {@link CanonicalOperation}s with different values for it should be tracked
   * separately in our monitoring tools.
   */
  private final SortedMap<String, String> options;

  /**
   * Constructs a new {@link CanonicalOperation}.
   *
   * @param endpoint the {@link Endpoint} constant for the HTTP endpoint/handler that will process
   *     the request
   */
  public CanonicalOperation(Endpoint endpoint) {
    this.endpoint = endpoint;
    this.options = new TreeMap<String, String>();
  }

  /**
   * Gets the canonical name for the HTTP request represented by this {@link CanonicalOperation}.
   *
   * @return the canonical name
   */
  private String getCanonicalName() {
    return String.format(
        "%s%s",
        endpoint.getCanonicalName(), options.toString().replace('{', '(').replace('}', ')'));
  }

  /**
   * Sets an {@link CanonicalOperation} option: a mode, query parameter, HTTP header, etc. that
   * meaningfully impacts the behavior of the operation such that {@link CanonicalOperation}s with
   * different values for it should be tracked separately in our monitoring tools.
   *
   * @param key the key/name of the option (case-sensitive)
   * @param value the value of the option (case-sensitive), note that any square brackets will be
   *     replaced by parentheses
   */
  public void setOption(String key, String value) {
    this.options.put(key, value.replace('[', '(').replace(']', ')'));
  }

  /**
   * Publish the {@link #getCanonicalName()} value to the logging {@link BfdMDC} as the transaction
   * name.
   */
  public void publishOperationName() {
    String canonicalName = getCanonicalName();

    // Ensure that the operation name lands in our access logs.
    BfdMDC.put(BfdMDC.computeMDCKey("http_access", "request", "operation"), canonicalName);
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

    /** Handled by {@link R4ExplanationOfBenefitResourceProvider}. */
    V2_EOB("/v2/fhir/ExplanationOfBenefit"),

    /** Handled by {@link R4ClaimResourceProvider}. */
    V2_CLAIM("/v2/fhir/Claim"),

    /** Handled by {@link R4ClaimResponseResourceProvider}. */
    V2_CLAIM_RESPONSE("/v2/fhir/ClaimResponse"),

    /** Some other, unknown HTTP endpoint/operation. */
    OTHER(null);

    /**
     * A URI path that represents all invocations of this {@link Endpoint}, or {@code null} for
     * unknown {@link Endpoint}s. *
     */
    private final String requestHttpUri;

    /**
     * Constructs a new Endpoint.
     *
     * @param requestHttpUri a URI path that represents all invocations of this {@link Endpoint}, or
     *     {@code null} for unknown {@link Endpoint}s
     */
    private Endpoint(String requestHttpUri) {
      this.requestHttpUri = requestHttpUri;
    }

    /**
     * Gets the canonical name for the HTTP endpoint/handler represented by this {@link Endpoint}.
     *
     * @return the canonical name
     */
    public String getCanonicalName() {
      return requestHttpUri;
    }

    /**
     * Attempts to return a known {@link Endpoint} based on the {@link HttpServletRequest} request
     * uri.
     *
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
