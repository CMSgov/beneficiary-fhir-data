package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Entity representing BeneficiaryMappedEnrollment. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_mapped_enrollment", schema = "idr")
public class BeneficiaryMappedEnrollment {

  @EmbeddedId private BeneficiaryEnrollmentId id;

  private BeneficiaryEnrollmentPeriod beneficiaryEnrollmentPeriod;

  @Column(name = "bene_cvrg_type_cd")
  private Optional<String> coverageTypeCode;

  @Column(name = "bene_enrlmt_emplr_sbsdy_sw")
  private Optional<String> employerSubsidySwitch;

  @Column(name = "bene_cntrct_num")
  private Optional<String> contractNumber;

  @Column(name = "bene_pbp_num")
  private Optional<String> drugPlanNumber;

  Period toFhirPeriod() {
    return beneficiaryEnrollmentPeriod.toFhirPeriod();
  }

  Coverage.CoverageStatus toFhirStatus() {
    return beneficiaryEnrollmentPeriod.toFhirStatus();
  }

  /**
   * Create contract, plan, and coverage type extension.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    var extContractNumber =
        contractNumber.map(
            validCode ->
                new Extension(SystemUrls.EXT_BENE_CNTRCT_NUM_URL)
                    .setValue(new StringType(validCode)));

    var extDrugPlanNumber =
        drugPlanNumber.map(
            validCode ->
                new Extension(SystemUrls.EXT_BENE_PBP_NUM_URL).setValue(new StringType(validCode)));

    var extCoverageTypeCode =
        coverageTypeCode.map(
            validCode ->
                new Extension(SystemUrls.EXT_BENE_CVRG_TYPE_CD_URL)
                    .setValue(new Coding(SystemUrls.SYS_CVRG_TYPE_CD, validCode, null)));

    return Stream.of(extContractNumber, extDrugPlanNumber, extCoverageTypeCode)
        .flatMap(Optional::stream)
        .toList();
  }
}
