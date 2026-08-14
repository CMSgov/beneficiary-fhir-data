package gov.cms.bfd.server.ng.claim.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

// todo, applies only to institutional claims, so rename file
class AdjudicationChargeClaimValue {
  private AdjudicationChargeClaimValue() {}

  @AllArgsConstructor
  enum AdjudicationValueCodes {
    OPRTNL_DSPRPRTNT("18", AdjudicationChargeType.OPERATING_DISPROPORTIONATE_SHARE_AMOUNT),
    OPRTNL_IME("19", AdjudicationChargeType.OPERATING_INDIRECT_MEDICAL_EDUCATION_AMOUNT),
    INSTNL_PRFNL("05", AdjudicationChargeType.PROFESSIONAL_COMPONENT_CHARGE_AMOUNT),
    INSTNL_LOW_VOL_PMT("74", AdjudicationChargeType.LOW_VOLUME_PAYMENT_AMOUNT),
    // todo
    // PBP_INCLSN("Q0", "CLM_PBP_INCLSN_AMT", AdjudicationChargeType.CLM_PBP_INCLSN_AMT),
    // PBP_RDCTN("Q1", "CLM_PBP_RDCTN_AMT", AdjudicationChargeType.CLM_PBP_RDCTN_AMT),
    // OPRTNL_OUTLR("17", "CLM_OPRTNL_OUTLR_AMT", AdjudicationChargeType.CLM_OPRTNL_OUTLR_AMT),
    // MDCR_NEW_TECH("77", "CLM_MDCR_NEW_TECH_AMT", AdjudicationChargeType.CLM_MDCR_NEW_TECH_AMT),
    // SQSTRTN_RDCTN("73", "CLM_SQSTRTN_RDCTN_AMT", AdjudicationChargeType.CLM_SQSTRTN_RDCTN_AMT),
    // MIPS_PMT("QM", "CLM_MIPS_PMT_AMT", AdjudicationChargeType.CLM_MIPS_PMT_AMT);
    MDCR_IP_BENE_DDCTBL("A1", AdjudicationChargeType.BENE_INPATIENT_DEDUCTIBLE_AMOUNT);

    final String code;
    final AdjudicationChargeType category;
  }

  static List<ExplanationOfBenefit.AdjudicationComponent> toFhir(List<ClaimValue> claimValues) {

    var claimValueAmounts = new HashMap<AdjudicationValueCodes, BigDecimal>();

    for (AdjudicationValueCodes valueCode : AdjudicationValueCodes.values()) {
      claimValueAmounts.put(
          valueCode, mapSum(claimValues.stream().map(c -> c.getClaimValueAmount(valueCode.code))));
    }

    return claimValueAmounts.entrySet().stream()
        .map(cv -> cv.getKey().category.toFhirAdjudication(cv.getValue()))
        .toList();
  }

  private static BigDecimal mapSum(Stream<Optional<BigDecimal>> inputStream) {
    return inputStream
        .flatMap(Optional::stream)
        .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
  }
}
