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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the PreAdjFissProcCodes table */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjFissProcCode.PK.class)
@Table(name = "`FissProcCodes`", schema = "`pre_adj`")
public class PreAdjFissProcCode {

  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Column(name = "`procCode`", length = 10, nullable = false)
  private String procCode;

  @Column(name = "`procFlag`", length = 4)
  private String procFlag;

  @Column(name = "`procDate`")
  private LocalDate procDate;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  /* PK class for the PreAdjFissProcCodes table */
  public static class PK implements Serializable {

    private String dcn;
    private short priority;
  }
}
