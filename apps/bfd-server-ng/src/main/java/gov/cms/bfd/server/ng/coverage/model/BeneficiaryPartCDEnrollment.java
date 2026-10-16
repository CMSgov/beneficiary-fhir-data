package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.model.Contract;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.hl7.fhir.r4.model.*;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryPartCDEnrollment. */
@Entity
@Getter
@EqualsAndHashCode
@Immutable
@Subselect(
"""
 SELECT e.bene_sk,
   e.bene_enrlmt_pgm_type_cd,
   e.bene_enrlmt_bgn_dt,
   e.bene_enrlmt_end_dt,
   e.bene_cntrct_num,
   e.bene_pbp_num,
   e.cntrct_pbp_sk,
   e.bene_cvrg_type_cd,
   e.bene_enrlmt_emplr_sbsdy_sw,
   COALESCE(rx.bene_enrlmt_pdp_rx_info_bgn_dt, '9999-12-31'::date) AS bene_enrlmt_pdp_rx_info_bgn_dt,
   rx.bene_pdp_enrlmt_mmbr_id_num,
   rx.bene_pdp_enrlmt_grp_num,
   rx.bene_pdp_enrlmt_prcsr_num,
   rx.bene_pdp_enrlmt_bank_id_num
  FROM idr.beneficiary_ma_part_d_enrollment e
    LEFT JOIN idr.beneficiary_ma_part_d_enrollment_rx rx ON e.bene_sk = rx.bene_sk AND e.bene_enrlmt_bgn_dt = rx.bene_enrlmt_bgn_dt AND e.cntrct_pbp_sk = rx.cntrct_pbp_sk AND (e.bene_enrlmt_pgm_type_cd::text = ANY (ARRAY['2'::character varying, '3'::character varying]::text[])) AND rx.idr_trans_obslt_ts >= '9999-12-31 00:00:00+00'::timestamp with time zone
 WHERE e.idr_trans_obslt_ts >= '9999-12-31 00:00:00+00'::timestamp with time zone
""")
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

  Coverage.CoverageStatus toFhirStatus(LocalDate benefitDate) {
    return beneficiaryEnrollmentPeriod.toFhirStatus(benefitDate);
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
            code ->
                new Extension(SystemUrls.EXT_BENE_ENRLMT_EMPLR_SBSDY_SW_URL)
                    .setValue(new Coding(SystemUrls.SYS_BENE_ENRLMT_EMPLR_SBSDY_SW, code, null)));

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
