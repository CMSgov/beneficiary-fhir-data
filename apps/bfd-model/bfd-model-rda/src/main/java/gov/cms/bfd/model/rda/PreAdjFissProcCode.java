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
@Table(name = "`FissProcCodes`", schema = "`pre_adj`")
@IdClass(PreAdjFissProcCode.PK.class)
public class PreAdjFissProcCode {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`dcn`", nullable = false, length = 23)
  private String dcn;

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`procCode`", nullable = false, length = 10)
  private String procCode;

  @Column(name = "`procFlag`", length = 4)
  private String procFlag;

  @Column(name = "`procDate`")
  private LocalDate procDate;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the FissProcCodes table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
