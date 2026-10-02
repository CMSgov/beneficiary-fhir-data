package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
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

  private final String implementationUrl;

  /**
   * constructor injecting the configuration value for url.
   *
   * @param implementationUrl extracted from config
   */
  public CapabilityStatementCustomizer(@Value("${bfd.project.url}") String implementationUrl) {
    this.implementationUrl = implementationUrl;
  }

  /**
   * hook function used to actually transform the CapabilityStatement prior to returning.
   *
   * @param theCapabilityStatement generated
   */
  @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
  public void customize(IBaseConformance theCapabilityStatement) {
    ((CapabilityStatement) theCapabilityStatement).getImplementation().setUrl(implementationUrl);
  }
}
