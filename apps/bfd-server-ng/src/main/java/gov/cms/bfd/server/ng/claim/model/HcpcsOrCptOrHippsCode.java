package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Represents the HCPCS, CPT and HIPPS code information for a prior authorization line. */
@Getter
@Embeddable
public class HcpcsOrCptOrHippsCode {

  @Column(name = "hcpcs_or_cpt_or_hipps")
  private String hcpcsOrCptOrHipps;

  /**
   * Determines the corresponding coding system for the applicable code.
   *
   * @param claimType the claim type specific to prior authorization
   * @return the coding system
   */
  public String getCodingSystem(Optional<ClaimTypePriorAuth> claimType) {
    if (claimType.isPresent() && claimType.get() == ClaimTypePriorAuth.Valid.I) {
      return SystemUrls.CMS_HIPPS;
    }
    return Character.isDigit(hcpcsOrCptOrHipps.charAt(0))
        ? SystemUrls.AMA_CPT
        : SystemUrls.CMS_HCPCS;
  }

  CodeableConcept toFhir(Optional<ClaimTypePriorAuth> claimType) {
    return new CodeableConcept()
        .addCoding(new Coding().setSystem(getCodingSystem(claimType)).setCode(hcpcsOrCptOrHipps));
  }
}
