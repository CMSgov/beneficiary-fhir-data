package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Pharmacy claim line table. */
@Getter
@Entity
@Table(name = "claim_line_rx", schema = "idr")
public class ClaimLineRx {
  @EmbeddedId ClaimLineRxId claimLineInstitutionalId;
  @Embedded private AdjudicationChargeRx adjudicationCharge;
  @Embedded private ClaimRxSupportingInfo claimRxSupportingInfo;

  @OneToOne(mappedBy = "claimLineRx")
  private ClaimItem claimLine;

  public void resolveCompoundCode(CodeableConcept productOrService) {
    claimRxSupportingInfo
        .getCompoundCode()
        .filter(c -> c == ClaimLineCompoundCode._2)
        .ifPresent(
            c ->
                productOrService.addCoding(
                    new Coding().setSystem(SystemUrls.CARIN_COMPOUND_LITERAL).setCode("compound")));
  }
}
