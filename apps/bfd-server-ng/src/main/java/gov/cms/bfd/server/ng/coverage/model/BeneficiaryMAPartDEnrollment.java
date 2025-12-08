package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;

/** Entity representing BeneficiaryMAPartDEnrollment. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_ma_part_d_enrollment", schema = "idr")
public class BeneficiaryMAPartDEnrollment {

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

  @OneToOne
  @JoinColumn(
      name = "bene_cntrct_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_num")
  @JoinColumn(
      name = "bene_pbp_num",
      insertable = false,
      updatable = false,
      referencedColumnName = "cntrct_pbp_num")
  private Contract contract;

  Period toFhirPeriod() {
    return beneficiaryEnrollmentPeriod.toFhirPeriod();
  }

  Coverage.CoverageStatus toFhirStatus() {
    return beneficiaryEnrollmentPeriod.toFhirStatus();
  }

  /**
   * Create contract, plan, coverage type, contract segment, and employer subsidy switch extensions.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    var extContractNumber =
        contractNumber.map(
            number ->
                new Extension(SystemUrls.EXT_BENE_CNTRCT_NUM_URL).setValue(new StringType(number)));

    var extDrugPlanNumber =
        drugPlanNumber.map(
            number ->
                new Extension(SystemUrls.EXT_BENE_PBP_NUM_URL).setValue(new StringType(number)));

    var extCoverageTypeCode =
        coverageTypeCode.map(
            code ->
                new Extension(SystemUrls.EXT_BENE_CVRG_TYPE_CD_URL)
                    .setValue(new Coding(SystemUrls.SYS_CVRG_TYPE_CD, code, null)));

    var extSegmentNumber =
        contract
            .getContractPbpSegmentNumber()
            .map(
                number ->
                    new Extension(SystemUrls.EXT_CNTRCT_PBP_SGMT_NUM_URL)
                        .setValue(new StringType(number)));

    var extEmployerSubsidySwitch =
        employerSubsidySwitch.map(
            number ->
                new Extension(SystemUrls.EXT_BENE_ENRLMT_EMPLR_SBSDY_SW_URL)
                    .setValue(new StringType(number)));

    return Stream.of(
            extContractNumber,
            extDrugPlanNumber,
            extCoverageTypeCode,
            extSegmentNumber,
            extEmployerSubsidySwitch)
        .flatMap(Optional::stream)
        .toList();
  }
}
