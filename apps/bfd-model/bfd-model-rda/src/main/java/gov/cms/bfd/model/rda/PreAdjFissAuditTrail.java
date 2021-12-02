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

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`FissAuditTrails`", schema = "`pre_adj`")
@IdClass(PreAdjFissAuditTrail.PK.class)
public class PreAdjFissAuditTrail {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`dcn`", nullable = false, length = 23)
  private String dcn;

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`badtStatus`", length = 1)
  private String badtStatus;

  @Column(name = "`badtLoc`", length = 5)
  private String badtLoc;

  @Column(name = "`badtOperId`", length = 9)
  private String badtOperId;

  @Column(name = "`badtReas`", length = 5)
  private String badtReas;

  @Column(name = "`badtCurrDate`")
  private LocalDate badtCurrDate;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the FissAuditTrails table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
