package gov.cms.bfd.server.ng.capability;

import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.hl7.fhir.instance.model.api.IBaseConformance;

/** Non-caching CapabilityStatementProvider to prevent cache-poisoning via Host header. */
public class NonCachingCapabilityStatementProvider extends ServerCapabilityStatementProvider {

  /** Represents the capabilities publisher value. */
  static final String CAPABILITIES_PUBLISHER = "Centers for Medicare & Medicaid Services";

  /**
   * Constructor accepting the server for super().
   *
   * @param theServer rest-server
   */
  public NonCachingCapabilityStatementProvider(RestfulServer theServer) {
    super(theServer);
    this.setPublisher(CAPABILITIES_PUBLISHER);
  }

  @Override
  @Metadata(cacheMillis = 0) // 0 = regenerate on every /metadata request
  public IBaseConformance getServerConformance(
      HttpServletRequest theRequest, RequestDetails theRequestDetails) {
    return super.getServerConformance(theRequest, theRequestDetails);
  }
}
