package gov.cms.bfd.model.rda;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the FissClaims table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`FissClaims`", schema = "`pre_adj`")
public class PreAdjFissClaim {
  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Column(name = "`sequenceNumber`", nullable = false)
  private Long sequenceNumber;

  @Column(name = "`hicNo`", length = 12, nullable = false)
  private String hicNo;

  @Column(name = "`currStatus`", nullable = false)
  private char currStatus;

  @Column(name = "`currLoc1`", nullable = false)
  private char currLoc1;

  @Column(name = "`currLoc2`", length = 5, nullable = false)
  private String currLoc2;

  @Column(name = "`medaProvId`", length = 13)
  private String medaProvId;

  @Column(name = "`medaProv_6`", length = 6)
  private String medaProv_6;

  @Column(name = "`totalChargeAmount`", columnDefinition = "decimal(11,2)")
  private BigDecimal totalChargeAmount;

  @Column(name = "`receivedDate`")
  private LocalDate receivedDate;

  @Column(name = "`currTranDate`")
  private LocalDate currTranDate;

  @Column(name = "`admitDiagCode`", length = 7)
  private String admitDiagCode;

  @Column(name = "`principleDiag`", length = 7)
  private String principleDiag;

  @Column(name = "`npiNumber`", length = 10)
  private String npiNumber;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "`mbiId`")
  private Mbi mbiRecord;

  @Column(name = "`fedTaxNumber`", length = 10)
  private String fedTaxNumber;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Column(name = "`pracLocAddr1`")
  private String pracLocAddr1;

  @Column(name = "`pracLocAddr2`")
  private String pracLocAddr2;

  @Column(name = "`pracLocCity`")
  private String pracLocCity;

  @Column(name = "`pracLocState`", length = 2)
  private String pracLocState;

  @Column(name = "`pracLocZip`", length = 15)
  private String pracLocZip;

  @Column(name = "`stmtCovFromDate`")
  private LocalDate stmtCovFromDate;

  @Column(name = "`stmtCovToDate`")
  private LocalDate stmtCovToDate;

  @Column(name = "`lobCd`", length = 1)
  private String lobCd;

  @Enumerated(EnumType.STRING)
  @Column(name = "`servTypeCdMapping`", length = 20)
  private ServTypeCdMapping servTypeCdMapping;

  @Column(name = "`servTypeCd`", length = 1)
  private String servTypeCd;

  @Column(name = "`freqCd`", length = 1)
  private String freqCd;

  @Column(name = "`billTypCd`", length = 3)
  private String billTypCd;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  @Column(name = "`apiSource`", length = 24)
  private String apiSource;

  /** Reject Code */
  @Column(name = "`rejectCd`", length = 5)
  private String rejectCd;

  /** Fully or Partially Denied Indicator */
  @Column(name = "`fullPartDenInd`", length = 1)
  private String fullPartDenInd;

  /** Non-Pay Code Indicator */
  @Column(name = "`nonPayInd`", length = 2)
  private String nonPayInd;

  /** Cross-reference Document Control Number */
  @Column(name = "`xrefDcnNbr`", length = 23)
  private String xrefDcnNbr;

  /** Adjustment Requestor Identification */
  @Column(name = "`adjReqCd`", length = 1)
  private String adjReqCd;

  /** Adjustment Reason Code */
  @Column(name = "`adjReasCd`", length = 2)
  private String adjReasCd;

  /** Cancel Cross-reference Document Control Number */
  @Column(name = "`cancelXrefDcn`", length = 23)
  private String cancelXrefDcn;

  /** Cancel Date */
  @Column(name = "`cancelDate`")
  private LocalDate cancelDate;

  /** Cancel Adjustment Code */
  @Column(name = "`cancAdjCd`", length = 1)
  private String cancAdjCd;

  /** Original Cross-Reference Document Control Number */
  @Column(name = "`originalXrefDcn`", length = 23)
  private String originalXrefDcn;

  /** Paid Date */
  @Column(name = "`paidDt`")
  private LocalDate paidDt;

  /** Admission Date */
  @Column(name = "`admDate`")
  private LocalDate admDate;

  /** Source of Admission */
  @Column(name = "`admSource`", length = 1)
  private String admSource;

  /** Primary Payer Code */
  @Column(name = "`primaryPayerCode`", length = 1)
  private String primaryPayerCode;

  /** Attending Physician NPI */
  @Column(name = "`attendPhysId`", length = 16)
  private String attendPhysId;

  /** Attending Physician Last Name */
  @Column(name = "`attendPhysLname`", length = 17)
  private String attendPhysLname;

  /** Attending Physician First Name */
  @Column(name = "`attendPhysFname`", length = 18)
  private String attendPhysFname;

  /** Attending Physician Middle Initial */
  @Column(name = "`attendPhysMint`", length = 1)
  private String attendPhysMint;

  /** Attending Physician Flag */
  @Column(name = "`attendPhysFlag`", length = 1)
  private String attendPhysFlag;

  /** Operating Physician NPI */
  @Column(name = "`operatingPhysId`", length = 16)
  private String operatingPhysId;

  /** Operating Physician Last Name */
  @Column(name = "`operPhysLname`", length = 17)
  private String operPhysLname;

  /** Operating Physician First Name */
  @Column(name = "`operPhysFname`", length = 18)
  private String operPhysFname;

  /** Operating Physician Middle Initial */
  @Column(name = "`operPhysMint`", length = 1)
  private String operPhysMint;

  /** Operating Physician Flag */
  @Column(name = "`operPhysFlag`", length = 1)
  private String operPhysFlag;

  /** Other Physician NPI */
  @Column(name = "`othPhysId`", length = 16)
  private String othPhysId;

  /** Other Physician Last Name */
  @Column(name = "`othPhysLname`", length = 17)
  private String othPhysLname;

  /** Other Physician First Name */
  @Column(name = "`othPhysFname`", length = 18)
  private String othPhysFname;

  /** Other Physician Middle Initial */
  @Column(name = "`othPhysMint`", length = 1)
  private String othPhysMint;

  /** Other Physician Flag */
  @Column(name = "`othPhysFlag`", length = 1)
  private String othPhysFlag;

  /** Cross-Reference Health Insurance Claim Number */
  @Column(name = "`xrefHicNbr`", length = 12)
  private String xrefHicNbr;

  /** Process new Health Insurance Claim Number */
  @Column(name = "`procNewHicInd`", length = 1)
  private String procNewHicInd;

  /** New Health Insurance Claim Number */
  @Column(name = "`newHic`", length = 12)
  private String newHic;

  /** Repository Indicator */
  @Column(name = "`reposInd`", length = 1)
  private String reposInd;

  /** Repository HIC */
  @Column(name = "`reposHic`", length = 12)
  private String reposHic;

  /** Health Insurance Claim (HIC) Number or Medicare Beneficiary Identify (MBI) */
  @Column(name = "`mbiSubmBeneInd`", length = 1)
  private String mbiSubmBeneInd;

  /** Adjustment Medicare Beneficiary Identifier (MBI) Indicator */
  @Column(name = "`adjMbiInd`", length = 1)
  private String adjMbiInd;

  /** Adjustment Medicare Beneficiary Identifier */
  @Column(name = "`adjMbi`", length = 11)
  private String adjMbi;

  /** Medical Record Number */
  @Column(name = "`medicalRecordNo`", length = 17)
  private String medicalRecordNo;

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjFissProcCode> procCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjFissDiagnosisCode> diagCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjFissPayer> payers = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @Builder.Default
  private Set<PreAdjFissAuditTrail> auditTrail = new HashSet<>();

  public String getMbi() {
    return mbiRecord != null ? mbiRecord.getMbi() : null;
  }

  public String getMbiHash() {
    return mbiRecord != null ? mbiRecord.getHash() : null;
  }

  public enum ServTypeCdMapping {
    Normal,
    Clinic,
    SpecialFacility,
    Unrecognized
  }

  /**
   * Defines extra field names. Lombok will append all of the other fields to this class
   * automatically.
   */
  public static class Fields {
    public static final String mbi = "mbi";
  }
}
