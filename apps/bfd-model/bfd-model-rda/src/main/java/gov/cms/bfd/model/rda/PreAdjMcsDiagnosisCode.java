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

/** JPA class for the McsDiagnosisCodes table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjMcsDiagnosisCode.PK.class)
@Table(name = "mcs_diagnosis_codes", schema = "part_adj")
public class PreAdjMcsDiagnosisCode {
  @Id
  @Column(name = "idr_clm_hd_icn", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "idr_diag_icd_type", length = 1)
  private String idrDiagIcdType;

  @Column(name = "idr_diag_code", length = 7, nullable = false)
  private String idrDiagCode;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /** PK class for the McsDiagnosisCodes table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
