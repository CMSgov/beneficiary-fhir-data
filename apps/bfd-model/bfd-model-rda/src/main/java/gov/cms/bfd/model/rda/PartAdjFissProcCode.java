package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the FissProcCodes table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PartAdjFissProcCode.PK.class)
@Table(name = "fiss_proc_codes", schema = "part_adj")
public class PartAdjFissProcCode {
  @Id
  @Column(name = "dcn", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "proc_code", length = 10, nullable = false)
  private String procCode;

  @Column(name = "proc_flag", length = 4)
  private String procFlag;

  @Column(name = "proc_date")
  private LocalDate procDate;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /** PK class for the FissProcCodes table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
