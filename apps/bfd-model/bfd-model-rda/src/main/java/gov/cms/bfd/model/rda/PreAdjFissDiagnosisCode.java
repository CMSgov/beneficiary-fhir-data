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

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`FissDiagnosisCodes`", schema = "`pre_adj`")
@IdClass(PreAdjFissDiagnosisCode.PK.class)
public class PreAdjFissDiagnosisCode {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`dcn`", nullable = false, length = 23)
  private String dcn;

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`diagCd2`", nullable = false, length = 7)
  private String diagCd2;

  @Column(name = "`diagPoaInd`", nullable = false, length = 1)
  private String diagPoaInd;

  @Column(name = "`bitFlags`", length = 4)
  private String bitFlags;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the FissDiagnosisCodes table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String dcn;

    private short priority;
  }
}
