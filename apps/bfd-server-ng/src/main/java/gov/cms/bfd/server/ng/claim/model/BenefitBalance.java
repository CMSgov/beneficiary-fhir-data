package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

class BenefitBalance {
  @Column(name = "clm_mdcr_ddctbl_amt")
  private double deductibleAmount;

  @Column(name = "clm_mdcr_coinsrnc_amt")
  private double coinsuranceAmount;

  @Column(name = "clm_blood_lblty_amt")
  private double bloodLiabilityAmount;

  @Column(name = "clm_ncvrd_chrg_amt")
  private double noncoveredChargeAmount;

  ExplanationOfBenefit.BenefitBalanceComponent toFhir(
      BenefitBalanceInstitutional benefitBalanceInstitutional, List<ClaimValue> claimValues) {
    return new ExplanationOfBenefit.BenefitBalanceComponent()
        .setCategory(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.HL7_BENEFIT_CATEGORY)
                    .setCode("1")
                    .setDisplay("Medical Care")))
        .setFinancial(
            Stream.of(
                    toFhirBenefits(),
                    benefitBalanceInstitutional.toFhir(),
                    BenefitBalanceClaimValue.toFhir(claimValues))
                .flatMap(Collection::stream)
                .toList());
  }

  List<ExplanationOfBenefit.BenefitComponent> toFhirBenefits() {
    return List.of(
        BenefitBalanceInstitutionalType.CLM_MDCR_DDCTBL_AMT.toFhirMoney(deductibleAmount),
        BenefitBalanceInstitutionalType.CLM_MDCR_COINSRNC_AMT.toFhirMoney(coinsuranceAmount),
        BenefitBalanceInstitutionalType.CLM_BLOOD_LBLTY_AMT.toFhirMoney(bloodLiabilityAmount),
        BenefitBalanceInstitutionalType.CLM_NCVRD_CHRG_AMT.toFhirMoney(noncoveredChargeAmount));
  }
}
