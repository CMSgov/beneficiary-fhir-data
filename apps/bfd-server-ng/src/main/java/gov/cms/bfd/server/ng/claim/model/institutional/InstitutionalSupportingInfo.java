package gov.cms.bfd.server.ng.claim.model.institutional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimAdmissionSourceCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimAdmissionTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.PatientStatusCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoComponentBase;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** The institutional supporting info base. */
@Embeddable
@Getter
public class InstitutionalSupportingInfo implements SupportingInfoComponentBase {
  @Column(name = "clm_admsn_src_cd")
  private Optional<ClaimAdmissionSourceCode> claimAdmissionSourceCode;

  @Column(name = "bene_ptnt_stus_cd")
  private Optional<PatientStatusCode> patientStatusCode;

  @Column(name = "clm_admsn_type_cd")
  private Optional<ClaimAdmissionTypeCode> claimAdmissionTypeCode;

  @Override
  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            claimAdmissionSourceCode.map(c -> c.toFhir(supportingInfoFactory)),
            patientStatusCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimAdmissionTypeCode.map(c -> c.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }
}
