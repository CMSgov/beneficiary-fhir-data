package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class BenefitBalance {
  @Column(name = "clm_mdcr_ddctbl_amt")
  private float deductibleAmount;

  @Column(name = "clm_mdcr_coinsrnc_amt")
  private float coinsuranceAmount;

  @Column(name = "clm_blood_lblty_amt")
  private float bloodLiabilityAmount;

  @Column(name = "clm_ncvrd_chrg_amt")
  private float noncoveredChargeAmount;

  ExplanationOfBenefit.BenefitBalanceComponent toFhir(
      BenefitBalanceInstitutional benefitBalanceInstitutional) {
    return new ExplanationOfBenefit.BenefitBalanceComponent()
        .setCategory(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.HL7_BENEFIT_CATEGORY)
                    .setCode("1")
                    .setDisplay("Medical Care")))
        .setFinancial(
            Stream.of(toFhirBenefits(), benefitBalanceInstitutional.toFhir())
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
