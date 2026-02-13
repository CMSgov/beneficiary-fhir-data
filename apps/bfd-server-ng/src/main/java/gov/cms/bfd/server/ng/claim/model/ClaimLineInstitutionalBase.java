package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;

/** Institutional claim line table. */
@Getter
@MappedSuperclass
public abstract class ClaimLineInstitutionalBase {

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Column(name = "clm_line_instnl_rev_ctr_dt")
  private Optional<LocalDate> revenueCenterDate;

  @Embedded private ClaimLineHippsCode hippsCode;
  @Embedded private ClaimLineInstitutionalExtensions extensions;
}
