package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.Optional;

/** Common interface for RifRecords. */
public interface RifRecordBase {
  /**
   * Gets the last updated time.
   *
   * @return the last updated time
   */
  Optional<Instant> getLastUpdated();

  /**
   * Sets the last updated time.
   *
   * @param lastUpdated the last updated time
   */
  default void setLastUpdated(Instant lastUpdated) {
    setLastUpdated(lastUpdated != null ? Optional.of(lastUpdated) : Optional.empty());
  }

  /**
   * Sets the last updated time.
   *
   * @param lastUpdated the last updated time
   */
  void setLastUpdated(Optional<Instant> lastUpdated);

  /**
   * Gets the record number of this object in its source RIF file. Valid record numbers start at 1.
   *
   * @return record number
   */
  long getRecordNumber();

  /**
   * Sets the record number of this object in its source RIF file. Valid record numbers start at 1.
   *
   * @param recordNumber record number
   */
  void setRecordNumber(long recordNumber);
}
