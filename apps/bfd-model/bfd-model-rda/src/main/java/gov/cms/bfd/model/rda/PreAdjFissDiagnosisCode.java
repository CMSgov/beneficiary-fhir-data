package gov.cms.bfd.model.rda;

import java.io.Serializable;
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
@IdClass(PreAdjFissProcCode.PK.class)
@Table(name = "`FissDiagnosisCodes`", schema = "`pre_adj`")
public class PreAdjFissDiagnosisCode {
  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "`diagCd2`", length = 7, nullable = false)
  private String diagCd2;

  @Column(name = "`diagPoaInd`", length = 1, nullable = false)
  private String diagPoaInd;

  @Column(name = "`bitFlags`", length = 4)
  private String bitFlags;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  /* PK class for the FissDiagnosisCodes table */
  public static class PK implements Serializable {
    private String dcn;
    private short priority;
  }
}
