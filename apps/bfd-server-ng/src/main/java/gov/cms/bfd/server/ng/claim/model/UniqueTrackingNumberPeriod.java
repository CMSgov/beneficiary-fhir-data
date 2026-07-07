package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.Period;

@Getter
@Embeddable
class UniqueTrackingNumberPeriod {
  @Column(name = "utn_valid_st_dt")
  private LocalDate startDate;

  @Column(name = "utn_valid_en_dt")
  private Optional<LocalDate> endDate;

  Period toFhir() {
    var period = new Period();
    period.setStartElement(DateUtil.toFhirDate(startDate));
    endDate.ifPresent(d -> period.setEndElement(DateUtil.toFhirDate(d)));
    return period;
  }
}
