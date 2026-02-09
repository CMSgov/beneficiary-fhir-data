package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

/** Claim line info. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineInstitutionalNch extends ClaimLineInstitutionalBase {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_line_num", insertable = false, updatable = false)
  private Optional<Integer> claimLineNumber;

  @Column(name = "clm_line_rev_ctr_cd")
  private Optional<ClaimLineRevenueCenterCode> revenueCenterCode;

  @Column(name = "clm_line_pmd_uniq_trkng_num")
  private Optional<String> adjudicatedTrackingNumber;

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineHcpcsCode hcpcsCode;
  @Embedded private ClaimLineHcpcsModifierCode hcpcsModifierCode;
  @Embedded private ClaimLineAdjudicationChargeInstitutionalNch adjudicationCharge;
  @Embedded private ClaimAnsiSignatureInfo ansiSignature;

  Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent() {
    if (claimLineNumber.isEmpty()) {
      return Optional.empty();
    }
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(claimLineNumber.get());

    var productOrService = new CodeableConcept();
    hcpcsCode.toFhir().ifPresent(productOrService::addCoding);
    getHippsCode().toFhir().ifPresent(productOrService::addCoding);

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));

    revenueCenterCode.ifPresent(
        c -> {
          var revenueCoding = c.toFhir(getDeductibleCoinsuranceCode());
          line.setRevenue(revenueCoding);
        });

    line.addModifier(hcpcsModifierCode.toFhir());
    getRevenueCenterDate().ifPresent(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    adjudicationCharge.toFhir().forEach(line::addAdjudication);

    ansiSignature.toFhir().ifPresent(line::addAdjudication);

    line.setExtension(getExtensions().toFhir());

    return Optional.of(line);
  }

  Optional<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {

    var trackingNumber = adjudicatedTrackingNumber;

    if (trackingNumber.isEmpty()) {
      return Optional.empty();
    }

    var category = BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM;

    return Optional.of(
        supportingInfoFactory
            .createSupportingInfo()
            .setCategory(category.toFhir())
            .setValue(new StringType(trackingNumber.get())));
  }
}
