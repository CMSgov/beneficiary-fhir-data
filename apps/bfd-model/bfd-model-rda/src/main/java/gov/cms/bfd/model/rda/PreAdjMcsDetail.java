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

/** JPA class for the McsDetails table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjMcsDetail.PK.class)
@Table(name = "`McsDetails`", schema = "`pre_adj`")
public class PreAdjMcsDetail {
  @Id
  @Column(name = "`idrClmHdIcn`", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
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

  @Column(name = "`idrTos`", length = 1)
  private String idrTos;

  @Column(name = "`idrTwoDigitPos`", length = 2)
  private String idrTwoDigitPos;

  @Column(name = "`idrDtlRendType`", length = 2)
  private String idrDtlRendType;

  @Column(name = "`idrDtlRendSpec`", length = 2)
  private String idrDtlRendSpec;

  @Column(name = "`idrDtlRendNpi`", length = 10)
  private String idrDtlRendNpi;

  @Column(name = "`idrDtlRendProv`", length = 10)
  private String idrDtlRendProv;

  @Column(name = "`idrKDtlFacProvNpi`", length = 10)
  private String idrKDtlFacProvNpi;

  @Column(name = "`idrDtlAmbPickupAddres1`", length = 25)
  private String idrDtlAmbPickupAddres1;

  @Column(name = "`idrDtlAmbPickupAddres2`", length = 20)
  private String idrDtlAmbPickupAddres2;

  @Column(name = "`idrDtlAmbPickupCity`", length = 20)
  private String idrDtlAmbPickupCity;

  @Column(name = "`idrDtlAmbPickupState`", length = 2)
  private String idrDtlAmbPickupState;

  @Column(name = "`idrDtlAmbPickupZipcode`", length = 9)
  private String idrDtlAmbPickupZipcode;

  @Column(name = "`idrDtlAmbDropoffName`", length = 24)
  private String idrDtlAmbDropoffName;

  @Column(name = "`idrDtlAmbDropoffAddrL1`", length = 25)
  private String idrDtlAmbDropoffAddrL1;

  @Column(name = "`idrDtlAmbDropoffAddrL2`", length = 20)
  private String idrDtlAmbDropoffAddrL2;

  @Column(name = "`idrDtlAmbDropoffCity`", length = 20)
  private String idrDtlAmbDropoffCity;

  @Column(name = "`idrDtlAmbDropoffState`", length = 2)
  private String idrDtlAmbDropoffState;

  @Column(name = "`idrDtlAmbDropoffZipcode`", length = 9)
  private String idrDtlAmbDropoffZipcode;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /** PK class for the McsDetails table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
