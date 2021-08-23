package gov.cms.bfd.model.rda;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

/** JPA class for the FissPayers table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@IdClass(PreAdjFissPayer.PK.class)
@Table(name = "`FissPayers`", schema = "`pre_adj`")
public class PreAdjFissPayer {
  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "`priority`", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  public enum PayerType {
    BeneZ,
    Insured
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "`payerType`", length = 20)
  private PayerType payerType;

  @Column(name = "`payersId`", length = 1)
  private String payersId;

  @Column(name = "`payersName`", length = 32)
  private String payersName;

  @Column(name = "`relInd`", length = 1)
  private String relInd;

  @Column(name = "`assignInd`", length = 1)
  private String assignInd;

  @Column(name = "`providerNumber`", length = 13)
  private String providerNumber;

  @Column(name = "`adjDcnIcn`", length = 23)
  private String adjDcnIcn;

  @Column(name = "`priorPmt`", columnDefinition = "decimal(11,2)")
  private BigDecimal priorPmt;

  @Column(name = "`estAmtDue`", columnDefinition = "decimal(11,2)")
  private BigDecimal estAmtDue;

  // BeneZ only
  @Column(name = "`beneRel`", length = 2)
  private String beneRel;

  // BeneZ only
  @Column(name = "`beneLastName`", length = 15)
  private String beneLastName;

  // BeneZ only
  @Column(name = "`beneFirstName`", length = 10)
  private String beneFirstName;

  // BeneZ only
  @Column(name = "`beneMidInit`", length = 1)
  private String beneMidInit;

  // BeneZ only
  @Column(name = "`beneSsnHic`", length = 19)
  private String beneSsnHic;

  // Insured only
  @Column(name = "`insuredRel`", length = 2)
  private String insuredRel;

  // Insured only
  @Column(name = "`insuredName`", length = 25)
  private String insuredName;

  // Insured only
  @Column(name = "`insuredSsnHic`", length = 19)
  private String insuredSsnHic;

  @Column(name = "`insuredGroupName`", length = 17)
  private String insuredGroupName;

  // Insured only
  @Column(name = "`insuredGroupNbr`", length = 20)
  private String insuredGroupNbr;

  // BeneZ only
  @Column(name = "`beneDob`")
  private LocalDate beneDob;

  // BeneZ only
  @Column(name = "`beneSex`", length = 1)
  private String beneSex;

  @Column(name = "`treatAuthCd`", length = 18)
  private String treatAuthCd;

  @Column(name = "`insuredSex`", length = 1)
  private String insuredSex;

  @Column(name = "`insuredRelX12`", length = 2)
  private String insuredRelX12;

  // Insured only
  @Column(name = "`insuredDob`")
  private LocalDate insuredDob;

  // Insured only
  @Column(name = "`insuredDobText`", length = 9)
  private String insuredDobText;

  @Column(name = "`lastUpdated`")
  private Instant lastUpdated;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  /* PK class for the FissDiagnosisCodes table */
  public static class PK implements Serializable {
    private String dcn;
    private short priority;
  }
}
