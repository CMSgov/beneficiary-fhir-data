package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Entity
public class ClaimLineInstitutional {

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private ClaimLineDeductibleCoinsuranceCode deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private LocalDate revenueCenterDate;

  @Embedded private ClaimLineHippsCode hippsCode;

  @Embedded private AdjudicationChargeInstitutional adjudicationCharge;

  @OneToOne private ClaimAnsiSignature ansiSignature;
}
