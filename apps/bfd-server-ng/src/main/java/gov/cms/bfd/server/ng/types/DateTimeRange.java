package gov.cms.bfd.server.ng.types;

import ca.uhn.fhir.rest.param.DateRangeParam;
import java.time.LocalDateTime;
import java.util.Optional;

public record DateTimeRange(
    Optional<DateTimeBound> lowerBound, Optional<DateTimeBound> upperBound) {

  public DateTimeRange() {
    this(Optional.empty(), Optional.empty());
  }

  public DateTimeRange(DateRangeParam dateRangeParam) {
    this(
        Optional.ofNullable(dateRangeParam.getLowerBound()).map(DateTimeBound::new),
        Optional.ofNullable(dateRangeParam.getUpperBound()).map(DateTimeBound::new));
  }

  public Optional<LocalDateTime> getLowerBoundDateTime() {
    return lowerBound.map(DateTimeBound::bound);
  }

  public Optional<LocalDateTime> getUpperBoundDateTime() {
    return upperBound.map(DateTimeBound::bound);
  }

  public String lowerBoundOperator() {
    return getOperator(lowerBound, ">", ">=");
  }

  public String upperBoundOperator() {
    return getOperator(upperBound, "<", "<=");
  }

  private String getOperator(
      Optional<DateTimeBound> bound, String exclusiveOperator, String inclusiveOperator) {
    return bound
        .map(
            b ->
                switch (b.boundType()) {
                  case DateTimeBoundType.EXLCUSIVE -> exclusiveOperator;
                  case DateTimeBoundType.INCLUSIVE -> inclusiveOperator;
                })
        .orElse("!=");
  }
}
