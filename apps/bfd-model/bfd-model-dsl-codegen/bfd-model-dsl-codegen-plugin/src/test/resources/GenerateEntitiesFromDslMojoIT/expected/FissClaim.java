package gov.cms.test;

import gov.cms.bfd.model.rda.Mbi;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.lang.Long;
import java.lang.String;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

/**
 * JPA class for the {@code FissClaims} table.
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
    name = "`FissClaims`",
    schema = "`pre_adj`"
)
public class FissClaim {
  @Id
  @Column(
      name = "`dcn`",
      nullable = false,
      length = 23
  )
  @EqualsAndHashCode.Include
  private String dcn;

  @Column(
      name = "`sequenceNumber`",
      nullable = false
  )
  private Long sequenceNumber;

  @Column(
      name = "`currStatus`",
      nullable = false,
      length = 1
  )
  private char currStatus;

  @Column(
      name = "`provStateCd`",
      nullable = true,
      length = 2
  )
  private String provStateCd;

  /**
   * Medicare Provider ID:
   *
   * <p>The Medicare Provider ID consists of the following:
   *
   * <ul>
   *   <li>Provider State Code
   *   <li>Provider Type Facility Code
   *   <li>Provider Emergency Indicator
   *   <li>Provider Department Identification
   * </ul>
   */
  @Column(
      name = "`medaProvId`",
      nullable = true,
      length = 13
  )
  private String medaProvId;

  @Column(
      name = "`medaProv_6`",
      nullable = true,
      length = 6
  )
  private String medaProv_6;

  @Column(
      name = "`totalChargeAmount`",
      nullable = true,
      columnDefinition = "decimal(11,2)",
      precision = 11,
      scale = 2
  )
  private BigDecimal totalChargeAmount;

  @Column(
      name = "`receivedDate`",
      nullable = true
  )
  private LocalDate receivedDate;

  @Column(
      name = "`lastUpdated`",
      nullable = true
  )
  private Instant lastUpdated;

  @Column(
      name = "`pracLocAddr1`",
      nullable = true
  )
  private String pracLocAddr1;

  @Column(
      name = "`lobCd`",
      nullable = true,
      length = 1
  )
  private String lobCd;

  @Enumerated(EnumType.STRING)
  @Column(
      name = "`servTypeCdMapping`",
      nullable = true,
      length = 20
  )
  private ServTypeCdMapping servTypeCdMapping;

  @Column(
      name = "`servTypeCd`",
      nullable = true,
      length = 1
  )
  private String servTypeCd;

  @Column(
      name = "`freqCd`",
      nullable = true,
      length = 1
  )
  private String freqCd;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the version string returned by the RDA API server but when populating data from mock server it will also include information about the mode the server was running in.
   */
  @Column(
      name = "`apiSource`",
      nullable = true,
      length = 24
  )
  private String apiSource;

  /**
   * Paid Date
   */
  @Column(
      name = "`paidDt`",
      nullable = true
  )
  private LocalDate paidDt;

  /**
   * Source of Admission
   */
  @Column(
      name = "`admSource`",
      nullable = true,
      length = 1
  )
  private String admSource;

  @Column(
      name = "`bene_history_id`",
      nullable = false,
      updatable = false
  )
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "beneficiaryhistory_beneficiaryhistoryid_seq"
  )
  @SequenceGenerator(
      name = "beneficiaryhistory_beneficiaryhistoryid_seq",
      sequenceName = "beneficiaryhistory_beneficiaryhistoryid_seq",
      allocationSize = 50,
      schema = "`pre_adj`"
  )
  private long beneHistoryId;

  @OneToMany(
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL
  )
  @BatchSize(
      size = 100
  )
  @Builder.Default
  private Set<FissProcCode> procCodes = new HashSet<>();

  /**
   * List of FissPayers for this claim.
   */
  @OneToMany(
      mappedBy = "parentClaim",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL
  )
  @OrderBy("PRIORITY ASC")
  @BatchSize(
      size = 100
  )
  @Builder.Default
  private Set<FissPayer> payers = new HashSet<>();

  @ManyToOne(
      fetch = FetchType.EAGER
  )
  @JoinColumn(
      name = "`mbi_id`"
  )
  private Mbi mbiRecord;

  public String getDcn() {
    return dcn;
  }

  public void setDcn(String dcn) {
    this.dcn = dcn;
  }

  public Long getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(Long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public char getCurrStatus() {
    return currStatus;
  }

  public void setCurrStatus(char currStatus) {
    this.currStatus = currStatus;
  }

  public String getProvStateCd() {
    return provStateCd;
  }

  public void setProvStateCd(String provStateCd) {
    this.provStateCd = provStateCd;
  }

  public String getMedaProvId() {
    return medaProvId;
  }

  public void setMedaProvId(String medaProvId) {
    this.medaProvId = medaProvId;
  }

  public String getMedaProv_6() {
    return medaProv_6;
  }

  public void setMedaProv_6(String medaProv_6) {
    this.medaProv_6 = medaProv_6;
  }

  public BigDecimal getTotalChargeAmount() {
    return totalChargeAmount;
  }

  public void setTotalChargeAmount(BigDecimal totalChargeAmount) {
    this.totalChargeAmount = totalChargeAmount;
  }

  public LocalDate getReceivedDate() {
    return receivedDate;
  }

  public void setReceivedDate(LocalDate receivedDate) {
    this.receivedDate = receivedDate;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public String getPracLocAddr1() {
    return pracLocAddr1;
  }

  public void setPracLocAddr1(String pracLocAddr1) {
    this.pracLocAddr1 = pracLocAddr1;
  }

  public String getLobCd() {
    return lobCd;
  }

  public void setLobCd(String lobCd) {
    this.lobCd = lobCd;
  }

  public ServTypeCdMapping getServTypeCdMapping() {
    return servTypeCdMapping;
  }

  public void setServTypeCdMapping(ServTypeCdMapping servTypeCdMapping) {
    this.servTypeCdMapping = servTypeCdMapping;
  }

  public String getServTypeCd() {
    return servTypeCd;
  }

  public void setServTypeCd(String servTypeCd) {
    this.servTypeCd = servTypeCd;
  }

  public String getFreqCd() {
    return freqCd;
  }

  public void setFreqCd(String freqCd) {
    this.freqCd = freqCd;
  }

  public String getApiSource() {
    return apiSource;
  }

  public void setApiSource(String apiSource) {
    this.apiSource = apiSource;
  }

  public LocalDate getPaidDt() {
    return paidDt;
  }

  public void setPaidDt(LocalDate paidDt) {
    this.paidDt = paidDt;
  }

  public String getAdmSource() {
    return admSource;
  }

  public void setAdmSource(String admSource) {
    this.admSource = admSource;
  }

  public long getBeneHistoryId() {
    return beneHistoryId;
  }

  public void setBeneHistoryId(long beneHistoryId) {
    this.beneHistoryId = beneHistoryId;
  }

  public Set<FissProcCode> getProcCodes() {
    return procCodes;
  }

  public void setProcCodes(Set<FissProcCode> procCodes) {
    this.procCodes = procCodes;
  }

  public Set<FissPayer> getPayers() {
    return payers;
  }

  public void setPayers(Set<FissPayer> payers) {
    this.payers = payers;
  }

  public Mbi getMbiRecord() {
    return mbiRecord;
  }

  public void setMbiRecord(Mbi mbiRecord) {
    this.mbiRecord = mbiRecord;
  }

  public String getDirectMbi() {
    return mbiRecord == null ? null : mbiRecord.getMbi();
  }

  public String getDirectMbiHash() {
    return mbiRecord == null ? null : mbiRecord.getHash();
  }

  public enum ServTypeCdMapping {
    Normal,

    Clinic,

    SpecialFacility,

    Unrecognized
  }

  /**
   * Defines extra field names. Lombok will append all of the other fields to this class automatically.
   */
  public static class Fields {
    public static final String first = "first";

    public static final String second = "Second";
  }
}
