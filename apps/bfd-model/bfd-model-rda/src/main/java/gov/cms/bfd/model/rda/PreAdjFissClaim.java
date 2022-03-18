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
import org.hibernate.annotations.BatchSize;

/** JPA class for the FissClaims table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "fiss_claims", schema = "part_adj")
public class PreAdjFissClaim {
  @Id
  @Column(name = "dcn", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Column(name = "sequence_number", nullable = false)
  private Long sequenceNumber;

  @Column(name = "hic_no", length = 12, nullable = false)
  private String hicNo;

  @Column(name = "curr_status", nullable = false)
  private char currStatus;

  @Column(name = "curr_loc1", nullable = false)
  private char currLoc1;

  @Column(name = "curr_loc2", length = 5, nullable = false)
  private String currLoc2;

  @Column(name = "meda_prov_id", length = 13)
  private String medaProvId;

  @Column(name = "meda_prov_6", length = 6)
  private String medaProv_6;

  @Column(name = "total_charge_amount", columnDefinition = "decimal(11,2)")
  private BigDecimal totalChargeAmount;

  @Column(name = "received_date")
  private LocalDate receivedDate;

  @Column(name = "curr_tran_date")
  private LocalDate currTranDate;

  @Column(name = "admit_diag_code", length = 7)
  private String admitDiagCode;

  @Column(name = "principle_diag", length = 7)
  private String principleDiag;

  @Column(name = "npi_number", length = 10)
  private String npiNumber;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "mbi_id")
  private Mbi mbiRecord;

  @Column(name = "fed_tax_number", length = 10)
  private String fedTaxNumber;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  @Column(name = "prac_loc_addr1")
  private String pracLocAddr1;

  @Column(name = "prac_loc_addr2")
  private String pracLocAddr2;

  @Column(name = "prac_loc_city")
  private String pracLocCity;

  @Column(name = "prac_loc_state", length = 2)
  private String pracLocState;

  @Column(name = "prac_loc_zip", length = 15)
  private String pracLocZip;

  @Column(name = "stmt_cov_from_date")
  private LocalDate stmtCovFromDate;

  @Column(name = "stmt_cov_to_date")
  private LocalDate stmtCovToDate;

  @Column(name = "lob_cd", length = 1)
  private String lobCd;

  @Enumerated(EnumType.STRING)
  @Column(name = "serv_type_cd_mapping", length = 20)
  private ServTypeCdMapping servTypeCdMapping;

  @Column(name = "serv_type_cd", length = 1)
  private String servTypeCd;

  @Column(name = "freq_cd", length = 1)
  private String freqCd;

  @Column(name = "bill_typ_cd", length = 3)
  private String billTypCd;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  @Column(name = "api_source", length = 24)
  private String apiSource;

  /** Reject Code */
  @Column(name = "reject_cd", length = 5)
  private String rejectCd;

  /** Fully or Partially Denied Indicator */
  @Column(name = "full_part_den_ind", length = 1)
  private String fullPartDenInd;

  /** Non-Pay Code Indicator */
  @Column(name = "non_pay_ind", length = 2)
  private String nonPayInd;

  /** Cross-reference Document Control Number */
  @Column(name = "xref_dcn_nbr", length = 23)
  private String xrefDcnNbr;

  /** Adjustment Requestor Identification */
  @Column(name = "adj_req_cd", length = 1)
  private String adjReqCd;

  /** Adjustment Reason Code */
  @Column(name = "adj_reas_cd", length = 2)
  private String adjReasCd;

  /** Cancel Cross-reference Document Control Number */
  @Column(name = "cancel_xref_dcn", length = 23)
  private String cancelXrefDcn;

  /** Cancel Date */
  @Column(name = "cancel_date")
  private LocalDate cancelDate;

  /** Cancel Adjustment Code */
  @Column(name = "canc_adj_cd", length = 1)
  private String cancAdjCd;

  /** Original Cross-Reference Document Control Number */
  @Column(name = "original_xref_dcn", length = 23)
  private String originalXrefDcn;

  /** Paid Date */
  @Column(name = "paid_dt")
  private LocalDate paidDt;

  /** Admission Date */
  @Column(name = "adm_date")
  private LocalDate admDate;

  /** Source of Admission */
  @Column(name = "adm_source", length = 1)
  private String admSource;

  /** Primary Payer Code */
  @Column(name = "primary_payer_code", length = 1)
  private String primaryPayerCode;

  /** Attending Physician NPI */
  @Column(name = "attend_phys_id", length = 16)
  private String attendPhysId;

  /** Attending Physician Last Name */
  @Column(name = "attend_phys_lname", length = 17)
  private String attendPhysLname;

  /** Attending Physician First Name */
  @Column(name = "attend_phys_fname", length = 18)
  private String attendPhysFname;

  /** Attending Physician Middle Initial */
  @Column(name = "attend_phys_mint", length = 1)
  private String attendPhysMint;

  /** Attending Physician Flag */
  @Column(name = "attend_phys_flag", length = 1)
  private String attendPhysFlag;

  /** Operating Physician NPI */
  @Column(name = "operating_phys_id", length = 16)
  private String operatingPhysId;

  /** Operating Physician Last Name */
  @Column(name = "oper_phys_lname", length = 17)
  private String operPhysLname;

  /** Operating Physician First Name */
  @Column(name = "oper_phys_fname", length = 18)
  private String operPhysFname;

  /** Operating Physician Middle Initial */
  @Column(name = "oper_phys_mint", length = 1)
  private String operPhysMint;

  /** Operating Physician Flag */
  @Column(name = "oper_phys_flag", length = 1)
  private String operPhysFlag;

  /** Other Physician NPI */
  @Column(name = "oth_phys_id", length = 16)
  private String othPhysId;

  /** Other Physician Last Name */
  @Column(name = "oth_phys_lname", length = 17)
  private String othPhysLname;

  /** Other Physician First Name */
  @Column(name = "oth_phys_fname", length = 18)
  private String othPhysFname;

  /** Other Physician Middle Initial */
  @Column(name = "oth_phys_mint", length = 1)
  private String othPhysMint;

  /** Other Physician Flag */
  @Column(name = "oth_phys_flag", length = 1)
  private String othPhysFlag;

  /** Cross-Reference Health Insurance Claim Number */
  @Column(name = "xref_hic_nbr", length = 12)
  private String xrefHicNbr;

  /** Process new Health Insurance Claim Number */
  @Column(name = "proc_new_hic_ind", length = 1)
  private String procNewHicInd;

  /** New Health Insurance Claim Number */
  @Column(name = "new_hic", length = 12)
  private String newHic;

  /** Repository Indicator */
  @Column(name = "repos_ind", length = 1)
  private String reposInd;

  /** Repository HIC */
  @Column(name = "repos_hic", length = 12)
  private String reposHic;

  /** Health Insurance Claim (HIC) Number or Medicare Beneficiary Identify (MBI) */
  @Column(name = "mbi_subm_bene_ind", length = 1)
  private String mbiSubmBeneInd;

  /** Adjustment Medicare Beneficiary Identifier (MBI) Indicator */
  @Column(name = "adj_mbi_ind", length = 1)
  private String adjMbiInd;

  /** Adjustment Medicare Beneficiary Identifier */
  @Column(name = "adj_mbi", length = 11)
  private String adjMbi;

  /** Medical Record Number */
  @Column(name = "medical_record_no", length = 17)
  private String medicalRecordNo;

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<PreAdjFissProcCode> procCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<PreAdjFissDiagnosisCode> diagCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<PreAdjFissPayer> payers = new HashSet<>();

  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
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
