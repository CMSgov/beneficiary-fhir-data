package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineBase;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineNdcQuantity;
import gov.cms.bfd.server.ng.claim.model.common.ClaimLineServiceUnitQuantity;
import gov.cms.bfd.server.ng.claim.model.common.RenderingCareTeamLine;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.FhirUtil;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim line info base. */
@Embeddable
@Getter
@SuppressWarnings("java:S2201")
public class ClaimLineRx implements ClaimLineBase {

  @Embedded private ClaimLineNdcQuantity ndc;
  @Embedded private ClaimLineServiceUnitQuantity serviceUnitQuantity;
  @Embedded private ClaimLineSupportingInfoRx supportingInfo;

  @Override
  public Optional<ExplanationOfBenefit.ItemComponent> toFhirItemComponent(
      ClaimFilterOptions options) {
    var line = new ExplanationOfBenefit.ItemComponent();
    line.setSequence(1);
    var productOrService = new CodeableConcept();
    var quantity = serviceUnitQuantity.toFhir();
    supportingInfo.toFhirNdcCompound().ifPresent(productOrService::addCoding);

    if (productOrService.isEmpty()) {
      ndc.toFhirCoding().ifPresent(productOrService::addCoding);
      ndc.getQualifier().ifPresent(quantity::setUnit);
    }

    line.setProductOrService(FhirUtil.checkDataAbsent(productOrService));
    ndc.toFhirDetail().ifPresent(line::addDetail);
    line.setQuantity(quantity);
    return Optional.of(line);
  }

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhirSupportingInfo(
      SupportingInfoFactory supportingInfoFactory) {
    return supportingInfo.toFhir(supportingInfoFactory);
  }

  @Override
  public Optional<RenderingCareTeamLine> getClaimLineRenderingProvider() {
    return Optional.empty();
  }

  @Override
  public Optional<Integer> getClaimLineNumber() {
    return Optional.empty();
  }
}
