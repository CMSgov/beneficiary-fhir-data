package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;

/** Institutional claim line table. */
@Getter
@Entity
@Table(name = "claim_line_institutional", schema = "idr")
public class ClaimLineInstitutional {
  @EmbeddedId ClaimLineInstitutionalId claimLineInstitutionalId;

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private Optional<LocalDate> revenueCenterDate;

  @Embedded private ClaimLineHippsCode hippsCode;
  @Embedded private ClaimLineInstitutionalExtensions extensions;
  @Embedded private ClaimLineInstitutionalOptional claimLineInstitutionalOptional;

  @Embedded
  private ClaimLineAdjudicationChargeInstitutional claimLineAdjudicationChargeInstitutional;
}
