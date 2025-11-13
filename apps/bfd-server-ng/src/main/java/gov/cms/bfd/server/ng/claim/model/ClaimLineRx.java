package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

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
}
