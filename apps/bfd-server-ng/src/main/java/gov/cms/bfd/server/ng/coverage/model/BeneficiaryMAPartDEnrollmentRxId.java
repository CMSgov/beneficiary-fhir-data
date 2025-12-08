package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/** Represents the composite primary key for the {@link BeneficiaryMAPartDEnrollment} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryMAPartDEnrollmentRxId
    implements Serializable, Comparable<BeneficiaryMAPartDEnrollmentRxId> {

  @Column(name = "bene_sk")
  private Long beneSk;

  @Column(name = "bene_enrlmt_pdp_rx_info_bgn_dt")
  private LocalDate enrollmentPdpRxInfoBeginDate;

  @Override
  public int compareTo(@NotNull BeneficiaryMAPartDEnrollmentRxId o) {
    return Comparator.comparing((BeneficiaryMAPartDEnrollmentRxId id) -> id.beneSk)
        .thenComparing(id -> id.enrollmentPdpRxInfoBeginDate, Comparator.reverseOrder())
        .compare(this, o);
  }
}
