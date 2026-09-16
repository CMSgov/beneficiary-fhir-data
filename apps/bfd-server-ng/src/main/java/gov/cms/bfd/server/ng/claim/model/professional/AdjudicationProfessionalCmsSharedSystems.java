package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** eob level adjudication information for Professional-CMS-SharedSystems. */
@Embeddable
public class AdjudicationProfessionalCmsSharedSystems implements AdjudicationEmbedded {

  @Embedded private AdjudicationProfessionalSharedSystems baseAdjudication;

  @Column(name = "clm_bene_intrst_pd_amt") // CMS
  private BigDecimal beneInterestPaidAmount;

  @Column(name = "clm_blood_lblty_amt") // CMS
  private BigDecimal bloodLiabilityAmount;

  @Column(name = "clm_cob_ptnt_resp_amt") // CMS
  private BigDecimal cobPatientResponsibilityAmount;

  @Column(name = "clm_prvdr_otaf_amt") // CMS
  private BigDecimal providerObligationToAcceptAmount;

  @Column(name = "clm_prvdr_rmng_due_amt") // CMS
  private BigDecimal remainingAmountToProvider;

  @Column(name = "clm_blood_ncvrd_chrg_amt") // CMS
  private BigDecimal bloodNoncoveredChargeAmount;

  @Column(name = "clm_prvdr_intrst_pd_amt") // CMS
  private BigDecimal providerInterestPaidAmount;

  @Override
  public List<ExplanationOfBenefit.TotalComponent> toFhirTotal() {
    return baseAdjudication.toFhirTotal();
  }

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return List.of(
        AdjudicationChargeType.BENE_INTEREST_PAID_AMOUNT.toFhirAdjudication(beneInterestPaidAmount),
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
            remainingAmountToProvider));
  }
}
