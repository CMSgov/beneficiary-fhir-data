package gov.cms.bfd.model.rif;

import java.util.Date;
import java.util.Optional;

/** Common interface for RifRecords */
public interface RifRecordBase {
  Optional<Date> getLastUpdated();

  void setLastUpdated(Date lastUpdated);
}
