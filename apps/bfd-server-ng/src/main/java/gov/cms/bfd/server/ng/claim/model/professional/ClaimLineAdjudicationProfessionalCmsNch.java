package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimLineAdjudicationProfessionalCmsNch extends ClaimLineAdjudicationProfessionalCms {

  @Column(name = "clm_line_prfnl_intrst_amt")
  private BigDecimal professionalInterestAmount;

  @Column(name = "clm_mdcr_prmry_pyr_alowd_amt")
  private BigDecimal primaryPayerAllowedAmount;

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private BigDecimal primaryPayerPaidAmount;

  @Column(name = "clm_line_dmerc_scrn_svgs_amt")
  private BigDecimal screenSavingsAmount;

  @Override
  List<ExplanationOfBenefit.AdjudicationComponent> addSubclassAdjudications() {
    var adjudicationComponents = new ArrayList<>(super.addSubclassAdjudications());
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_INTEREST_AMOUNT.toFhirAdjudication(
            professionalInterestAmount));
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT.toFhirAdjudication(
            primaryPayerAllowedAmount));
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT.toFhirAdjudication(
            primaryPayerPaidAmount));
    adjudicationComponents.add(
        AdjudicationChargeType.LINE_PROFESSIONAL_SCREEN_SAVINGS_AMOUNT.toFhirAdjudication(
            screenSavingsAmount));
    return adjudicationComponents;
  }
}
