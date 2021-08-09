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

/** JPA class for the PreAdjFissClaims table */
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

  // null if other servTypeCd variations are set
  @Column(name = "`servTypeCd`", length = 1)
  private String servTypeCd;

  // null if other servTypeCd variations are set
  @Column(name = "`servTypeCdForClinics`", length = 1)
  private String servTypeCdForClinics;

  // null if other servTypeCd variations are set
  @Column(name = "`servTypeCdForSpecialFacilities`", length = 1)
  private String servTypeCdForSpecialFacilities;

  @Column(name = "`freqCd`", length = 1)
  private String freqCd;

  @Column(name = "`billTypCd`", length = 3)
  private String billTypCd;

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
}
