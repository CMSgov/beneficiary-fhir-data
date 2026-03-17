package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@MappedSuperclass
abstract class ClaimLineAdjudicationChargeProfessionalBase {

  @Column(name = "clm_line_alowd_chrg_amt")
  private BigDecimal allowedChargeAmount;

  @Column(name = "clm_line_sbmt_chrg_amt")
  private BigDecimal submittedChargeAmount;

  @Column(name = "clm_line_prvdr_pmt_amt")
  private BigDecimal providerPaymentAmount;

  @Column(name = "clm_line_bene_pd_amt")
  private BigDecimal benePaidAmount;

  @Column(name = "clm_line_cvrd_pd_amt")
  private BigDecimal coveredPaidAmount;

  @Column(name = "clm_line_mdcr_ddctbl_amt")
  private BigDecimal deductibleAmount;

  @Column(name = "clm_line_mdcr_coinsrnc_amt")
  private BigDecimal coinsrncAmount;

  @Column(name = "clm_line_carr_psych_ot_lmt_amt")
  private BigDecimal therapyAmountAppliedToLimit;

  @Column(name = "clm_line_prfnl_dme_price_amt")
  private BigDecimal purchasePriceAmount;

  @Column(name = "clm_srvc_ddctbl_sw")
  private Optional<ClaimServiceDeductibleCode> serviceDeductibleCode;

  List<ExplanationOfBenefit.AdjudicationComponent> toFhir() {
    var adjudicationComponent =
        new ArrayList<>(
            List.of(
                AdjudicationChargeType.LINE_PROFESSIONAL_THERAPY_LMT_AMOUNT.toFhirAdjudication(
                    therapyAmountAppliedToLimit),
                AdjudicationChargeType.LINE_MEDICARE_COINSURANCE_AMOUNT.toFhirAdjudication(
                    coinsrncAmount),
                AdjudicationChargeType.LINE_ALLOWED_CHARGE_AMOUNT.toFhirAdjudication(
                    allowedChargeAmount),
                AdjudicationChargeType.LINE_MEDICARE_DEDUCTIBLE_AMOUNT.toFhirAdjudication(
                    deductibleAmount),
                AdjudicationChargeType.LINE_BENE_PAID_AMOUNT.toFhirAdjudication(benePaidAmount),
                AdjudicationChargeType.LINE_PROVIDER_PAYMENT_AMOUNT.toFhirAdjudication(
                    providerPaymentAmount),
                AdjudicationChargeType.LINE_COVERED_PAID_AMOUNT.toFhirAdjudication(
                    coveredPaidAmount),
                AdjudicationChargeType.LINE_PROFESSIONAL_PURCHASE_PRICE_AMOUNT.toFhirAdjudication(
                    purchasePriceAmount),
                AdjudicationChargeType.LINE_SUBMITTED_CHARGE_AMOUNT.toFhirAdjudication(
                    submittedChargeAmount)));
    toAdjudicationComponent().ifPresent(adjudicationComponent::add);
    return adjudicationComponent;
  }

  Optional<ExplanationOfBenefit.AdjudicationComponent> toAdjudicationComponent() {
    // todo: actually might not need this if serviceDeductibleCode really is just a proxy field for
    // professional claims, check
    return serviceDeductibleCode.map(
        c -> {
          var adjudication = new ExplanationOfBenefit.AdjudicationComponent();
          adjudication.setCategory(
              new CodeableConcept()
                  .addCoding(
                      new Coding()
                          .setSystem(SystemUrls.CARIN_CODE_SYSTEM_ADJUDICATION_DISCRIMINATOR)
                          .setCode("benefitpaymentstatus")
                          .setDisplay("Benefit Payment Status")));
          adjudication.setReason(
              new CodeableConcept()
                  .addCoding(
                      new Coding()
                          .setSystem(SystemUrls.CARIN_CODE_SYSTEM_PAYER_ADJUDICATION_STATUS)
                          .setCode("other")
                          .setDisplay("Other")));
          return adjudication;
        });
  }

  abstract Stream<ExplanationOfBenefit.AdjudicationComponent> subClassCharges();
}
