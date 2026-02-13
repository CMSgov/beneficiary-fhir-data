package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** Institutional claim line table. */
@Getter
@Entity
@Table(name = "claim_line_institutional", schema = "idr")
public class ClaimLineInstitutional extends ClaimLineInstitutionalBase {
  @EmbeddedId ClaimLineInstitutionalId claimLineInstitutionalId;
  @Embedded private ClaimLineInstitutionalOptional claimLineInstitutionalOptional;

  @Embedded
  private ClaimLineAdjudicationChargeInstitutional claimLineAdjudicationChargeInstitutional;
}
