package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class ClaimProfessionalSupportingInfo {
  @Column(name = "clm_carr_pmt_dnl_cd")
  private Optional<ClaimPaymentDenialCode> claimPaymentDenialCode;

  @Column(name = "clm_mdcr_prfnl_prvdr_asgnmt_sw")
  private Optional<ProviderAssignmentIndicatorSwitch> providerAssignmentIndicatorSwitch;

  @Embedded ClinicalTrialNumber clinicalTrialNumber;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            claimPaymentDenialCode.map(c -> c.toFhir(supportingInfoFactory)),
            providerAssignmentIndicatorSwitch.map(c -> c.toFhir(supportingInfoFactory)),
            clinicalTrialNumber.toFhir(supportingInfoFactory))
        .flatMap(Optional::stream)
        .toList();
  }
}
