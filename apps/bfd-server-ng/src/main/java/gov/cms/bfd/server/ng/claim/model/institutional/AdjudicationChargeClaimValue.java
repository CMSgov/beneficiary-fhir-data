package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.AdjudicationChargeType;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim Value codes for Adjudication elements. */
public class AdjudicationChargeClaimValue {
  private AdjudicationChargeClaimValue() {}

  @AllArgsConstructor
  enum AdjudicationClaimValueCodes {
    INSTNL_PRFNL("05", AdjudicationChargeType.PROFESSIONAL_COMPONENT_CHARGE_AMOUNT),
    OPRTNL_OUTLR("17", AdjudicationChargeType.OPERATING_OUTLIER_AMOUNT),
    OPRTNL_DSPRPRTNT("18", AdjudicationChargeType.OPERATING_DISPROPORTIONATE_SHARE_AMOUNT),
    OPRTNL_IME("19", AdjudicationChargeType.OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT),
    SQSTRTN_RDCTN("73", AdjudicationChargeType.SEQUESTRATION_REDUCTION_AMOUNT),
    INSTNL_LOW_VOL_PMT("74", AdjudicationChargeType.LOW_VOLUME_PAYMENT_AMOUNT),
    MDCR_NEW_TECH("77", AdjudicationChargeType.NEW_TECH_PAYMENT_AMOUNT),
    MDCR_IP_BENE_DDCTBL("A1", AdjudicationChargeType.BENE_INPATIENT_DEDUCTIBLE_AMOUNT),
    PBP_INCLSN("Q0", AdjudicationChargeType.PBP_INCLUSION_AMOUNT),
    PBP_RDCTN("Q1", AdjudicationChargeType.PBP_REDUCTION_AMOUNT),
    MIPS_PMT("QM", AdjudicationChargeType.MIPS_PAYMENT_AMOUNT);

    final String code;
    final AdjudicationChargeType category;
  }

  /**
   * Finds all the values for each code present in the claim values, sums these values per code, and
   * then processes these into {@link AdjudicationComponent} elements.
   *
   * @param claimValues all the {@link ClaimValues} db records.
   * @return a list of the resulting {@link AdjudicationComponent} elements.
   */
  public static List<ExplanationOfBenefit.AdjudicationComponent> toFhir(
      List<ClaimValue> claimValues) {
    var sumsByCode = new HashMap<String, BigDecimal>();

    for (ClaimValue claimValue : claimValues) {
      claimValue
          .getClaimValueCode()
          .ifPresent(
              claimValueCode ->
                  claimValue
                      .getClaimValueAmount(claimValueCode)
                      .ifPresent(
                          claimValueAmount ->
                              sumsByCode.merge(claimValueCode, claimValueAmount, BigDecimal::add)));
    }

    return Arrays.stream(AdjudicationClaimValueCodes.values())
        .filter(valueCode -> sumsByCode.containsKey(valueCode.code))
        .map(valueCode -> valueCode.category.toFhirAdjudication(sumsByCode.get(valueCode.code)))
        .toList();
  }
}
