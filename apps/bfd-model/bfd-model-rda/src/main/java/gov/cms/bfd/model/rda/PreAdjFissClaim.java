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

  @Column(name = "`mbi`", length = 13)
  private String mbi;

  @Column(name = "`mbiHash`", length = 64)
  private String mbiHash;

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

  public enum ServTypeCdMapping {
    Normal,
    Clinic,
    SpecialFacility,
    Unrecognized
  }

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
  @Column(name = "`apiSource`", length = 64)
  private String apiSource;

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
}
