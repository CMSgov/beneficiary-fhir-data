package gov.cms.bfd.server.ng.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;

/** Table that tracks the IDR load progress for each table. */
@Entity
@Table(name = "load_progress", schema = "idr")
public class LoadProgress {
  @Id
  @Column(name = "table_name")
  String tableName;

  @Column(name = "last_id")
  String lastId;

  @Column(name = "last_ts")
  ZonedDateTime lastTimestamp;

  @Column(name = "batch_complete_ts")
  ZonedDateTime batchCompletionTimestamp;
}
