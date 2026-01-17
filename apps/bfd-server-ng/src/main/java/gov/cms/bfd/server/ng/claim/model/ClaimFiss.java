package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Fiscal Intermediary Standard System claims table. */
@Getter
@Entity
@Table(name = "claim_fiss", schema = "idr")
public class ClaimFiss {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @Column(name = "clm_crnt_stus_cd")
  private Optional<ClaimCurrentStatusCode> claimCurrentStatusCode;

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome(ClaimTypeCode claimTypecode) {
    if (claimTypecode.isPacStage2()) {
      return claimCurrentStatusCode.map(ClaimCurrentStatusCode::getOutcome);
    }
    return Optional.empty();
  }
}
