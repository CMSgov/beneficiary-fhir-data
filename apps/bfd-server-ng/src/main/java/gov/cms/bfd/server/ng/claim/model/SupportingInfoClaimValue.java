package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

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
    var sumsByCode = new HashMap<String, Integer>();

    for (ClaimValue claimValue : claimValues) {
      claimValue
          .getClaimValueCode()
          .ifPresent(
              claimValueCode ->
                  claimValue
                      .getClaimValueQuantity(claimValueCode)
                      .ifPresent(
                          claimValueAmount ->
                              sumsByCode.merge(claimValueCode, claimValueAmount, Integer::sum)));
    }

    return Arrays.stream(SupportingInfoValueCodes.values())
        .filter(valueCode -> sumsByCode.containsKey(valueCode.code))
        .map(
            valueCode ->
                toFhirSupportingInformation(
                    sumsByCode.get(valueCode.code), valueCode.category, supportingInfoFactory))
        .toList();
  }

  static ExplanationOfBenefit.SupportingInformationComponent toFhirSupportingInformation(
      Integer value,
      BlueButtonSupportingInfoCategory category,
      SupportingInfoFactory supportingInfoFactory) {
    var bloodQuantity =
        new SimpleQuantity()
            .setValue(value)
            .setSystem(SystemUrls.UNITS_OF_MEASURE)
            .setUnit("pint")
            .setCode("[pt_us]");

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(category.toFhir())
        .setValue(bloodQuantity);
  }
}
