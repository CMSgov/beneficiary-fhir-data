package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjMcsDiagnosisCode.PK.class)
@Table(name = "`McsDiagnosisCodes`", schema = "`pre_adj`")
public class PreAdjMcsDiagnosisCode {
  @Id
  @Column(name = "`idrClmHdIcn`", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "`idrDiagIcdType`", length = 1)
  private String idrDiagIcdType;

  @Column(name = "`idrDiagCode`", length = 7)
  private String idrDiagCode;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  /* PK class for the McsDiagnosisCodes table */
  public static class PK implements Serializable {
    private String idrClmHdIcn;
    private short priority;
  }
}
