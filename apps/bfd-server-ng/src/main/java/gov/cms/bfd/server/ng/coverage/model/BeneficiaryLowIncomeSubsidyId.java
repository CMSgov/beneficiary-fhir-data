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

/** Represents the composite primary key for the {@link BeneficiaryLowIncomeSubsidy} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryLowIncomeSubsidyId
    implements Serializable, Comparable<BeneficiaryLowIncomeSubsidyId> {

  @Column(name = "bene_sk")
  private long beneSk;

  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Override
  public int compareTo(@NotNull BeneficiaryLowIncomeSubsidyId o) {
    return Comparator.comparing((BeneficiaryLowIncomeSubsidyId id) -> id.beneSk)
        .thenComparing(id -> id.benefitRangeBeginDate, Comparator.reverseOrder())
        .compare(this, o);
  }
}
