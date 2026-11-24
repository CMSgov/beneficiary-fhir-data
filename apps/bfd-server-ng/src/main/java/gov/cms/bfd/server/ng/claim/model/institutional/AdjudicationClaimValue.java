package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Class to handle conversion of claim value amounts into adjudication components. */
public class AdjudicationClaimValue {
  private AdjudicationClaimValue() {}

  @AllArgsConstructor
  enum AdjudicationClaimValueCode {
    OPRTNL_OUTLR("17", AdjudicationChargeType.OPERATING_OUTLIER_AMOUNT),
    OPRTNL_DSPRPRTNT("18", AdjudicationChargeType.OPERATING_DISPROPORTIONATE_SHARE_AMOUNT),
    OPRTNL_IME("19", AdjudicationChargeType.OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT),
    SQSTRTN_RDCTN("73", AdjudicationChargeType.SEQUESTRATION_REDUCTION_AMOUNT),
    INSTNL_LOW_VOL_PMT("74", AdjudicationChargeType.LOW_VOLUME_PAYMENT_AMOUNT),
    MDCR_NEW_TECH("77", AdjudicationChargeType.NEW_TECH_PAYMENT_AMOUNT),
    PBP_INCLSN("Q0", AdjudicationChargeType.PBP_INCLUSION_AMOUNT),
    PBP_RDCTN("Q1", AdjudicationChargeType.PBP_REDUCTION_AMOUNT),
    MIPS_PMT("QM", AdjudicationChargeType.MIPS_PAYMENT_AMOUNT);

    final String code;
    final AdjudicationChargeType category;
  }

  /**
   * Takes a list of claim values and sums by claim value code to generate a list of {@link
   * ExplanationOfBenefit.AdjudicationComponent}.
   *
   * @param claimValues a list of Claim Values from the Leaf entity
   * @return the {@link ExplanationOfBenefit.AdjudicationComponent} elements.
   */
  public static List<ExplanationOfBenefit.AdjudicationComponent> toFhir(
      List<ClaimValue> claimValues) {
    var sumsByCode = new HashMap<String, BigDecimal>();
    claimValues.forEach(
        claimValue ->
            claimValue
                .getClaimValueCode()
                .ifPresent(
                    code ->
                        claimValue
                            .getClaimValueAmount(code)
                            .ifPresent(amount -> sumsByCode.merge(code, amount, BigDecimal::add))));

    return Arrays.stream(AdjudicationClaimValueCode.values())
        .flatMap(
            valueCode ->
                Optional.ofNullable(sumsByCode.get(valueCode.code))
                    .map(amount -> valueCode.category.toFhirAdjudication(amount))
                    .stream())
        .toList();
  }
}
