package gov.cms.bfd.server.ng.claim.model.professional.entities;

import gov.cms.bfd.server.ng.claim.ClaimProfile;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponent;
import gov.cms.bfd.server.ng.claim.model.common.PaymentComponentBase;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Shared base for regular profile professional claims. */
@MappedSuperclass
public abstract class ClaimProfessionalBasisBase extends ClaimProfessionalBase {

  // Basis does not contain an amount field
  @Embedded private PaymentComponent paymentComponent;

  // region Overrides

  @Override
  public PaymentComponentBase getPaymentComponent() {
    return paymentComponent;
  }

  @Override
  List<ExplanationOfBenefit.SupportingInformationComponent> getSubclassSupportingInfo() {
    return List.of();
  }

  @Override
  void addSubclassAdjudication(ExplanationOfBenefit eob) {}

  @Override
  void addSubclassCareTeam(ExplanationOfBenefit eob, SequenceGenerator sequenceGenerator) {}

  @Override
  public ClaimProfile getClaimProfile() {
    return ClaimProfile.BASIS;
  }

  // endregion

}
