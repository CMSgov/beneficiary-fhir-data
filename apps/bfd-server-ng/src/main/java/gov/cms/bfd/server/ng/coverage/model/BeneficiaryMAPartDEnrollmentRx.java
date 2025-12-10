package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.jetbrains.annotations.NotNull;

/** Entity representing BeneficiaryMAPartDEnrollmentRx. */
@Entity
@Getter
@EqualsAndHashCode
@Table(name = "beneficiary_ma_part_d_enrollment_rx", schema = "idr")
public class BeneficiaryMAPartDEnrollmentRx implements Comparable<BeneficiaryMAPartDEnrollmentRx> {

  @EmbeddedId private BeneficiaryMAPartDEnrollmentRxId id;

  @Column(name = "bene_enrlmt_bgn_dt")
  private LocalDate enrollmentBeginDate;

  @Column(name = "bene_cntrct_num")
  private String contractNumber;

  @Column(name = "bene_pbp_num")
  private Optional<String> drugPlanNumber;

  @Column(name = "bene_pdp_enrlmt_mmbr_id_num")
  private Optional<String> memberId;

  @Column(name = "bene_pdp_enrlmt_grp_num")
  private Optional<String> groupNumber;

  @Column(name = "bene_pdp_enrlmt_prcsr_num")
  private Optional<String> processorNumber;

  @Column(name = "bene_pdp_enrlmt_bank_id_num")
  private Optional<String> bankId;

  @Column(name = "cntrct_pbp_sk")
  private long contractPbpSk;

  @Column(name = "bfd_updated_ts")
  private ZonedDateTime bfdUpdatedTimestamp;

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

  @Override
  public int compareTo(@NotNull BeneficiaryMAPartDEnrollmentRx o) {
    return this.id.compareTo(o.id);
  }
}
