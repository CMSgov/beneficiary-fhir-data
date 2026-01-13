package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Professional claims table. */
@Getter
@Entity
@Table(name = "claim_professional", schema = "idr")
public class ClaimProfessional {
  @Id
  @Column(name = "clm_uniq_id")
  private long claimUniqueId;

  @OneToOne(mappedBy = "claimProfessional")
  private Claim claim;

  @Column(name = "clm_mdcr_prfnl_prmry_pyr_amt")
  private BigDecimal primaryProviderPaidAmount;

  @Column(name = "clm_prvdr_acnt_rcvbl_ofst_amt")
  private BigDecimal providerOffsetAmount;

  @Column(name = "clm_audt_trl_stus_cd")
  private Optional<ClaimAuditTrailStatusCode> claimAuditTrailStatusCode;

  @Embedded private ClaimProfessionalSupportingInfo supportingInfo;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.PAYER_PAID_AMOUNT.toFhirAdjudication(primaryProviderPaidAmount),
        AdjudicationChargeType.PROVIDER_OFFSET_AMOUNT.toFhirAdjudication(providerOffsetAmount));
  }

  Optional<ExplanationOfBenefit.RemittanceOutcome> toFhirOutcome(ClaimTypeCode claimTypecode) {
    if (claimTypecode.isPacStage2()) {
      return claimAuditTrailStatusCode.map(ClaimAuditTrailStatusCode::getOutcome);
    }
    return Optional.empty();
  }
}
