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
@Table(name = "`McsDetails`", schema = "`pre_adj`")
@IdClass(PreAdjMcsDetail.PK.class)
public class PreAdjMcsDetail {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`idrClmHdIcn`", nullable = false, length = 15)
  private String idrClmHdIcn;

  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`priority`", nullable = false)
  private short priority;

  @Column(name = "`idrDtlStatus`", length = 1)
  private String idrDtlStatus;

  @Column(name = "`idrDtlFromDate`")
  private LocalDate idrDtlFromDate;

  @Column(name = "`idrDtlToDate`")
  private LocalDate idrDtlToDate;

  @Column(name = "`idrProcCode`", length = 5)
  private String idrProcCode;

  @Column(name = "`idrModOne`", length = 2)
  private String idrModOne;

  @Column(name = "`idrModTwo`", length = 2)
  private String idrModTwo;

  @Column(name = "`idrModThree`", length = 2)
  private String idrModThree;

  @Column(name = "`idrModFour`", length = 2)
  private String idrModFour;

  @Column(name = "`idrDtlDiagIcdType`", length = 1)
  private String idrDtlDiagIcdType;

  @Column(name = "`idrDtlPrimaryDiagCode`", length = 7)
  private String idrDtlPrimaryDiagCode;

  @Column(name = "`idrKPosLnameOrg`", length = 60)
  private String idrKPosLnameOrg;

  @Column(name = "`idrKPosFname`", length = 35)
  private String idrKPosFname;

  @Column(name = "`idrKPosMname`", length = 25)
  private String idrKPosMname;

  @Column(name = "`idrKPosAddr1`", length = 55)
  private String idrKPosAddr1;

  @Column(name = "`idrKPosAddr2_1st`", length = 30)
  private String idrKPosAddr2_1st;

  @Column(name = "`idrKPosAddr2_2nd`", length = 25)
  private String idrKPosAddr2_2nd;

  @Column(name = "`idrKPosCity`", length = 30)
  private String idrKPosCity;

  @Column(name = "`idrKPosState`", length = 2)
  private String idrKPosState;

  @Column(name = "`idrKPosZip`", length = 15)
  private String idrKPosZip;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the McsDetails table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
