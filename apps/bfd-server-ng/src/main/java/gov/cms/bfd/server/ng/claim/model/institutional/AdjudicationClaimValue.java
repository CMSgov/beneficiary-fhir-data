package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Class to handle conversion of disproportionate and indirect amounts into adjudication components.
 */
public class AdjudicationClaimValue {
  private AdjudicationClaimValue() {}

  /**
   * Takes a list of claim values and calculates two sums to generate two Adjudication Components.
   *
   * @param claimValues a list of Claim Values from the Leaf entity
   * @return the adjudication components
   */
  public static List<ExplanationOfBenefit.AdjudicationComponent> toFhir(
      List<ClaimValue> claimValues) {
    var disproportionateAmount =
        mapSum(claimValues.stream().map(ClaimValue::getDisproportionateAmount));
    var imeAmount = mapSum(claimValues.stream().map(ClaimValue::getImeAmount));

    return List.of(
        AdjudicationChargeType.OPERATING_DISPROPORTIONATE_SHARE_AMOUNT.toFhirAdjudication(
            disproportionateAmount),
        AdjudicationChargeType.OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT.toFhirAdjudication(
            imeAmount));
  }

  private static BigDecimal mapSum(Stream<Optional<BigDecimal>> inputStream) {
    return inputStream
        .flatMap(Optional::stream)
        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
  }
}
