package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

// todo, applies only to institutional claims, so rename file
class SupportingInfoClaimValue {
  private SupportingInfoClaimValue() {}

  @AllArgsConstructor
  enum SupportingInfoValueCodes {
    BLOOD_PT("37", BlueButtonSupportingInfoCategory.CLM_BLOOD_PT_FRNSH_QTY);

    final String code;
    final BlueButtonSupportingInfoCategory category;
  }

  static List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      List<ClaimValue> claimValues, SupportingInfoFactory supportingInfoFactory) {

    var claimValueAmounts = new HashMap<SupportingInfoValueCodes, BigDecimal>();

    for (SupportingInfoValueCodes valueCode : SupportingInfoValueCodes.values()) {
      claimValueAmounts.put(
          valueCode, mapSum(claimValues.stream().map(c -> c.getClaimValueAmount(valueCode.code))));
    }

    return claimValueAmounts.entrySet().stream()
        .map(
            cv ->
                toFhirSupportingInformation(
                    cv.getValue(),
                    cv.getKey().category,
                    supportingInfoFactory))
        .toList();
  }

  static ExplanationOfBenefit.SupportingInformationComponent toFhirSupportingInformation(
      BigDecimal value,
      BlueButtonSupportingInfoCategory category,
      SupportingInfoFactory supportingInfoFactory) {
    var bloodQuantity =
        new SimpleQuantity()
            .setValue(value) // convert to non zero int?
            .setSystem(SystemUrls.UNITS_OF_MEASURE)
            .setUnit("pint")
            .setCode("[pt_us]");

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(category.toFhir())
        .setValue(bloodQuantity);
  }

  private static BigDecimal mapSum(Stream<Optional<BigDecimal>> inputStream) {
    return inputStream
        .flatMap(Optional::stream)
        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
  }
}
