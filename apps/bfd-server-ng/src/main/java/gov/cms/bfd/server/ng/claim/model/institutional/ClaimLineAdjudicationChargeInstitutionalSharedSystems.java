package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The shared adjudication charge columns between CMS and REGULAR. */
@Embeddable
public class ClaimLineAdjudicationChargeInstitutionalSharedSystems {
  @Column(name = "clm_line_ncvrd_chrg_amt") // REGULAR CMS
  private BigDecimal noncoveredChargeAmount;

  @Column(name = "clm_line_alowd_chrg_amt") // REGULAR CMS
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt") // REGULAR CMS
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_bene_pmt_amt") // REGULAR CMS
  private BigDecimal benePaymentAmount;

  @Column(name = "clm_line_bene_pd_amt") // REGULAR CMS
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt") // REGULAR CMS
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt") // REGULAR CMS
  private BigDecimal deductibleAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    return List.of(
        AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(allowedChargeAmount),
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(deductibleAmount),
        AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
        AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(benePaymentAmount),
        AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            noncoveredChargeAmount),
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount),
        AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
            submittedChargeAmount));
  }
}
