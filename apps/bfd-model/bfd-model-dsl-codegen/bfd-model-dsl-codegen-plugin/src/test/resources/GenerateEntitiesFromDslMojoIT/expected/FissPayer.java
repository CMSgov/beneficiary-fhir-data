package gov.cms.test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/**
 * This is a comment
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(
    onlyExplicitlyIncluded = true
)
@FieldNameConstants
@Table(
    name = "`FissPayers`",
    schema = "`pre_adj`"
)
@IdClass(FissPayer.PK.class)
public class FissPayer {
  @Id
  @EqualsAndHashCode.Include
  @ManyToOne
  @JoinColumn(
      name = "`dcn`",
      foreignKey = @ForeignKey(name = "fiss_payer_dcn_to_fiss_claims")
  )
  private FissClaim parentClaim;

  @Id
  @Column(
      name = "`priority`",
      nullable = false
  )
  @EqualsAndHashCode.Include
  private short priority;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "`payerType`",
      nullable = true,
      length = 20
  )
  private PayerType payerType;

  @Column(
      name = "`payersId`",
      nullable = true,
      length = 1
  )
  private String payersId;

  @Column(
      name = "`estAmtDue`",
      nullable = true,
      columnDefinition = "decimal(11,2)",
      precision = 11,
      scale = 2
  )
  private BigDecimal estAmtDue;

  /**
   * BeneZ only
   */
  @Column(
      name = "`beneRel`",
      nullable = true,
      length = 2
  )
  private String beneRel;

  /**
   * Insured only
   */
  @Column(
      name = "`insuredName`",
      nullable = true,
      length = 25
  )
  private String insuredName;

  @Column(
      name = "`insuredSex`",
      nullable = true,
      length = 1
  )
  private String insuredSex;

  @Column(
      name = "`insuredRelX12`",
      nullable = true,
      length = 2
  )
  private String insuredRelX12;

  @Column(
      name = "`insuredDob`",
      nullable = true
  )
  private LocalDate insuredDob;

  @Column(
      name = "`insuredDobText`",
      nullable = true,
      length = 9
  )
  private String insuredDobText;

  @Column(
      name = "`lastUpdated`",
      nullable = true
  )
  private Instant lastUpdated;

  public FissClaim getParentClaim() {
    return parentClaim;
  }

  public void setParentClaim(FissClaim parentClaim) {
    this.parentClaim = parentClaim;
  }

  public short getPriority() {
    return priority;
  }

  public void setPriority(short priority) {
    this.priority = priority;
  }

  public PayerType getPayerType() {
    return payerType;
  }

  public void setPayerType(PayerType payerType) {
    this.payerType = payerType;
  }

  public String getPayersId() {
    return payersId;
  }

  public void setPayersId(String payersId) {
    this.payersId = payersId;
  }

  public BigDecimal getEstAmtDue() {
    return estAmtDue;
  }

  public void setEstAmtDue(BigDecimal estAmtDue) {
    this.estAmtDue = estAmtDue;
  }

  public String getBeneRel() {
    return beneRel;
  }

  public void setBeneRel(String beneRel) {
    this.beneRel = beneRel;
  }

  public String getInsuredName() {
    return insuredName;
  }

  public void setInsuredName(String insuredName) {
    this.insuredName = insuredName;
  }

  public String getInsuredSex() {
    return insuredSex;
  }

  public void setInsuredSex(String insuredSex) {
    this.insuredSex = insuredSex;
  }

  public String getInsuredRelX12() {
    return insuredRelX12;
  }

  public void setInsuredRelX12(String insuredRelX12) {
    this.insuredRelX12 = insuredRelX12;
  }

  public LocalDate getInsuredDob() {
    return insuredDob;
  }

  public void setInsuredDob(LocalDate insuredDob) {
    this.insuredDob = insuredDob;
  }

  public String getInsuredDobText() {
    return insuredDobText;
  }

  public void setInsuredDobText(String insuredDobText) {
    this.insuredDobText = insuredDobText;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public enum PayerType {
    BeneZ,

    Insured
  }

  /**
   * PK class for the FissPayers table
   */
  @Getter
  @EqualsAndHashCode
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private static final long serialVersionUID = 1;

    private String parentClaim;

    private short priority;
  }
}
