package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
  @Embedded private ClaimLineRxSupportingInfo claimRxSupportingInfo;

  /**
   * Per C4BB, if compound code = 2 -> populate productOrService with "compound".
   *
   * @return Optional containing the coding if applicable, otherwise empty
   */
  public Optional<Coding> toFhirNdcCompound() {
    return claimRxSupportingInfo.toFhirNdcCompound();
  }
}
