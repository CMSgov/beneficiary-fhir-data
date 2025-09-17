package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Coding;

@Embeddable
class ClaimLineHcpcsCode {

  @Column(name = "clm_line_hcpcs_cd")
  private Optional<String> hcpcsCode;

  Optional<Coding> toFhir() {
    if (hcpcsCode.isEmpty()) {
      return Optional.empty();
    }
    var hcpcs = hcpcsCode.get();
    return Optional.of(
        new Coding().setSystem(FhirUtil.getHcpcsSystem(hcpcs.substring(0, 1))).setCode(hcpcs));
  }
}
