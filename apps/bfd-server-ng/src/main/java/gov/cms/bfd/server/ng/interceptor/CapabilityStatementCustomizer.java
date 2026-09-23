package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Interceptor
public class CapabilityStatementCustomizer {

  private final String implementationUrl;

  public CapabilityStatementCustomizer(@Value("${bfd.project.url}") String implementationUrl) {
    this.implementationUrl = implementationUrl;
  }

  @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
  public void customize(IBaseConformance theCapabilityStatement) {
    ((CapabilityStatement) theCapabilityStatement).getImplementation().setUrl(implementationUrl);
  }
}
