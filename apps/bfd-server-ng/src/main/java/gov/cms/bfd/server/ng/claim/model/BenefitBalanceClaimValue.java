package gov.cms.bfd.server.ng.claim.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class BenefitBalanceClaimValue {
  private BenefitBalanceClaimValue() {}

  static List<ExplanationOfBenefit.BenefitComponent> toFhir(List<ClaimValue> claimValues) {
    var disproportionateAmount =
        mapSum(claimValues.stream().map(ClaimValue::getDisproportionateAmount));
    var imeAmount = mapSum(claimValues.stream().map(ClaimValue::getImeAmount));

    return List.of(
        BenefitBalanceInstitutionalType.CLM_OPRTNL_DSPRTNT_AMT.toFhirMoney(disproportionateAmount),
        BenefitBalanceInstitutionalType.CLM_OPRTNL_IME_AMT.toFhirMoney(imeAmount));
  }

  private static double mapSum(Stream<Optional<Double>> inputStream) {
    return inputStream.flatMap(Optional::stream).mapToDouble(v -> v).sum();
  }
}
