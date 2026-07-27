package gov.cms.bfd.server.ng.claim.model.rx;

import gov.cms.bfd.server.ng.claim.model.common.CatastrophicCoverageCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimPatientResidenceCode;
import gov.cms.bfd.server.ng.claim.model.common.ClaimSubmissionCode;
import gov.cms.bfd.server.ng.claim.model.common.PharmacySrvcTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.SupportingInfoFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Rx claim line supporting info specific to the CMS profile. */
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
@Embeddable
@Getter
public class ClaimLineRxSupportingInfoCms {

  @Column(name = "clm_phrmcy_srvc_type_cd")
  private Optional<PharmacySrvcTypeCode> pharmacyServiceTypeCode;

  @Column(name = "clm_ptnt_rsdnc_cd")
  private Optional<ClaimPatientResidenceCode> claimPatientResidenceCode;

  @Column(name = "clm_ltc_dspnsng_mthd_cd")
  private Optional<ClaimSubmissionCode> claimSubmissionCode;

  @Column(name = "clm_drug_cvrg_stus_cd")
  private Optional<DrugCoverageStatusCode> drugCoverageStatusCode;

  @Column(name = "clm_ctstrphc_cvrg_ind_cd")
  private Optional<CatastrophicCoverageCode> catastrophicCovCode;

  public List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            pharmacyServiceTypeCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimPatientResidenceCode.map(s -> s.toFhir(supportingInfoFactory)),
            claimSubmissionCode.map(s -> s.toFhir(supportingInfoFactory)),
            drugCoverageStatusCode.map(s -> s.toFhir(supportingInfoFactory)),
            catastrophicCovCode.map(s -> s.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }
}
