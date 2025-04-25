package gov.cms.bfd.server.ng.types;

import java.time.LocalDateTime;
import java.util.Optional;

public record DateTimeRange(
    Optional<LocalDateTime> lowerBound, Optional<LocalDateTime> upperBound) {

  public DateTimeRange() {
    this(Optional.empty(), Optional.empty());
  }
}
