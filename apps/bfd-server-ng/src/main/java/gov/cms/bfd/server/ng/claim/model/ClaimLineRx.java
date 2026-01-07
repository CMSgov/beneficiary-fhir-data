package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** Pharmacy claim line table. */
@Getter
@Entity
@Table(name = "claim_line_rx", schema = "idr")
public class ClaimLineRx {
  @EmbeddedId ClaimLineRxId claimLineInstitutionalId;
  @Embedded private ClaimLineAdjudicationChargeRx claimLineAdjudicationChargeRx;
  @Embedded private ClaimRxSupportingInfo claimRxSupportingInfo;

  @OneToOne(mappedBy = "claimLineRx")
  private ClaimItem claimLine;

  /**
   * Per C4BB, if compound code = 2 -> populate productOrService with "compound".
   *
   * @return Optional containing the coding if applicable, otherwise empty
   */
  public Optional<Coding> toFhirNdcCompound() {
    return claimRxSupportingInfo
        .getCompoundCode()
        .filter(c -> c == ClaimLineCompoundCode._2)
        .map(c -> new Coding().setSystem(SystemUrls.CARIN_COMPOUND_LITERAL).setCode("compound"));
  }
}
