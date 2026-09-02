package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimLineDeductibleCoinsuranceCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pmd_uniq_trkng_num"))
public class ClaimLineInstitutionalCmsNch extends ClaimLineInstitutionalBase {

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Embedded private ClaimLineInstitutionalExtensions institutionalExtensions;
  @Embedded private ClaimLineAdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimAnsiSignature ansiSignature;
  @Embedded private ClaimLineInstitutionalNchExtensions claimLineInstitutionalNchExtensions;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhir().forEach(line::addAdjudication);
    ansiSignature.toFhir().ifPresent(line::addAdjudication);
  }

  @Override
  protected void addExtensions(ExplanationOfBenefit.ItemComponent line) {
    claimLineInstitutionalNchExtensions.toFhir().forEach(line::addExtension);
    institutionalExtensions.toFhir().forEach(line::addExtension);
  }

  @Override
  protected void addRevenue(ExplanationOfBenefit.ItemComponent line) {
    getRevenueCenterCode()
        .ifPresent(
            c -> {
              var revenueCoding = c.toFhir(getDeductibleCoinsuranceCode());
              line.setRevenue(revenueCoding);
            });
  }
}
