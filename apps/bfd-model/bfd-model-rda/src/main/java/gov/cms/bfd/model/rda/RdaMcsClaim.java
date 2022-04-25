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

/** JPA class for the McsClaims table */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@FieldNameConstants
@Table(name = "mcs_claims", schema = "rda")
public class RdaMcsClaim {
  @Id
  @Column(name = "idr_clm_hd_icn", length = 15, nullable = false)
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Column(name = "sequence_number", nullable = false)
  private Long sequenceNumber;

  @Column(name = "idr_contr_id", length = 5, nullable = false)
  private String idrContrId;

  @Column(name = "idr_hic", length = 12)
  private String idrHic;

  @Column(name = "idr_claim_type", length = 1, nullable = false)
  private String idrClaimType;

  @Column(name = "idr_dtl_cnt")
  private Integer idrDtlCnt;

  @Column(name = "idr_bene_last_1_6", length = 6)
  private String idrBeneLast_1_6;

  @Column(name = "idr_bene_first_init", length = 1)
  private String idrBeneFirstInit;

  @Column(name = "idr_bene_mid_init", length = 1)
  private String idrBeneMidInit;

  @Column(name = "idr_bene_sex", length = 1)
  private String idrBeneSex;

  @Column(name = "idr_status_code", length = 1)
  private String idrStatusCode;

  @Column(name = "idr_status_date")
  private LocalDate idrStatusDate;

  @Column(name = "idr_bill_prov_npi", length = 10)
  private String idrBillProvNpi;

  @Column(name = "idr_bill_prov_num", length = 10)
  private String idrBillProvNum;

  @Column(name = "idr_bill_prov_ein", length = 10)
  private String idrBillProvEin;

  @Column(name = "idr_bill_prov_type", length = 2)
  private String idrBillProvType;

  @Column(name = "idr_bill_prov_spec", length = 2)
  private String idrBillProvSpec;

  @Column(name = "idr_bill_prov_group_ind", length = 1)
  private String idrBillProvGroupInd;

  @Column(name = "idr_bill_prov_price_spec", length = 2)
  private String idrBillProvPriceSpec;

  @Column(name = "idr_bill_prov_county", length = 2)
  private String idrBillProvCounty;

  @Column(name = "idr_bill_prov_loc", length = 2)
  private String idrBillProvLoc;

  @Column(name = "idr_tot_allowed", columnDefinition = "decimal(7,2)")
  private BigDecimal idrTotAllowed;

  @Column(name = "idr_coinsurance", columnDefinition = "decimal(7,2)")
  private BigDecimal idrCoinsurance;

  @Column(name = "idr_deductible", columnDefinition = "decimal(7,2)")
  private BigDecimal idrDeductible;

  @Column(name = "idr_bill_prov_status_cd", length = 1)
  private String idrBillProvStatusCd;

  @Column(name = "idr_tot_billed_amt", columnDefinition = "decimal(7,2)")
  private BigDecimal idrTotBilledAmt;

  @Column(name = "idr_claim_receipt_date")
  private LocalDate idrClaimReceiptDate;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "mbi_id")
  private Mbi mbiRecord;

  @Column(name = "idr_hdr_from_date_of_svc")
  private LocalDate idrHdrFromDateOfSvc;

  @Column(name = "idr_hdr_to_date_of_svc")
  private LocalDate idrHdrToDateOfSvc;

  @Column(name = "last_updated")
  private Instant lastUpdated;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  @Column(name = "api_source", length = 24)
  private String apiSource;

  @Column(name = "idr_assignment", length = 1)
  private String idrAssignment;

  @Column(name = "idr_clm_level_ind", length = 1)
  private String idrClmLevelInd;

  @Column(name = "idr_hdr_audit")
  private Integer idrHdrAudit;

  @Column(name = "idr_hdr_audit_ind", length = 1)
  private String idrHdrAuditInd;

  @Column(name = "idr_u_split_reason", length = 1)
  private String idrUSplitReason;

  @Column(name = "idr_j_referring_prov_npi", length = 10)
  private String idrJReferringProvNpi;

  @Column(name = "idr_j_fac_prov_npi", length = 10)
  private String idrJFacProvNpi;

  @Column(name = "idr_u_demo_prov_npi", length = 10)
  private String idrUDemoProvNpi;

  @Column(name = "idr_u_super_npi", length = 10)
  private String idrUSuperNpi;

  @Column(name = "idr_u_fcadj_bil_npi", length = 10)
  private String idrUFcadjBilNpi;

  @Column(name = "idr_amb_pickup_addres_line1", length = 25)
  private String idrAmbPickupAddresLine1;

  @Column(name = "idr_amb_pickup_addres_line2", length = 20)
  private String idrAmbPickupAddresLine2;

  @Column(name = "idr_amb_pickup_city", length = 20)
  private String idrAmbPickupCity;

  @Column(name = "idr_amb_pickup_state", length = 2)
  private String idrAmbPickupState;

  @Column(name = "idr_amb_pickup_zipcode", length = 9)
  private String idrAmbPickupZipcode;

  @Column(name = "idr_amb_dropoff_name", length = 24)
  private String idrAmbDropoffName;

  @Column(name = "idr_amb_dropoff_addr_line1", length = 25)
  private String idrAmbDropoffAddrLine1;

  @Column(name = "idr_amb_dropoff_addr_line2", length = 20)
  private String idrAmbDropoffAddrLine2;

  @Column(name = "idr_amb_dropoff_city", length = 20)
  private String idrAmbDropoffCity;

  @Column(name = "idr_amb_dropoff_state", length = 2)
  private String idrAmbDropoffState;

  @Column(name = "idr_amb_dropoff_zipcode", length = 9)
  private String idrAmbDropoffZipcode;

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<RdaMcsDetail> details = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<RdaMcsDiagnosisCode> diagCodes = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<RdaMcsAdjustment> adjustments = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<RdaMcsAudit> audits = new HashSet<>();

  @OneToMany(
      mappedBy = "idrClmHdIcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL)
  @BatchSize(size = 100)
  @Builder.Default
  private Set<RdaMcsLocation> locations = new HashSet<>();

  public String getIdrClaimMbi() {
    return mbiRecord != null ? mbiRecord.getMbi() : null;
  }

  public String getIdrClaimMbiHash() {
    return mbiRecord != null ? mbiRecord.getHash() : null;
  }

  /**
   * Defines extra field names. Lombok will append all of the other fields to this class
   * automatically.
   */
  public static class Fields {
    public static final String idrClaimMbi = "idrClaimMbi";
  }
}
