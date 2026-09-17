package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import gov.cms.bfd.server.ng.claim.model.common.AdjudicationEmbedded;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** clm_line adjudication information for Professional-CMS-NCH. */
@Embeddable
public class ClaimLineAdjudicationProfessionalCmsNch implements AdjudicationEmbedded {

  @Embedded private ClaimLineAdjudicationProfessionalCms cmsAdjudication;

  @Column(name = "clm_line_prfnl_intrst_amt")
  private BigDecimal professionalInterestAmount;

  @Column(name = "clm_mdcr_prmry_pyr_alowd_amt")
  private BigDecimal primaryPayerAllowedAmount;

  @Column(name = "clm_bene_prmry_pyr_pd_amt")
  private BigDecimal primaryPayerPaidAmount;

  @Column(name = "clm_line_dmerc_scrn_svgs_amt")
  private BigDecimal screenSavingsAmount;

  @Override
  public List<ExplanationOfBenefit.AdjudicationComponent> toFhirAdjudication() {
    return Stream.concat(
            cmsAdjudication.toFhirAdjudication().stream(),
            Stream.of(
                AdjudicationChargeType.LINE_PROFESSIONAL_INTEREST_AMOUNT.toFhirAdjudication(
                    professionalInterestAmount),
                AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_ALLOWED_AMOUNT
                    .toFhirAdjudication(primaryPayerAllowedAmount),
                AdjudicationChargeType.LINE_PROFESSIONAL_PRIMARY_PAYER_PAID_AMOUNT
                    .toFhirAdjudication(primaryPayerPaidAmount),
                AdjudicationChargeType.LINE_PROFESSIONAL_SCREEN_SAVINGS_AMOUNT.toFhirAdjudication(
                    screenSavingsAmount)))
        .toList();
  }
}
