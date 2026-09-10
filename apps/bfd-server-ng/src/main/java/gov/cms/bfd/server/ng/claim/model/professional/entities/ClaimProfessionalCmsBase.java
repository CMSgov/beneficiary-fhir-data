package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPaymentComponentBase;
import gov.cms.bfd.server.ng.claim.model.professional.ClinicalTrialNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Shared base for CMS profile professional claim types (NCH and Shared Systems). */
@MappedSuperclass
public abstract class ClaimProfessionalCmsBase extends ClaimProfessionalBase {

  // region Claim IDR Load Date
  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  // endregion

  // region PaymentComponent
  @Embedded private ClaimPaymentComponentAmount paymentComponent;

  @Override
  public ClaimPaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  // endregion

  // region Claim Contractor Number
  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Override
  public Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return claimContractorNumber;
  }

  // endregion

  // region Clinical Trial Number
  @Embedded private ClinicalTrialNumber clinicalTrialNumber;

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent>
      buildSubclassSupportingInfo() {
    return clinicalTrialNumber.toFhir(supportingInfoFactory).stream().toList();
  }

  // endregion

}
