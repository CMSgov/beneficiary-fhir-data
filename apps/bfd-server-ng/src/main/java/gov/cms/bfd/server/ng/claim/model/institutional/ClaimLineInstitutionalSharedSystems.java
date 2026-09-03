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
@AttributeOverride(name = "trackingNumber", column = @Column(name = "clm_line_pa_uniq_trkng_num"))
public class ClaimLineInstitutionalSharedSystems extends ClaimLineInstitutionalBase {

  @Column(name = "clm_ddctbl_coinsrnc_cd")
  private Optional<ClaimLineDeductibleCoinsuranceCode> deductibleCoinsuranceCode;

  @Embedded private ClaimLineAdjudicationChargeInstitutionalSharedSystems adjudicationCharge;
  @Embedded private ClaimLineInstitutionalExtensions institutionalExtensions;

  @Override
  protected void addAdjudication(ExplanationOfBenefit.ItemComponent line) {
    adjudicationCharge.toFhir().forEach(line::addAdjudication);
  }

  @Override
  protected void addExtensions(ExplanationOfBenefit.ItemComponent line) {
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
