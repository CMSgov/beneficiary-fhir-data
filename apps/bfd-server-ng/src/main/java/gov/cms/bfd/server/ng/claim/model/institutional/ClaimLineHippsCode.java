package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;

@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
public class ClaimLineHippsCode {
  @Column(name = "clm_rev_apc_hipps_cd")
  private Optional<String> hippsCode;

  public Optional<Coding> toFhir() {
    return hippsCode.map(s -> new Coding().setSystem(SystemUrls.CMS_HIPPS).setCode(s));
  }
}
