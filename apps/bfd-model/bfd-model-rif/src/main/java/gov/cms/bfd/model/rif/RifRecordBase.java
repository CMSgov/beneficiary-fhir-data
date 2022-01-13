package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.Optional;

/** Common interface for RifRecords */
public interface RifRecordBase {
  Optional<Instant> getLastUpdated();

  default void setLastUpdated(Instant lastUpdated) {
    setLastUpdated(lastUpdated != null ? Optional.of(lastUpdated) : Optional.empty());
  }

  void setLastUpdated(Optional<Instant> lastUpdated);
}
