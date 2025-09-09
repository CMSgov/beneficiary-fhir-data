package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
class ClaimInstitutionalSupportingInfo {
  @Column(name = "clm_admsn_src_cd")
  private Optional<ClaimAdmissionSourceCode> claimAdmissionSourceCode;

  @Column(name = "bene_ptnt_stus_cd")
  private Optional<PatientStatusCode> patientStatusCode;

  @Column(name = "clm_admsn_type_cd")
  private ClaimAdmissionTypeCode claimAdmissionTypeCode;

  @Column(name = "clm_mdcr_instnl_mco_pd_sw")
  private Optional<McoPaidSwitch> mcoPaidSwitch;

  @Column(name = "clm_hha_lup_ind_cd")
  private Optional<HhaLupaIndicatorCode> hhaLupaIndicatorCode;

  @Column(name = "clm_hha_rfrl_cd")
  private Optional<HhaReferralCode> hhaReferralCode;

  @Embedded private DiagnosisDrgCode diagnosisDrgCode;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            claimAdmissionSourceCode.map(c -> c.toFhir(supportingInfoFactory)),
            patientStatusCode.map(c -> c.toFhir(supportingInfoFactory)),
            Optional.of(claimAdmissionTypeCode.toFhir(supportingInfoFactory)),
            mcoPaidSwitch.map(s -> s.toFhir(supportingInfoFactory)),
            diagnosisDrgCode.toFhir(supportingInfoFactory),
            hhaLupaIndicatorCode.map(s -> s.toFhir(supportingInfoFactory)),
            hhaReferralCode.map(s -> s.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }
}
