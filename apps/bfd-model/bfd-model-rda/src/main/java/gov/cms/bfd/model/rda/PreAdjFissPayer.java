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
@Table(name = "fiss_payers", schema = "rda")
public class PreAdjFissPayer {
  @Id
  @Column(name = "dcn", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Id
  @Column(name = "priority", nullable = false)
  @EqualsAndHashCode.Include
  private short priority;

  @Enumerated(EnumType.STRING)
  @Column(name = "payer_type", length = 20)
  private PayerType payerType;

  @Column(name = "payers_id", length = 1)
  private String payersId;

  @Column(name = "payers_name", length = 32)
  private String payersName;

  @Column(name = "rel_ind", length = 1)
  private String relInd;

  @Column(name = "assign_ind", length = 1)
  private String assignInd;

  @Column(name = "provider_number", length = 13)
  private String providerNumber;

  @Column(name = "adj_dcn_icn", length = 23)
  private String adjDcnIcn;

  @Column(name = "prior_pmt", columnDefinition = "decimal(11,2)")
  private BigDecimal priorPmt;

  @Column(name = "est_amt_due", columnDefinition = "decimal(11,2)")
  private BigDecimal estAmtDue;

  /** BeneZ only */
  @Column(name = "bene_rel", length = 2)
  private String beneRel;

  /** BeneZ only */
  @Column(name = "bene_last_name", length = 15)
  private String beneLastName;

  /** BeneZ only */
  @Column(name = "bene_first_name", length = 10)
  private String beneFirstName;

  /** BeneZ only */
  @Column(name = "bene_mid_init", length = 1)
  private String beneMidInit;

  /** BeneZ only */
  @Column(name = "bene_ssn_hic", length = 19)
  private String beneSsnHic;

  /** Insured only */
  @Column(name = "insured_rel", length = 2)
  private String insuredRel;

  /** Insured only */
  @Column(name = "insured_name", length = 25)
  private String insuredName;

  /** Insured only */
  @Column(name = "insured_ssn_hic", length = 19)
  private String insuredSsnHic;

  @Column(name = "insured_group_name", length = 17)
  private String insuredGroupName;

  /** Insured only */
  @Column(name = "insured_group_nbr", length = 20)
  private String insuredGroupNbr;

  /** BeneZ only */
  @Column(name = "bene_dob")
  private LocalDate beneDob;

  /** BeneZ only */
  @Column(name = "bene_sex", length = 1)
  private String beneSex;

  @Column(name = "treat_auth_cd", length = 18)
  private String treatAuthCd;

  @Column(name = "insured_sex", length = 1)
  private String insuredSex;

  @Column(name = "insured_rel_x12", length = 2)
  private String insuredRelX12;

  @Column(name = "insured_dob")
  private LocalDate insuredDob;

  @Column(name = "insured_dob_text", length = 9)
  private String insuredDobText;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /** PK class for the FissPayers table */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PK implements Serializable {
    private String dcn;

    private short priority;
  }

  public enum PayerType {
    BeneZ,

    Insured
  }
}
