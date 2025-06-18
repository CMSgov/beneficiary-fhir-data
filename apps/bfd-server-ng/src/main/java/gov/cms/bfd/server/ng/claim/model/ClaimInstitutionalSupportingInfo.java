package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimInstitutionalSupportingInfo {
  @Column(name = "clm_admsn_src_cd")
  private ClaimAdmissionSourceCode claimAdmissionSourceCode;

  @Column(name = "bene_ptnt_stus_cd")
  private PatientStatusCode patientStatusCode;

  @Column(name = "clm_admsn_type_cd")
  private ClaimAdmissionTypeCode claimAdmissionTypeCode;

  @Column(name = "clm_mdcr_instnl_mco_pd_sw")
  private McoPaidSwitch mcoPaidSwitch;

  @Embedded private DiagnosisDrgCode diagnosisDrgCode;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return List.of(
        claimAdmissionSourceCode.toFhir(supportingInfoFactory),
        patientStatusCode.toFhir(supportingInfoFactory),
        claimAdmissionTypeCode.toFhir(supportingInfoFactory),
        mcoPaidSwitch.toFhir(supportingInfoFactory),
        diagnosisDrgCode.toFhir(supportingInfoFactory));
  }
}
