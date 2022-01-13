package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.time.Instant;
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

/** JPA class for the FissDiagnosisCodes table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PartAdjFissDiagnosisCode.PK.class)
@Table(name = "`FissDiagnosisCodes`", schema = "`part_adj`")
public class PartAdjFissDiagnosisCode {
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

  @Column(name = "`diagPoaInd`", length = 1)
  private String diagPoaInd;

  @Column(name = "`bitFlags`", length = 4)
  private String bitFlags;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the FissDiagnosisCodes table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
