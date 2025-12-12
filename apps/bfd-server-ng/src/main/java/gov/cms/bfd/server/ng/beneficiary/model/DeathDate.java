package gov.cms.bfd.server.ng.beneficiary.model;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.bfd.server.ng.converter.DefaultFalseBooleanConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import java.util.TimeZone;
import org.hl7.fhir.r4.model.DateTimeType;

/** Beneficiary death date. */
@Embeddable
public class DeathDate {

  @Column(name = "bene_death_dt")
  private Optional<LocalDate> deathDate;

  @Convert(converter = DefaultFalseBooleanConverter.class)
  @Column(name = "bene_vrfy_death_day_sw")
  private Boolean verifyDeathDate;

  Optional<DateTimeType> toFhir() {
    if (verifyDeathDate) {
      return deathDate.map(
          d ->
              new DateTimeType(
                  DateUtil.toDateAndSanitize(d),
                  TemporalPrecisionEnum.DAY,
                  TimeZone.getTimeZone(DateUtil.ZONE_ID_UTC)));
    } else {
      return Optional.empty();
    }
  }
}
