package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Period;

/** Entitlement period. */
@Getter
@Embeddable
public class BeneficiaryEnrollmentPeriod {
  @Column(name = "bene_enrlmt_bgn_dt", insertable = false, updatable = false)
  private LocalDate enrollmentBeginDate;

  @Column(name = "bene_enrlmt_end_dt")
  private LocalDate enrollmentEndDate;

  Period toFhirPeriod() {
    var period = new Period().setStartElement(DateUtil.toFhirDate(enrollmentBeginDate));
    final LocalDate defaultMaxDate = LocalDate.of(9999, 12, 31);
    if (enrollmentEndDate != null && enrollmentEndDate.isBefore(defaultMaxDate)) {
      period.setEndElement(DateUtil.toFhirDate(enrollmentEndDate));
    }
    return period;
  }

  Coverage.CoverageStatus toFhirStatus() {
    LocalDate today = LocalDate.now();
    if (today.isBefore(enrollmentBeginDate) || today.isAfter(enrollmentEndDate)) {
      return Coverage.CoverageStatus.CANCELLED;
    }

    return Coverage.CoverageStatus.ACTIVE;
  }
}
