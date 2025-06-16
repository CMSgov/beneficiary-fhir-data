package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "claim_line_institutional", schema = "idr")
public class ClaimLineInstitutional {
  @EmbeddedId ClaimLineInstitutionalId claimLineInstitutionalId;

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private ClaimLineDeductibleCoinsuranceCode deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private LocalDate revenueCenterDate;

  @Embedded private ClaimLineHippsCode hippsCode;

  @Embedded private AdjudicationChargeInstitutional adjudicationCharge;

  @OneToOne
  @JoinColumn(name = "clm_ansi_sgntr_sk")
  private ClaimAnsiSignature ansiSignature;

  @OneToOne(mappedBy = "claimLineInstitutional")
  private ClaimLine claimLine;
}
