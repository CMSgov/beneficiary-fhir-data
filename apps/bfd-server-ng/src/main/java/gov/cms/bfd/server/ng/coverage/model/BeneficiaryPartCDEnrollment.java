package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryPartCDEnrollment. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_part_c_and_d_enrollment", schema = "idr")
public class BeneficiaryPartCDEnrollment implements Comparable<BeneficiaryPartCDEnrollment> {

  @EmbeddedId private BeneficiaryPartCDEnrollmentId id;

  @Embedded private BeneficiaryEnrollmentPeriod beneficiaryEnrollmentPeriod;

  @Column(name = "bene_cvrg_type_cd")
  private Optional<String> coverageTypeCode;

  @Column(name = "bene_enrlmt_emplr_sbsdy_sw")
  private Optional<String> employerSubsidySwitch;

  @Column(name = "bene_pdp_enrlmt_mmbr_id_num")
  private Optional<String> memberId;

  @Column(name = "bene_pdp_enrlmt_grp_num")
  private Optional<String> groupNumber;

  @Column(name = "bene_pdp_enrlmt_prcsr_num")
  private Optional<String> processorNumber;

  @Column(name = "bene_pdp_enrlmt_bank_id_num")
  private Optional<String> bankId;

  @Embedded private BeneficiaryPartCDEnrollmentOptional enrollmentOptional;

  Period toFhirPeriod() {
    return beneficiaryEnrollmentPeriod.toFhirPeriod();
  }

  Coverage.CoverageStatus toFhirStatus() {
    return beneficiaryEnrollmentPeriod.toFhirStatus();
  }

  /**
   * Creates ClassComponents.
   *
   * @return optional classComponents
   */
  public List<Coverage.ClassComponent> toFhirClassComponents() {
    var memberIdClass =
        memberId.map(
            pdpEnrlmtMbrIdNum ->
                new Coverage.ClassComponent()
                    .setType(
                        new CodeableConcept()
                            .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxid", null)))
                    .setValue(pdpEnrlmtMbrIdNum));

    var groupNumberClass =
        groupNumber.map(
            pdpEnrlmtGrpNum ->
                new Coverage.ClassComponent()
                    .setType(
                        new CodeableConcept()
                            .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxgroup", null)))
                    .setValue(pdpEnrlmtGrpNum));

    var processorNumberClass =
        processorNumber.map(
            pdpEnrlmtPrcsrNum ->
                new Coverage.ClassComponent()
                    .setType(
                        new CodeableConcept()
                            .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxpcn", null)))
                    .setValue(pdpEnrlmtPrcsrNum));

    var bankIdClass =
        bankId.map(
            pdpEnrlmtBankIdNum ->
                new Coverage.ClassComponent()
                    .setType(
                        new CodeableConcept()
                            .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxbin", null)))
                    .setValue(pdpEnrlmtBankIdNum));

    return Stream.of(memberIdClass, groupNumberClass, processorNumberClass, bankIdClass)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Create contract, plan, coverage type, contract segment, and employer subsidy switch extensions.
   *
   * @return optional extension
   */
  public List<Extension> toFhirExtensions() {

    var extContractNumber =
        new Extension(SystemUrls.EXT_BENE_CNTRCT_NUM_URL)
            .setValue(new StringType(id.getContractNumber()));

    var extPlanNumber =
        new Extension(SystemUrls.EXT_BENE_PBP_NUM_URL).setValue(new StringType(id.getPlanNumber()));

    var extCoverageTypeCode =
        coverageTypeCode.map(
            code ->
                new Extension(SystemUrls.EXT_BENE_CVRG_TYPE_CD_URL)
                    .setValue(new Coding(SystemUrls.SYS_CVRG_TYPE_CD, code, null)));

    var segment =
        enrollmentOptional.getEnrollmentContract().flatMap(Contract::getContractPbpSegmentNumber);
    var extSegmentNumber =
        segment.map(
            number ->
                new Extension(SystemUrls.EXT_CNTRCT_PBP_SGMT_NUM_URL)
                    .setValue(new StringType(number)));

    var extEmployerSubsidySwitch =
        employerSubsidySwitch.map(
            number ->
                new Extension(SystemUrls.EXT_BENE_ENRLMT_EMPLR_SBSDY_SW_URL)
                    .setValue(new StringType(number)));

    return Stream.of(
            Optional.of(extContractNumber),
            Optional.of(extPlanNumber),
            extCoverageTypeCode,
            extSegmentNumber,
            extEmployerSubsidySwitch)
        .flatMap(Optional::stream)
        .toList();
  }

  @Override
  public int compareTo(@NotNull BeneficiaryPartCDEnrollment o) {
    return this.id.compareTo(o.id);
  }
}
