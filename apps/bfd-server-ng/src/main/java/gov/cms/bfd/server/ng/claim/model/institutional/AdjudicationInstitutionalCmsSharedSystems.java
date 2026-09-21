package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Adjudication fields for institutional claim, cms profile, shared system. */
@Embeddable
public class AdjudicationInstitutionalCmsSharedSystems implements AdjudicationEmbedded {

  @Embedded AdjudicationInstitutionalRegularSharedSystems adjudicationChargeBase;

  @Column(name = "clm_bene_intrst_pd_amt")
  private BigDecimal beneInterestPaidAmount;

  @Column(name = "clm_blood_lblty_amt")
  private BigDecimal bloodLiabilityAmount;

  @Column(name = "clm_blood_ncvrd_chrg_amt")
  private BigDecimal bloodNoncoveredChargeAmount;

  @Column(name = "clm_tot_cntrctl_amt")
  private BigDecimal totalContractualAmountDiscrepancy;

  @Column(name = "clm_cob_ptnt_resp_amt")
  private BigDecimal cobPatientResponsibilityAmount;

  @Column(name = "clm_prvdr_otaf_amt")
  private BigDecimal providerObligationToAcceptAmount;

  @Column(name = "clm_prvdr_rmng_due_amt")
  private BigDecimal remainingAmountToProvider;

  @Column(name = "clm_prvdr_intrst_pd_amt")
  private BigDecimal providerInterestPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return adjudicationChargeBase.toFhirTotal();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            adjudicationChargeBase.toFhirAdjudication().stream(),
            Stream.of(
                AdjudicationChargeType.BENE_INTEREST_PAID_AMOUNT.toFhirAdjudication(
                    beneInterestPaidAmount),
                AdjudicationChargeType.BENE_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT.toFhirAdjudication(
                    bloodLiabilityAmount),
                AdjudicationChargeType.BLOOD_NONCOVERED_CHARGE_AMOUNT.toFhirAdjudication(
                    bloodNoncoveredChargeAmount),
                AdjudicationChargeType.COB_PATIENT_RESPONSIBILITY_AMOUNT.toFhirAdjudication(
                    cobPatientResponsibilityAmount),
                AdjudicationChargeType.PROVIDER_INTEREST_PAID_AMOUNT.toFhirAdjudication(
                    providerInterestPaidAmount),
                AdjudicationChargeType.PROVIDER_OBLIGATION_TO_ACCEPT_AMOUNT.toFhirAdjudication(
                    providerObligationToAcceptAmount),
                AdjudicationChargeType.REMAINING_AMOUNT_TO_PROVIDER.toFhirAdjudication(
                    remainingAmountToProvider),
                AdjudicationChargeType.TOTAL_CONTRACTUAL_AMOUNT_DISCREPANCY.toFhirAdjudication(
                    totalContractualAmountDiscrepancy)))
        .toList();
  }
}
