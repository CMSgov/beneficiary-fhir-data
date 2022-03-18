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

/** JPA class for the FissAuditTrails table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjFissAuditTrail.PK.class)
@Table(name = "fiss_audit_trails", schema = "part_adj")
public class PreAdjFissAuditTrail {
  @Id
  @Column(name = "dcn", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "badt_status", length = 1)
  private String badtStatus;

  @Column(name = "badt_loc", length = 5)
  private String badtLoc;

  @Column(name = "badt_oper_id", length = 9)
  private String badtOperId;

  @Column(name = "badt_reas", length = 5)
  private String badtReas;

  @Column(name = "badt_curr_date")
  private LocalDate badtCurrDate;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /** PK class for the FissAuditTrails table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
