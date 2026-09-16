package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * clm_line adjudication data shared by REGULAR and as the base for CMS, similar to institutional.
 */
@Embeddable
public class ClaimLineAdjudicationProfessionalRegular implements AdjudicationEmbedded {

  @Column(name = "clm_line_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_line_mdcr_coinsrnc_amt")
  private BigDecimal coinsrncAmount;

  @Column(name = "clm_line_prfnl_dme_price_amt")
  private BigDecimal purchasePriceAmount;

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    var charges = new ArrayList<ExplanationOfBenefit.AdjudicationComponent>();
    charges.add(AdjudicationEmbedded.getProfessionalBenefitPaymentStatus());
    charges.add(
        AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(allowedChargeAmount));
    charges.add(
        AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
            submittedChargeAmount));
    charges.add(AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount));
    charges.add(
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount));
    charges.add(
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
            deductibleAmount));
    charges.add(
        AdjudicationChargeType.LINE_MEDICARE_COINSURANCE_AMOUNT.toFhirAdjudication(coinsrncAmount));
    charges.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_PURCHASE_PRICE_AMOUNT.toFhirAdjudication(
            purchasePriceAmount));
    return charges;
  }
}
