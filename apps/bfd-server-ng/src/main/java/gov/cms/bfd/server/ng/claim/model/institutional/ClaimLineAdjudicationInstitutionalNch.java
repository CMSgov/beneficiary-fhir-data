package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Base class for clm_line adjudication data, institutional, NCH (complete data for Regular-NCH).
 */
@Embeddable
class ClaimLineAdjudicationInstitutionalNch implements AdjudicationEmbedded {

  @Column(name = "clm_line_ncvrd_chrg_amt")
  private BigDecimal noncoveredChargeAmount; // REGULAR, CMS

  @Column(name = "clm_line_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount; // REGULAR, CMS

  @Column(name = "clm_line_bene_pmt_amt")
  private BigDecimal benePaymentAmount; // REGULAR, CMS

  @Column(name = "clm_line_bene_pd_amt")
  private BigDecimal benePaidAmount; // REGULAR, CMS

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount; // REGULAR, CMS

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount; // REGULAR, CMS

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
        AdjudicationChargeType.LINE_BENE_PAYMENT_AMOUNT.toFhirAdjudication(benePaymentAmount),
        AdjudicationChargeType.LINE_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
            noncoveredChargeAmount),
        AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(coveredPaidAmount),
        AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
            submittedChargeAmount),
        AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
            deductibleAmount));
  }
}
