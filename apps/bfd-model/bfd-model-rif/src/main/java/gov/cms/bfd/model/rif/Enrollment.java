package gov.cms.bfd.model.rif;

import java.io.Serializable;
import java.util.Optional;
import javax.persistence.*;

@SuppressWarnings("serial")
@Entity
@Table(name = "`Enrollment`")
public class Enrollment implements Serializable {
  @Id
  @Column(name = "`beneficiaryId`", nullable = false)
  private String beneficiaryId;

  @Id
  @Column(name = "`yearMonth`", nullable = false)
  private String yearMonth;

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
      String yearMonth,
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
    this.yearMonth = yearMonth;
    this.fipsStateCntyCode = fipsStateCntyCode.orElse(null);
    this.medicareStatusCode = medicareStatusCode.orElse(null);
    this.entitlementBuyInInd = entitlementBuyInInd.orElse(null);
    this.hmoIndicatorInd = hmoIndicatorInd.orElse(null);
    this.partCContractNumberId = partCContractNumberId.orElse(null);
    this.partCPbpNumberId = partCPbpNumberId.orElse(null);
    this.partCPlanTypeCode = partCPlanTypeCode.orElse(null);
    this.partDContractNumberId = partDContractNumberId.orElse(null);
    this.partDPbpNumberId = partDPbpNumberId.orElse(null);
    this.partDSegmentNumberId = partDSegmentNumberId.orElse(null);
    this.partDRetireeDrugSubsidyInd = partDRetireeDrugSubsidyInd.orElse(null);
    this.medicaidDualEligibilityCode = medicaidDualEligibilityCode.orElse(null);
    this.partDLowIncomeCostShareGroupCode = partDLowIncomeCostShareGroupCode.orElse(null);
  }

  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  public void setBeneficiaryId(String beneficiaryId) {
    this.beneficiaryId = beneficiaryId;
  }

  public String getYearMonth() {
    return yearMonth;
  }

  public void setYearMonth(String yearMonth) {
    this.yearMonth = yearMonth;
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
