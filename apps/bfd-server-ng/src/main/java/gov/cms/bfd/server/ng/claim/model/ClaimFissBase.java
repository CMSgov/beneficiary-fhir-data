package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

// TODO: remove
/** Fiscal Intermediary Standard System claims table. */
@Getter
@MappedSuperclass
public class ClaimFissBase {

  /*@Column(name = "clm_crnt_stus_cd")
  private Optional<ClaimCurrentStatusCode> claimCurrentStatusCode;

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome(ClaimTypeCode claimTypecode) {
    if (claimTypecode.isPacStage2()) {
      return claimCurrentStatusCode.map(ClaimCurrentStatusCode::getOutcome);
    }
    return Optional.empty();
  }*/
}
