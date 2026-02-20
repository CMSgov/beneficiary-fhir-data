package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Period;

/** Entitlement period. */
@Getter
@Embeddable
public class BeneficiaryEntitlementPeriod {
  @Column(name = "bene_rng_bgn_dt")
  private LocalDate benefitRangeBeginDate;

  @Column(name = "original_bgn_dt")
  private LocalDate originalBeginDate;

  @Column(name = "bene_rng_end_dt")
  private Optional<LocalDate> benefitRangeEndDate;

  Period toFhirPeriod() {
    var period = new Period().setStartElement(DateUtil.toFhirDate(originalBeginDate));
    benefitRangeEndDate.ifPresent(d -> period.setEndElement(DateUtil.toFhirDate(d)));
    return period;
  }

  Coverage.CoverageStatus toFhirStatus(Clock clock) {
    if (benefitRangeEndDate.isPresent()
        && benefitRangeEndDate.get().isBefore(DateUtil.nowAoe(clock.instant()))) {
      return Coverage.CoverageStatus.CANCELLED;
    }

    return Coverage.CoverageStatus.ACTIVE;
  }
}
