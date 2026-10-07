package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.StringType;

@MappedSuperclass
@Getter
abstract class ClaimLineInstitutionalCms extends ClaimLineInstitutionalBase {

  private Optional<String> trackingNumber;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            super.toFhirSupportingInfo(supportingInfoFactory).stream(),
            getTrackingNumberSupportingInfo(supportingInfoFactory).stream())
        .flatMap(Function.identity())
        .toList();
  }

  /**
   * Helper method with logic for professional tracking number mapping.
   *
   * @param supportingInfoFactory the factory of supporting info
   * @return a list of 0..1 supporting info components
   */
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      getTrackingNumberSupportingInfo(SupportingInfoFactory supportingInfoFactory) {
    return trackingNumber
        .map(
            number ->
                supportingInfoFactory
                    .createSupportingInfo()
                    .setCategory(
                        BlueButtonSupportingInfoCategory.CLM_LINE_PMD_UNIQ_TRKNG_NUM.toFhir())
                    .setValue(new StringType(number)))
        .stream()
        .toList();
  }
}
