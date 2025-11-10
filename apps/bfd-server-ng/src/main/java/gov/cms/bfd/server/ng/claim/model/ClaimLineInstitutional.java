package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/** Institutional claim line table. */
@Getter
@Entity
@Table(name = "claim_line_institutional", schema = "idr")
public class ClaimLineInstitutional {
  @EmbeddedId ClaimLineInstitutionalId claimLineInstitutionalId;

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private LocalDate revenueCenterDate;

  @Embedded private ClaimLineHippsCode hippsCode;
  @Embedded private ClaimLineInstitutionalExtensions extensions;
  @Embedded private AdjudicationChargeInstitutional adjudicationCharge;

  @Nullable
  @OneToOne
  @JoinColumn(name = "clm_ansi_sgntr_sk")
  private ClaimAnsiSignature ansiSignature;

  @OneToOne(mappedBy = "claimLineInstitutional")
  private ClaimItem claimLine;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

  /**
   * Return claim ANSI signature data if available.
   *
   * @return claim ANSI signature
   */
  public Optional<ClaimAnsiSignature> getAnsiSignature() {
    return Optional.ofNullable(ansiSignature);
  }
}
