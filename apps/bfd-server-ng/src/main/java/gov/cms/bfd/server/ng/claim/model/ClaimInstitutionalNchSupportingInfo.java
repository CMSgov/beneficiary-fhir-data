package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class ClaimInstitutionalNchSupportingInfo extends ClaimInstitutionalSupportingInfoBase {

  @Column(name = "clm_hha_lup_ind_cd")
  private Optional<HhaLupaIndicatorCode> hhaLupaIndicatorCode;

  @Column(name = "clm_hha_rfrl_cd")
  private Optional<HhaReferralCode> hhaReferralCode;

  @Column(name = "clm_pps_ind_cd")
  private Optional<PpsIndicatorCode> ppsIndicatorCode;

  @Column(name = "clm_op_srvc_type_cd")
  private Optional<ClaimOutpatientServiceTypeCode> claimOutpatientServiceTypeCode;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            hhaLupaIndicatorCode.map(s -> s.toFhir(supportingInfoFactory)),
            hhaReferralCode.map(s -> s.toFhir(supportingInfoFactory)),
            ppsIndicatorCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimOutpatientServiceTypeCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }
}
