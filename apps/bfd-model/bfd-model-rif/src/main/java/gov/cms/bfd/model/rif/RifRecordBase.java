package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.Optional;

/** Common interface for RifRecords */
public interface RifRecordBase {
  Optional<Instant> getLastUpdated();

  void setLastUpdated(Optional<Instant> lastUpdated);
}
