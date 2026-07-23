package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdc;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineServiceUnitQuantity;
import gov.cms.bfd.server.ng.claim.model.common.RenderingCareTeamLine;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Observation;

/** Claim line info base. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineRx implements ClaimLineBase {

  @Column(name = "clm_line_from_dt")
  private Optional<LocalDate> fromDate;

  @Embedded private ClaimLineNdc ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;

  @Override
  public Optional<Observation> toFhirObservation(int bfdRowId) {
    return Optional.empty();
  }

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {

    return this.toFhirItemComponent(options, Optional.empty());
  }

  /**
   * Helper method to allow Cms to pass along a Coding generated from ClaimLineRxSupportingInfo.
   *
   * @param options options!
   * @param supportingInfo the Coding generated from CMS
   * @return the eob Item Component
   */
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options, Optional<Coding> supportingInfo) {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(1);
    var productOrService = new CodeableConcept();
    var quantity = serviceUnitQuantity.toFhir();
    supportingInfo.ifPresent(productOrService::addCoding);

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);

    fromDate.map(d -> line.setServiced(new DateType(DateUtil.toDate(d))));

    return Optional.of(line);
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of();
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getClaimLineNumber() {
    return Optional.empty();
  }

  @Override
  public Optional<String> getClaimLineDiagnosisCode() {
    return Optional.empty();
  }
}
