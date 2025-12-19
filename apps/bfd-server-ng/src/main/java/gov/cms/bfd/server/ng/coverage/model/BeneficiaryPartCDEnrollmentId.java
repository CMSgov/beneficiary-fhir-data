package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/** Represents the composite primary key for the {@link BeneficiaryPartCDEnrollment} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
@SuppressWarnings("java:S1948")
public class BeneficiaryPartCDEnrollmentId
    implements Serializable, Comparable<BeneficiaryPartCDEnrollmentId> {
  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_enrlmt_bgn_dt")
  private LocalDate enrollmentBeginDate;

  @Column(name = "bene_enrlmt_pgm_type_cd")
  private Optional<EnrollmentProgramTypeCode> enrollmentProgramTypeCode;

  @Column(name = "bene_cntrct_num")
  private String contractNumber;

  @Column(name = "bene_pbp_num")
  private String planNumber;

  @Column(name = "bene_enrlmt_pdp_rx_info_bgn_dt")
  private LocalDate enrollmentPdpRxInfoBeginDate;

  @Override
  public int compareTo(@NotNull BeneficiaryPartCDEnrollmentId o) {
    return Comparator.comparing((BeneficiaryPartCDEnrollmentId id) -> id.beneSk)
        .thenComparing(id -> id.enrollmentBeginDate, Comparator.reverseOrder())
        .thenComparing(
            id -> id.enrollmentProgramTypeCode.orElse(null),
            Comparator.nullsLast(Comparator.naturalOrder()))
        .thenComparing(id -> id.contractNumber)
        .thenComparing(id -> id.planNumber)
        .thenComparing(id -> id.enrollmentPdpRxInfoBeginDate, Comparator.reverseOrder())
        .compare(this, o);
  }
}
