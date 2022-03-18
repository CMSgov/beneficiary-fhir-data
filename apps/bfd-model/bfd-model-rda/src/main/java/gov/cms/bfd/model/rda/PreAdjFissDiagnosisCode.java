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
@IdClass(PreAdjFissDiagnosisCode.PK.class)
@Table(name = "fiss_diagnosis_codes", schema = "part_adj")
public class PreAdjFissDiagnosisCode {
  @Id
  @Column(name = "dcn", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "diag_cd2", length = 7, nullable = false)
  private String diagCd2;

  @Column(name = "diag_poa_ind", length = 1)
  private String diagPoaInd;

  @Column(name = "bit_flags", length = 4)
  private String bitFlags;

  @Column(name = "last_updated")
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
