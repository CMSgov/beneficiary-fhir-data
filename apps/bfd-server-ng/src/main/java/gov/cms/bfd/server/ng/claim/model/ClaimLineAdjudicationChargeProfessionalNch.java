package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class ClaimLineAdjudicationChargeProfessionalNch
    extends ClaimLineAdjudicationChargeProfessionalBase {

  @Column(name = "clm_line_prfnl_intrst_amt")
  private BigDecimal professionalInterestAmount;

  @Column(name = "clm_mdcr_prmry_pyr_alowd_amt")
  private BigDecimal primaryPayerAllowedAmount;

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private BigDecimal primaryPayerPaidAmount;

  @Column(name = "clm_line_dmerc_scrn_svgs_amt")
  private BigDecimal screenSavingsAmount;

  @Override
  Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges() {
    return Stream.of(
        AdjudicationChargeType.LINE_PROFESSIONAL_INTEREST_AMOUNT.toFhirAdjudication(
            professionalInterestAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT.toFhirAdjudication(
            primaryPayerAllowedAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT.toFhirAdjudication(
            primaryPayerPaidAmount),
        AdjudicationChargeType.LINE_PROFESSIONAL_SCREEN_SAVINGS_AMOUNT.toFhirAdjudication(
            screenSavingsAmount));
  }
}
