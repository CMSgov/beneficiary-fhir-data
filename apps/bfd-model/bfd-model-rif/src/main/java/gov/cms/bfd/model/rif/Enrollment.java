package gov.cms.bfd.model.rif;

import java.util.Optional;
import javax.persistence.*;

@Entity
@Table(name = "`Enrollment`")
public class Enrollment {
  @Column(name = "`EnrollmentId`", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "enrollment_enrollmentId_seq")
  @SequenceGenerator(
      name = "enrollment_enrollmentId_seq",
      sequenceName = "enrollment_enrollmentId_seq",
      allocationSize = 10000)
  private long enrollmentId;

  @Id
  @Column(name = "`beneficiaryId`", nullable = false)
  private String beneficiaryId;

  @Id
  @Column(name = "`date", nullable = false)
  private String date;

  @Column(name = "`fipsStateCntyCode`", nullable = true)
  private String fipsStateCntyCode;

  @Column(name = "`medicareStatusCode`", nullable = true)
  private String medicareStatusCode;

  @Column(name = "`entitlementBuyInInd`", nullable = true)
  private Character entitlementBuyInInd;

  @Column(name = "`hmoIndicatorInd`", nullable = true)
  private Character hmoIndicatorInd;

  @Column(name = "`partCContractNumberId`", nullable = true)
  private String partCContractNumberId;

  @Column(name = "`partCPbpNumberId`", nullable = true)
  private String partCPbpNumberId;

  @Column(name = "`partCPlanTypeCode`", nullable = true)
  private String partCPlanTypeCode;

  @Column(name = "`partDContractNumberId`", nullable = true)
  private String partDContractNumberId;

  @Column(name = "`partDPbpNumberId`", nullable = true)
  private String partDPbpNumberId;

  @Column(name = "`partDSegmentNumberId`", nullable = true)
  private String partDSegmentNumberId;

  @Column(name = "`partDRetireeDrugSubsidyInd`", nullable = true)
  private Character partDRetireeDrugSubsidyInd;

  @Column(name = "`medicaidDualEligibilityCode`", nullable = true)
  private String medicaidDualEligibilityCode;

  @Column(name = "`partDLowIncomeCostShareGroupCode`", nullable = true)
  private String partDLowIncomeCostShareGroupCode;

  public Enrollment() {}

  public Enrollment(
      String beneficiaryId,
      String date,
      Optional<String> fipsStateCntyCode,
      Optional<String> medicareStatusCode,
      Optional<Character> entitlementBuyInInd,
      Optional<Character> hmoIndicatorInd,
      Optional<String> partCContractNumberId,
      Optional<String> partCPbpNumberId,
      Optional<String> partCPlanTypeCode,
      Optional<String> partDContractNumberId,
      Optional<String> partDPbpNumberId,
      Optional<String> partDSegmentNumberId,
      Optional<Character> partDRetireeDrugSubsidyInd,
      Optional<String> medicaidDualEligibilityCode,
      Optional<String> partDLowIncomeCostShareGroupCode) {
    this.beneficiaryId = beneficiaryId;
    this.date = date;
    this.fipsStateCntyCode = fipsStateCntyCode.get();
    this.medicareStatusCode = medicareStatusCode.get();
    this.entitlementBuyInInd = entitlementBuyInInd.get();
    this.hmoIndicatorInd = hmoIndicatorInd.get();
    this.partCContractNumberId = partCContractNumberId.get();
    this.partCPbpNumberId = partCPbpNumberId.get();
    this.partCPlanTypeCode = partCPlanTypeCode.get();
    this.partDContractNumberId = partDContractNumberId.get();
    this.partDPbpNumberId = partDPbpNumberId.get();
    this.partDSegmentNumberId = partDSegmentNumberId.get();
    this.partDRetireeDrugSubsidyInd = partDRetireeDrugSubsidyInd.get();
    this.medicaidDualEligibilityCode = medicaidDualEligibilityCode.get();
    this.partDLowIncomeCostShareGroupCode = partDLowIncomeCostShareGroupCode.get();
  }

