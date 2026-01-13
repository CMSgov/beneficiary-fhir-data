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
class ClaimInstitutionalSupportingInfo {
  @Column(name = "clm_admsn_src_cd")
  private Optional<ClaimAdmissionSourceCode> claimAdmissionSourceCode;

  @Column(name = "bene_ptnt_stus_cd")
  private Optional<PatientStatusCode> patientStatusCode;

  @Column(name = "clm_admsn_type_cd")
  private Optional<ClaimAdmissionTypeCode> claimAdmissionTypeCode;

  @Column(name = "clm_mdcr_instnl_mco_pd_sw")
  private Optional<McoPaidSwitch> mcoPaidSwitch;

  @Column(name = "clm_hha_lup_ind_cd")
  private Optional<HhaLupaIndicatorCode> hhaLupaIndicatorCode;

  @Column(name = "clm_hha_rfrl_cd")
  private Optional<HhaReferralCode> hhaReferralCode;

  @Column(name = "clm_pps_ind_cd")
  private Optional<PpsIndicatorCode> ppsIndicatorCode;

  @Column(name = "clm_mdcr_npmt_rsn_cd")
  private Optional<ClaimNonpaymentReasonCode> nonpaymentReasonCode;

  @Column(name = "clm_fi_actn_cd")
  private Optional<ClaimFiscalIntermediaryActionCode> claimFiscalIntermediaryActionCode;

  @Column(name = "clm_op_srvc_type_cd")
  private Optional<ClaimOutpatientServiceTypeCode> claimOutpatientServiceTypeCode;

  @Embedded private DiagnosisDrgCode diagnosisDrgCode;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            claimAdmissionSourceCode.map(c -> c.toFhir(supportingInfoFactory)),
            patientStatusCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimAdmissionTypeCode.map(c -> c.toFhir(supportingInfoFactory)),
            mcoPaidSwitch.map(s -> s.toFhir(supportingInfoFactory)),
            diagnosisDrgCode.toFhir(supportingInfoFactory),
            hhaLupaIndicatorCode.map(s -> s.toFhir(supportingInfoFactory)),
            hhaReferralCode.map(s -> s.toFhir(supportingInfoFactory)),
            ppsIndicatorCode.map(c -> c.toFhir(supportingInfoFactory)),
            nonpaymentReasonCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimFiscalIntermediaryActionCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimOutpatientServiceTypeCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }
}
