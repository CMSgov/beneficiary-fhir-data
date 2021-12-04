package gov.cms.bfd.model.rda;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@Table(name = "`McsClaims`", schema = "`pre_adj`")
public class PreAdjMcsClaim {
  @Id
  @EqualsAndHashCode.Include
  @Column(name = "`idrClmHdIcn`", nullable = false, length = 15)
  private String idrClmHdIcn;

  @Column(name = "`sequenceNumber`", nullable = false)
  private Long sequenceNumber;

  @Column(name = "`idrContrId`", nullable = false, length = 5)
  private String idrContrId;

  @Column(name = "`idrHic`", length = 12)
  private String idrHic;

  @Column(name = "`idrClaimType`", nullable = false, length = 1)
  private String idrClaimType;

  @Column(name = "`idrDtlCnt`")
  private Integer idrDtlCnt;

  @Column(name = "`idrBeneLast_1_6`", length = 6)
  private String idrBeneLast_1_6;

  @Column(name = "`idrBeneFirstInit`", length = 1)
  private String idrBeneFirstInit;

  @Column(name = "`idrBeneMidInit`", length = 1)
  private String idrBeneMidInit;

  @Column(name = "`idrBeneSex`", length = 1)
  private String idrBeneSex;

  @Column(name = "`idrStatusCode`", length = 1)
  private String idrStatusCode;

  @Column(name = "`idrStatusDate`")
  private LocalDate idrStatusDate;

  @Column(name = "`idrBillProvNpi`", length = 10)
  private String idrBillProvNpi;

  @Column(name = "`idrBillProvNum`", length = 10)
  private String idrBillProvNum;

  @Column(name = "`idrBillProvEin`", length = 10)
  private String idrBillProvEin;

  @Column(name = "`idrBillProvType`", length = 2)
  private String idrBillProvType;

  @Column(name = "`idrBillProvSpec`", length = 2)
  private String idrBillProvSpec;

  @Column(name = "`idrBillProvGroupInd`", length = 1)
  private String idrBillProvGroupInd;

  @Column(name = "`idrBillProvPriceSpec`", length = 2)
  private String idrBillProvPriceSpec;

  @Column(name = "`idrBillProvCounty`", length = 2)
  private String idrBillProvCounty;

  @Column(name = "`idrBillProvLoc`", length = 2)
  private String idrBillProvLoc;

  @Column(name = "`idrTotAllowed`", columnDefinition = "decimal(7,2)")
  private BigDecimal idrTotAllowed;

  @Column(name = "`idrCoinsurance`", columnDefinition = "decimal(7,2)")
  private BigDecimal idrCoinsurance;

  @Column(name = "`idrDeductible`", columnDefinition = "decimal(7,2)")
  private BigDecimal idrDeductible;

  @Column(name = "`idrBillProvStatusCd`", length = 1)
  private String idrBillProvStatusCd;

  @Column(name = "`idrTotBilledAmt`", columnDefinition = "decimal(7,2)")
  private BigDecimal idrTotBilledAmt;

  @Column(name = "`idrClaimReceiptDate`")
  private LocalDate idrClaimReceiptDate;

  @Column(name = "`idrClaimMbi`", length = 13)
  private String idrClaimMbi;

  @Column(name = "`idrClaimMbiHash`", length = 64)
  private String idrClaimMbiHash;

  @Column(name = "`idrHdrFromDateOfSvc`")
  private LocalDate idrHdrFromDateOfSvc;

  @Column(name = "`idrHdrToDateOfSvc`")
  private LocalDate idrHdrToDateOfSvc;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  @Column(name = "`apiSource`", length = 24)
  private String apiSource;

  @Column(name = "`idrAssignment`", length = 1)
  private String idrAssignment;

  @Column(name = "`idrClmLevelInd`", length = 1)
  private String idrClmLevelInd;

  @Column(name = "`idrHdrAudit`")
  private Integer idrHdrAudit;

  @Column(name = "`idrHdrAuditInd`", length = 1)
  private String idrHdrAuditInd;

  @Column(name = "`idrUSplitReason`", length = 1)
  private String idrUSplitReason;

  @Column(name = "`idrJReferringProvNpi`", length = 10)
  private String idrJReferringProvNpi;

  @Column(name = "`idrJFacProvNpi`", length = 10)
  private String idrJFacProvNpi;

  @Column(name = "`idrUDemoProvNpi`", length = 10)
  private String idrUDemoProvNpi;

  @Column(name = "`idrUSuperNpi`", length = 10)
  private String idrUSuperNpi;

  @Column(name = "`idrUFcadjBilNpi`", length = 10)
  private String idrUFcadjBilNpi;

  @Column(name = "`idrAmbPickupAddresLine1`", length = 25)
  private String idrAmbPickupAddresLine1;

  @Column(name = "`idrAmbPickupAddresLine2`", length = 20)
  private String idrAmbPickupAddresLine2;

  @Column(name = "`idrAmbPickupCity`", length = 20)
  private String idrAmbPickupCity;

  @Column(name = "`idrAmbPickupState`", length = 2)
  private String idrAmbPickupState;

  @Column(name = "`idrAmbPickupZipcode`", length = 9)
  private String idrAmbPickupZipcode;

  @Column(name = "`idrAmbDropoffName`", length = 24)
  private String idrAmbDropoffName;

  @Column(name = "`idrAmbDropoffAddrLine1`", length = 25)
  private String idrAmbDropoffAddrLine1;

  @Column(name = "`idrAmbDropoffAddrLine2`", length = 20)
  private String idrAmbDropoffAddrLine2;

  @Column(name = "`idrAmbDropoffCity`", length = 20)
  private String idrAmbDropoffCity;

  @Column(name = "`idrAmbDropoffState`", length = 2)
  private String idrAmbDropoffState;

  @Column(name = "`idrAmbDropoffZipcode`", length = 9)
  private String idrAmbDropoffZipcode;

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjMcsDetail> details = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjMcsDiagnosisCode> diagCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjMcsAdjustment> adjustments = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjMcsAudit> audits = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjMcsLocation> locations = new HashSet<>();
}
