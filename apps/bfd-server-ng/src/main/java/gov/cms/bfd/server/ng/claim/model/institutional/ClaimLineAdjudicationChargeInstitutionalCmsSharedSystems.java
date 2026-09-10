package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The CMS specific shared system adjudication charge columns. */
@Embeddable
@Getter
public class ClaimLineAdjudicationChargeInstitutionalCmsSharedSystems {

  @Embedded ClaimLineAdjudicationChargeInstitutionalSharedSystems baseAdjudicationCharge;

  @Column(name = "clm_line_ncvrd_pd_amt") // CMS
  private BigDecimal noncoveredProductPaidAmount;

  @Column(name = "clm_line_prvdr_pmt_amt") // CMS
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_otaf_amt") // CMS
  private BigDecimal providerObligationToAcceptFullAmount;

  @Column(name = "clm_line_othr_tp_pd_amt") // CMS
  private BigDecimal otherThirdPartyPaidAmount;

  @Column(name = "clm_line_instnl_adjstd_amt") // CMS
  private BigDecimal adjustedAmount;

  @Column(name = "clm_line_instnl_rdcd_amt") // CMS
  private BigDecimal reducedAmount;

  @Column(name = "clm_line_instnl_msp1_pd_amt") // CMS
  private BigDecimal msp1PaidAmount;

  @Column(name = "clm_line_instnl_msp2_pd_amt") // CMS
  private BigDecimal msp2PaidAmount;

  @Column(name = "clm_line_instnl_rate_amt") // CMS
  private BigDecimal rateAmount;

  @Column(name = "clm_line_add_on_pymt_amt") // CMS
  private BigDecimal addOnPaymentAmount;

  @Column(name = "clm_line_non_ehr_rdctn_amt") // CMS
  private BigDecimal nonEHRReductionAmount;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    var result = new ArrayList<>(baseAdjudicationCharge.toFhir());
    result.add(
        AdjudicationChargeType.LINE_NONCOVERED_PRODUCT_PAID_AMOUNT.toFhirAdjudication(
            noncoveredProductPaidAmount));
    result.add(
        AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
            providerPaymentAmount));
    result.add(
        AdjudicationChargeType.LINE_PROVIDER_OBLIGATION_FULL_AMOUNT.toFhirAdjudication(
            providerObligationToAcceptFullAmount));
    result.add(
        AdjudicationChargeType.LINE_OTHER_THIRD_PARTY_PAID_AMOUNT.toFhirAdjudication(
            otherThirdPartyPaidAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_ADJUSTED_AMOUNT.toFhirAdjudication(
            adjustedAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_REDUCED_AMOUNT.toFhirAdjudication(reducedAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_1ST_MSP_PAID_AMOUNT.toFhirAdjudication(
            msp1PaidAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_2ND_PAID_AMOUNT.toFhirAdjudication(
            msp2PaidAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_RATE_AMOUNT.toFhirAdjudication(rateAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_ADD_ON_PAYMENT_AMOUNT.toFhirAdjudication(
            addOnPaymentAmount));
    result.add(
        AdjudicationChargeType.LINE_INSTITUTIONAL_NON_EHR_REDUCTION_AMOUNT.toFhirAdjudication(
            nonEHRReductionAmount));
    return result;
  }
}
