package gov.cms.bfd.server.ng.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "load_progress", schema = "idr")
public class LoadProgress {
  @Id
  @Column(name = "table_name")
  String tableName;

  @Column(name = "last_id")
  String lastId;

  @Column(name = "last_ts")
  LocalDateTime lastTimestamp;

  @Column(name = "batch_completion_ts")
  LocalDateTime batchCompletionTimestamp;
}
