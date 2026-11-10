package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.BlueButtonSupportingInfoCategory;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.SimpleQuantity;

/** Claim Value codes for SupportingInfo elements. */
public class SupportingInfoClaimValue {
  private SupportingInfoClaimValue() {}

  @AllArgsConstructor
  enum SupportingInfoValueCode {
    DYLS_MINS("D6", "minute", BlueButtonSupportingInfoCategory.DIALYSIS_MINUTES_AMOUNT);

    final String code;
    final String units;
    final BlueButtonSupportingInfoCategory category;
  }

  /**
   * Finds all the values for each code present in the claim values, sums these values per code, and
   * then processes these into {@link ExplanationOfBenefit.SupportingInformationComponent} elements.
   *
   * @param claimValues all the {@link ClaimValue} db records.
   * @param supportingInfoFactory helper class for building {@link
   *     ExplanationOfBenefit.SupportingInformationComponent}.
   * @return a list of the resulting {@link ExplanationOfBenefit.SupportingInformationComponent}
   *     elements.
   */
  public static List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      List<ClaimValue> claimValues, SupportingInfoFactory supportingInfoFactory) {
    var sumsByCode = new HashMap<String, Integer>();
    claimValues.forEach(
        claimValue ->
            claimValue
                .getClaimValueCode()
                .ifPresent(
                    code ->
                        claimValue
                            .getClaimValueQuantity(code)
                            .ifPresent(qty -> sumsByCode.merge(code, qty, Integer::sum))));

    return Arrays.stream(SupportingInfoValueCode.values())
        .flatMap(
            valueCode ->
                Optional.ofNullable(sumsByCode.get(valueCode.code))
                    .map(
                        quantity ->
                            toFhirSupportingInformation(quantity, valueCode, supportingInfoFactory))
                    .stream())
        .toList();
  }

  static ExplanationOfBenefit.SupportingInformationComponent toFhirSupportingInformation(
      Integer value,
      SupportingInfoValueCode valueCode,
      SupportingInfoFactory supportingInfoFactory) {

    var quantity =
        switch (valueCode.units) {
          case "pint" ->
              new SimpleQuantity()
                  .setValue(value)
                  .setSystem(SystemUrls.UNITS_OF_MEASURE)
                  .setUnit("pint")
                  .setCode("[pt_us]");
          case "minute" ->
              new SimpleQuantity()
                  .setValue(value)
                  .setSystem(SystemUrls.UNITS_OF_MEASURE)
                  .setUnit("minute")
                  .setCode("min");
          default -> throw new IllegalStateException("Not a valid unit: " + valueCode.units);
        };

    return supportingInfoFactory
        .createSupportingInfo()
        .setCategory(valueCode.category.toFhir())
        .setValue(quantity);
  }
}
