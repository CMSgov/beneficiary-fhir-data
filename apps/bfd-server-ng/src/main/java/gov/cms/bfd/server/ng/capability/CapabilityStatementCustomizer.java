package gov.cms.bfd.server.ng.capability;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.net.URI;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Interceptor to customize the FHIR:CapabilityStatement after generation before returning a
 * /metadata call. currently transforming url
 */
@Component
@Interceptor
public class CapabilityStatementCustomizer {

  private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");

  private final String implementationUrl;
  private final String version;

  /**
   * constructor injecting the configuration value for url.
   *
   * @param implementationUrl extracted from config
   * @param version project version extracted from config
   */
  public CapabilityStatementCustomizer(
      @Value("${bfd.project.url}") String implementationUrl,
      @Value("${bfd.project.version}") String version) {
    this.implementationUrl = implementationUrl;
    this.version = version;
  }

  /**
   * hook function used to actually transform the CapabilityStatement prior to returning.
   *
   * @param theCapabilityStatement generated
   * @param theRequestDetails request-details from /metadata call
   */
  @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
  public void customize(IBaseConformance theCapabilityStatement, RequestDetails theRequestDetails) {
    ((CapabilityStatement) theCapabilityStatement).setVersion(version);

    var host = URI.create(theRequestDetails.getFhirServerBase()).getHost();
    if (null != host && LOCAL_HOSTS.contains(host.toLowerCase())) {
      return; // running locally: leave HAPI's default URL alone
    }
    ((CapabilityStatement) theCapabilityStatement).getImplementation().setUrl(implementationUrl);
  }
}