  /** @return the identifier */
  public long getEnrollmentId() {
    return enrollmentId;
  }

  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  public void setBeneficiaryId(String beneficiaryId) {
    this.beneficiaryId = beneficiaryId;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public Optional<String> getFipsStateCntyCode() {
    return Optional.ofNullable(fipsStateCntyCode);
  }

  public void setFipsStateCntyCode(Optional<String> fipsStateCntyCode) {
    this.fipsStateCntyCode = fipsStateCntyCode.orElse(null);
  }

  public Optional<String> getMedicareStatusCode() {
    return Optional.ofNullable(medicareStatusCode);
  }

  public void setMedicareStatusCode(Optional<String> medicareStatusCode) {
    this.medicareStatusCode = medicareStatusCode.orElse(null);
  }

  public Optional<Character> getEntitlementBuyInInd() {
    return Optional.ofNullable(entitlementBuyInInd);
  }

  public void setEntitlementBuyInInd(Optional<Character> entitlementBuyInInd) {
    this.entitlementBuyInInd = entitlementBuyInInd.orElse(null);
  }

  public Optional<Character> getHmoIndicatorInd() {
    return Optional.ofNullable(hmoIndicatorInd);
  }

  public void setHmoIndicatorInd(Optional<Character> hmoIndicatorInd) {
    this.hmoIndicatorInd = hmoIndicatorInd.orElse(null);
  }

  public Optional<String> getPartCContractNumberId() {
    return Optional.ofNullable(partCContractNumberId);
  }

  public void setPartCContractNumberId(Optional<String> partCContractNumberId) {
    this.partCContractNumberId = partCContractNumberId.orElse(null);
  }

  public Optional<String> getPartCPbpNumberId() {
    return Optional.ofNullable(partCPbpNumberId);
  }

  public void setPartCPbpNumberId(Optional<String> partCPbpNumberId) {
    this.partCPbpNumberId = partCPbpNumberId.orElse(null);
  }

  public Optional<String> getPartCPlanTypeCode() {
    return Optional.ofNullable(partCPlanTypeCode);
  }

  public void setPartCPlanTypeCode(Optional<String> partCPlanTypeCode) {
    this.partCPlanTypeCode = partCPlanTypeCode.orElse(null);
  }

  public Optional<String> getPartDContractNumberId() {
    return Optional.ofNullable(partDContractNumberId);
  }

  public void setPartDContractNumberId(Optional<String> partDContractNumberId) {
    this.partDContractNumberId = partDContractNumberId.orElse(null);
  }

  public Optional<String> getPartDPbpNumberId() {
    return Optional.ofNullable(partDPbpNumberId);
  }

  public void setPartDPbpNumberId(Optional<String> partDPbpNumberId) {
    this.partDPbpNumberId = partDPbpNumberId.orElse(null);
  }

  public Optional<String> getPartDSegmentNumberId() {
    return Optional.ofNullable(partDSegmentNumberId);
  }

  public void setPartDSegmentNumberId(Optional<String> partDSegmentNumberId) {
    this.partDSegmentNumberId = partDSegmentNumberId.orElse(null);
  }

  public Optional<Character> getPartDRetireeDrugSubsidyInd() {
    return Optional.ofNullable(partDRetireeDrugSubsidyInd);
  }

  public void setPartDRetireeDrugSubsidyInd(Optional<Character> partDRetireeDrugSubsidyInd) {
    this.partDRetireeDrugSubsidyInd = partDRetireeDrugSubsidyInd.orElse(null);
  }

  public Optional<String> getMedicaidDualEligibilityCode() {
    return Optional.ofNullable(medicaidDualEligibilityCode);
  }

  public void setMedicaidDualEligibilityCode(Optional<String> medicaidDualEligibilityCode) {
    this.medicaidDualEligibilityCode = medicaidDualEligibilityCode.orElse(null);
  }

  public Optional<String> getPartDLowIncomeCostShareGroupCode() {
    return Optional.ofNullable(partDLowIncomeCostShareGroupCode);
  }

  public void setPartDLowIncomeCostShareGroupCode(
      Optional<String> partDLowIncomeCostShareGroupCode) {
    this.partDLowIncomeCostShareGroupCode = partDLowIncomeCostShareGroupCode.orElse(null);
  }
}
