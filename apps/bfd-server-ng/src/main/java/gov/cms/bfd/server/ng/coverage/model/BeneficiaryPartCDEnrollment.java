package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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

  private BeneficiaryEnrollmentPeriod beneficiaryEnrollmentPeriod;

  @Column(name = "bene_enrlmt_pdp_rx_info_bgn_dt")
  private LocalDate enrollmentPdpRxInfoBeginDate;

  @Column(name = "bene_cvrg_type_cd")
  private String coverageTypeCode;

  @Column(name = "bene_enrlmt_emplr_sbsdy_sw")
  private String employerSubsidySwitch;

  @ManyToOne(fetch = FetchType.EAGER)
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
  private Contract enrollmentContract;

  @Column(name = "enr_bfd_updated_ts")
  private ZonedDateTime enrollmentBfdUpdatedTimestamp;

  @Column(name = "bene_pdp_enrlmt_mmbr_id_num")
  private String memberId;

  @Column(name = "bene_pdp_enrlmt_grp_num")
  private String groupNumber;

  @Column(name = "bene_pdp_enrlmt_prcsr_num")
  private String processorNumber;

  @Column(name = "bene_pdp_enrlmt_bank_id_num")
  private String bankId;

  @Column(name = "enr_rx_bfd_updated_ts")
  private ZonedDateTime enrollmentRxBfdUpdatedTimestamp;

  /**
   * Gets the coverage type code.
   *
   * @return the coverage type code.
   */
  public Optional<String> getCoverageTypeCode() {
    return Optional.ofNullable(coverageTypeCode);
  }

  /**
   * Gets the employer subsidy switch.
   *
   * @return the employer subsidy switch.
   */
  public Optional<String> getEmployerSubsidySwitch() {
    return Optional.ofNullable(employerSubsidySwitch);
  }

  /**
   * Gets the member id.
   *
   * @return the member id.
   */
  public Optional<String> getMemberId() {
    return Optional.ofNullable(memberId);
  }

  /**
   * Gets the group number.
   *
   * @return the group number.
   */
  public Optional<String> getGroupNumber() {
    return Optional.ofNullable(groupNumber);
  }

  /**
   * Gets the processor number.
   *
   * @return the processor number.
   */
  public Optional<String> getProcessorNumber() {
    return Optional.ofNullable(processorNumber);
  }

  /**
   * Gets the bank id.
   *
   * @return the bank id.
   */
  public Optional<String> getBankId() {
    return Optional.ofNullable(bankId);
  }

  /**
   * Gets the enrollment rx bfd updated timestamp.
   *
   * @return the enrollment rx bfd updated timestamp.
   */
  public Optional<ZonedDateTime> getEnrollmentRxBfdUpdatedTimestamp() {
    return Optional.ofNullable(enrollmentRxBfdUpdatedTimestamp);
  }

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
        getMemberId()
            .map(
                pdpEnrlmtMbrIdNum ->
                    new Coverage.ClassComponent()
                        .setType(
                            new CodeableConcept()
                                .addCoding(new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxid", null)))
                        .setValue(pdpEnrlmtMbrIdNum));

    var groupNumberClass =
        getGroupNumber()
            .map(
                pdpEnrlmtGrpNum ->
                    new Coverage.ClassComponent()
                        .setType(
                            new CodeableConcept()
                                .addCoding(
                                    new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxgroup", null)))
                        .setValue(pdpEnrlmtGrpNum));

    var processorNumberClass =
        getProcessorNumber()
            .map(
                pdpEnrlmtPrcsrNum ->
                    new Coverage.ClassComponent()
                        .setType(
                            new CodeableConcept()
                                .addCoding(
                                    new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxpcn", null)))
                        .setValue(pdpEnrlmtPrcsrNum));

    var bankIdClass =
        getBankId()
            .map(
                pdpEnrlmtBankIdNum ->
                    new Coverage.ClassComponent()
                        .setType(
                            new CodeableConcept()
                                .addCoding(
                                    new Coding(SystemUrls.SYS_COVERAGE_CLASS, "rxbin", null)))
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
        getCoverageTypeCode()
            .map(
                code ->
                    new Extension(SystemUrls.EXT_BENE_CVRG_TYPE_CD_URL)
                        .setValue(new Coding(SystemUrls.SYS_CVRG_TYPE_CD, code, null)));

    var extSegmentNumber =
        enrollmentContract
            .getContractPbpSegmentNumber()
            .map(
                number ->
                    new Extension(SystemUrls.EXT_CNTRCT_PBP_SGMT_NUM_URL)
                        .setValue(new StringType(number)));

    var extEmployerSubsidySwitch =
        getEmployerSubsidySwitch()
            .map(
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
