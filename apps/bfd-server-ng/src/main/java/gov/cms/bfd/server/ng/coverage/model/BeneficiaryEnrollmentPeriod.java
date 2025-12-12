package gov.cms.bfd.server.ng.coverage.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
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
  private Optional<LocalDate> enrollmentEndDate;

  Period toFhirPeriod() {
    var period = new Period().setStartElement(DateUtil.toFhirDate(enrollmentBeginDate));
    enrollmentEndDate.ifPresent(endDate -> period.setEndElement(DateUtil.toFhirDate(endDate)));
    return period;
  }

  Coverage.CoverageStatus toFhirStatus() {
    var today = LocalDate.now();

    if (enrollmentEndDate.isPresent() && today.isAfter(enrollmentEndDate.get())) {
      return Coverage.CoverageStatus.CANCELLED;
    }

    return Coverage.CoverageStatus.ACTIVE;
  }
}
