package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.model.common.ClaimContractorNumber;
import gov.cms.bfd.server.ng.claim.model.common.ClaimIdrLoadDate;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentAmount;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
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

  @Column(name = "clm_cntrctr_num")
  private Optional<ClaimContractorNumber> claimContractorNumber;

  @Embedded private ClaimIdrLoadDate claimIdrLoadDate;
  @Embedded private PaymentComponentAmount paymentComponent;
  @Embedded private ClinicalTrialNumber clinicalTrialNumber;

  @Override
  public Optional<ClaimIdrLoadDate> getClaimIdrLoadDate() {
    return Optional.of(claimIdrLoadDate);
  }

  // region Overrides

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  @Override
  public Optional<ClaimContractorNumber> getClaimContractorNumber() {
    return claimContractorNumber;
  }

  @Override
  protected List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    return clinicalTrialNumber.toFhir(supportingInfoFactory).stream().toList();
  }

  // endregion

}
