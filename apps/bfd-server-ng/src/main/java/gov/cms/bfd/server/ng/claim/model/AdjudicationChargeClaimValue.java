package gov.cms.bfd.server.ng.claim.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class AdjudicationChargeClaimValue {
  private AdjudicationChargeClaimValue() {}

  static List<ExplanationOfBenefit.AdjudicationComponent> toFhir(List<ClaimValue> claimValues) {
    var disproportionateAmount =
        mapSum(claimValues.stream().map(ClaimValue::getDisproportionateAmount));
    var imeAmount = mapSum(claimValues.stream().map(ClaimValue::getImeAmount));

    return List.of(
        AdjudicationChargeType.OPERATING_DISPROPORTIONATE_SHARE_AMOUNT.toFhirAdjudication(
            disproportionateAmount),
        AdjudicationChargeType.OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT.toFhirAdjudication(
            imeAmount));
  }

  private static double mapSum(Stream<Optional<Double>> inputStream) {
    return inputStream.flatMap(Optional::stream).mapToDouble(v -> v).sum();
  }
}
