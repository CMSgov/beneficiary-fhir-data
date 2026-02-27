package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Embeddable
@Getter
class ClaimLineRxSupportingInfo {

  @Column(name = "clm_phrmcy_srvc_type_cd")
  private Optional<PharmacySrvcTypeCode> pharmacyServiceTypeCode;

  @Column(name = "clm_line_rx_orgn_cd")
  private Optional<ClaimPrescriptionOriginCode> claimPrescriptionOriginCode;

  @Column(name = "clm_brnd_gnrc_cd")
  private Optional<ClaimLineBrandGenericCode> brandGenericCode;

  @Column(name = "clm_ptnt_rsdnc_cd")
  private Optional<ClaimPatientResidenceCode> claimPatientResidenceCode;

  @Column(name = "clm_ltc_dspnsng_mthd_cd")
  private Optional<ClaimSubmissionCode> claimSubmissionCode;

  @Column(name = "clm_cmpnd_cd")
  private Optional<ClaimLineCompoundCode> compoundCode;

  @Embedded private ClaimLineRxDaysSupplyQuantity daysSupply;
  @Embedded private ClaimLineRxFillNumber fillNumber;
  @Embedded private ClaimDispenseAsWrittenProdSelectCode claimDispenseAsWrittenProdSelectCode;

  @Column(name = "clm_drug_cvrg_stus_cd")
  private Optional<DrugCoverageStatusCode> drugCoverageStatusCode;

  @Column(name = "clm_ctstrphc_cvrg_ind_cd")
  private Optional<CatastrophicCoverageCode> catastrophicCovCode;

  List<ExplanationOfBenefit.SupportingInformationComponent> toFhir(
      SupportingInfoFactory supportingInfoFactory) {
    return Stream.of(
            pharmacyServiceTypeCode.map(c -> c.toFhir(supportingInfoFactory)),
            claimPrescriptionOriginCode.map(c -> c.toFhir(supportingInfoFactory)),
            brandGenericCode.map(s -> s.toFhir(supportingInfoFactory)),
            claimPatientResidenceCode.map(s -> s.toFhir(supportingInfoFactory)),
            claimSubmissionCode.map(s -> s.toFhir(supportingInfoFactory)),
            compoundCode.map(s -> s.toFhir(supportingInfoFactory)),
            Optional.of(daysSupply.toFhir(supportingInfoFactory)),
            Optional.of(fillNumber.toFhir(supportingInfoFactory)),
            claimDispenseAsWrittenProdSelectCode.toFhir(supportingInfoFactory),
            drugCoverageStatusCode.map(s -> s.toFhir(supportingInfoFactory)),
            catastrophicCovCode.map(s -> s.toFhir(supportingInfoFactory)))
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Per C4BB, if compound code = 2 -> populate productOrService with "compound".
   *
   * @return Optional containing the coding if applicable, otherwise empty
   */
  public Optional<Coding> toFhirNdcCompound() {
    return compoundCode
        .filter(c -> c == ClaimLineCompoundCode._2)
        .map(c -> new Coding().setSystem(SystemUrls.CARIN_COMPOUND_LITERAL).setCode("compound"));
  }
}
