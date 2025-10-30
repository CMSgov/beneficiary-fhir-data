package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.*;
import lombok.Getter;

/** Pharmacy claim line table. */
@Getter
@Entity
@Table(name = "claim_line_rx", schema = "idr")
public class ClaimLineRx {
  @EmbeddedId ClaimLineRxId claimLineInstitutionalId;
  @Embedded private AdjudicationChargeRx adjudicationCharge;

  @OneToOne(mappedBy = "claimLineRx")
  private ClaimItem claimLine;
}
