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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/** JPA class for the PreAdjFissClaims table */
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "`FissClaims`", schema = "pre_adj")
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

  @Column(name = "`totalChargeAmount`")
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

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  private Set<PreAdjFissProcCode> procCodes = new HashSet<>();
}
