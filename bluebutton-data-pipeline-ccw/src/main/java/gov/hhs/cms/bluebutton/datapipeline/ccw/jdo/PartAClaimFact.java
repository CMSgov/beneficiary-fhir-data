package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_PTA_FACT</code> table, which represents
 * inpatient (Part A) and outpatient (Part B, confusingly enough) claims.
 */
@PersistenceCapable(table = "CCW_PTA_FACT", detachable = "true")
public class PartAClaimFact {
	@PrimaryKey
	@Persistent
	@Column(name = "CLM_ID")
	private Long id;

	@Persistent
	@Column(name = "BENE_ID")
	private CurrentBeneficiary beneficiary;

	@Persistent
	@Column(name = "CLM_TYPE_ID")
	private AllClaimsProfile claimProfile;

	@Persistent
	@Column(name = "DRG_ID")
	private DiagnosisRelatedGroup diagnosisGroup;

	@Persistent
	@Column(name = "CLM_ADMSN_DT")
	private LocalDate dateAdmission;

	@Persistent
	@Column(name = "CLM_FROM_DT")
	private LocalDate dateFrom;

	@Persistent
	@Column(name = "CLM_THRU_DT")
	private LocalDate dateThrough;

	@Persistent
	@Column(name = "NCH_BENE_DSCHRG_DT")
	private LocalDate dateDischarge;

	@Persistent
	@Column(name = "PRVDR_AT_TIME_OF_CLM_ID")
	private Long providerAtTimeOfClaimNpi;

	@Persistent
	@Column(name = "CLM_UTLZTN_DAY_CNT")
	private Long utilizationDayCount;

	@Persistent
	@Column(name = "CLM_PMT_AMT")
	private BigDecimal payment;

	@Persistent
	@Column(name = "CLM_PASS_THRU_PER_DIEM_AMT")
	private BigDecimal passThroughPerDiemAmount;

	@Persistent
	@Column(name = "NCH_BENE_BLOOD_DDCTBL_LBLTY_AM")
	private BigDecimal nchBeneficiaryBloodDeductibleLiability;

	@Persistent
	@Column(name = "NCH_BENE_IP_DDCTBL_AMT")
	private BigDecimal nchBeneficiaryInpatientDeductible;

	@Persistent
	@Column(name = "NCH_BENE_PTA_COINSRNC_LBLTY_AM")
	private BigDecimal nchBeneficiaryPartACoinsuranceLiability;

	@Persistent
	@Column(name = "NCH_BENE_PTB_DDCTBL_AMT")
	private BigDecimal nchBeneficiaryPartBDeductible;

	@Persistent
	@Column(name = "NCH_BENE_PTB_COINSRNC_AMT")
	private BigDecimal nchBeneficiaryPartBCoinsurance;

	@Persistent
	@Column(name = "NCH_PRMRY_PYR_CLM_PD_AMT")
	private BigDecimal nchPrimaryPayerPaid;

	@Persistent
	@Column(name = "ATNDG_PHYSN_ID")
	private Long attendingPhysicianNpi;

	@Persistent
	@Column(name = "CLM_OPRTG_PHYSN_ID")
	private Long operatingPhysicianNpi;

	@Persistent
	@Column(name = "CLM_OTHER_PHYSN_ID")
	private Long otherPhysicianNpi;

	@Persistent
	@Column(name = "ADMTG_DGNS_CD")
	private String admittingDiagnosisCode;

	@Persistent(mappedBy = "claim")
	@Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "lineNumber ASC"))
	private List<PartAClaimRevLineFact> claimLines = new ArrayList<>();

	/**
	 * Constructs a new {@link PartAClaimFact} instance.
	 */
	public PartAClaimFact() {
	}

	/**
	 * @return the beneficiary's ID
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the beneficiary that this claim is for
	 */
	public CurrentBeneficiary getBeneficiary() {
		return beneficiary;
	}

	/**
	 * @param beneficiary
	 *            the new value for {@link #getBeneficiary()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setBeneficiary(CurrentBeneficiary beneficiary) {
		this.beneficiary = beneficiary;
		return this;
	}

	/**
	 * @return the {@link AllClaimsProfile} that specifies this
	 *         {@link PartAClaimFact}'s {@link ClaimType} (and other metadata)
	 */
	public AllClaimsProfile getClaimProfile() {
		return claimProfile;
	}

	/**
	 * @param claimProfile
	 *            the new value for {@link #getClaimProfile()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setClaimProfile(AllClaimsProfile claimProfile) {
		this.claimProfile = claimProfile;
		return this;
	}

	/**
	 * @return the {@link DiagnosisRelatedGroup} for this {@link PartAClaimFact}
	 */
	public DiagnosisRelatedGroup getDiagnosisGroup() {
		return diagnosisGroup;
	}

	/**
	 * @param diagnosisGroup
	 *            the new value for {@link #getDiagnosisGroup()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setDiagnosisGroup(DiagnosisRelatedGroup diagnosisGroup) {
		this.diagnosisGroup = diagnosisGroup;
		return this;
	}

	/**
	 * @return TODO
	 */
	public LocalDate getDateAdmission() {
		return dateAdmission;
	}

	/**
	 * @param dateAdmission
	 *            the new value for {@link #getDateAdmission()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setDateAdmission(LocalDate dateAdmission) {
		this.dateAdmission = dateAdmission;
		return this;
	}

	/**
	 * @return TODO
	 */
	public LocalDate getDateFrom() {
		return dateFrom;
	}

	/**
	 * @param dateFrom
	 *            the new value for {@link #getDateFrom()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setDateFrom(LocalDate dateFrom) {
		this.dateFrom = dateFrom;
		return this;
	}

	/**
	 * @return TODO
	 */
	public LocalDate getDateThrough() {
		return dateThrough;
	}

	/**
	 * @return TODO
	 */
	public LocalDate getDateDischarge() {
		return dateDischarge;
	}

	/**
	 * @param dateDischarge
	 *            the new value for {@link #getDateDischarge()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setDateDischarge(LocalDate dateDischarge) {
		this.dateDischarge = dateDischarge;
		return this;
	}

	/**
	 * @param dateThrough
	 *            the new value for {@link #getDateThrough()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setDateThrough(LocalDate dateThrough) {
		this.dateThrough = dateThrough;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getProviderAtTimeOfClaimNpi() {
		return providerAtTimeOfClaimNpi;
	}

	/**
	 * @param providerAtTimeOfClaimNpi
	 *            the new value for {@link #getProviderAtTimeOfClaimNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setProviderAtTimeOfClaimNpi(Long providerAtTimeOfClaimNpi) {
		this.providerAtTimeOfClaimNpi = providerAtTimeOfClaimNpi;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getUtilizationDayCount() {
		return utilizationDayCount;
	}

	/**
	 * @param utilizationDayCount
	 *            the new value for {@link #getUtilizationDayCount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setUtilizationDayCount(Long utilizationDayCount) {
		this.utilizationDayCount = utilizationDayCount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getPayment() {
		return payment;
	}

	/**
	 * @param payment
	 *            the new value for {@link #getPayment()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setPayment(BigDecimal payment) {
		this.payment = payment;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getPassThroughPerDiemAmount() {
		return passThroughPerDiemAmount;
	}

	/**
	 * @param passThroughPerDiemAmount
	 *            the new value for {@link #getPassThroughPerDiemAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setPassThroughPerDiemAmount(BigDecimal passThroughPerDiemAmount) {
		this.passThroughPerDiemAmount = passThroughPerDiemAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchBeneficiaryBloodDeductibleLiability() {
		return nchBeneficiaryBloodDeductibleLiability;
	}

	/**
	 * @param nchBeneficiaryBloodDeductibleLiability
	 *            the new value for
	 *            {@link #getNchBeneficiaryBloodDeductibleLiability()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchBeneficiaryBloodDeductibleLiability(BigDecimal nchBeneficiaryBloodDeductibleLiability) {
		this.nchBeneficiaryBloodDeductibleLiability = nchBeneficiaryBloodDeductibleLiability;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchBeneficiaryInpatientDeductible() {
		return nchBeneficiaryInpatientDeductible;
	}

	/**
	 * @param nchBeneficiaryInpatientDeductible
	 *            the new value for
	 *            {@link #getNchBeneficiaryInpatientDeductible()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchBeneficiaryInpatientDeductible(BigDecimal nchBeneficiaryInpatientDeductible) {
		this.nchBeneficiaryInpatientDeductible = nchBeneficiaryInpatientDeductible;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchBeneficiaryPartACoinsuranceLiability() {
		return nchBeneficiaryPartACoinsuranceLiability;
	}

	/**
	 * @param nchBeneficiaryPartACoinsuranceLiability
	 *            the new value for
	 *            {@link #getNchBeneficiaryPartACoinsuranceLiability()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchBeneficiaryPartACoinsuranceLiability(
			BigDecimal nchBeneficiaryPartACoinsuranceLiability) {
		this.nchBeneficiaryPartACoinsuranceLiability = nchBeneficiaryPartACoinsuranceLiability;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchBeneficiaryPartBDeductible() {
		return nchBeneficiaryPartBDeductible;
	}

	/**
	 * @param nchBeneficiaryPartBDeductible
	 *            the new value for {@link #getNchBeneficiaryPartBDeductible()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchBeneficiaryPartBDeductible(BigDecimal nchBeneficiaryPartBDeductible) {
		this.nchBeneficiaryPartBDeductible = nchBeneficiaryPartBDeductible;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchBeneficiaryPartBCoinsurance() {
		return nchBeneficiaryPartBCoinsurance;
	}

	/**
	 * @param nchBeneficiaryPartBCoinsurance
	 *            the new value for {@link #getNchBeneficiaryPartBCoinsurance()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchBeneficiaryPartBCoinsurance(BigDecimal nchBeneficiaryPartBCoinsurance) {
		this.nchBeneficiaryPartBCoinsurance = nchBeneficiaryPartBCoinsurance;
		return this;
	}

	/**
	 * @return TODO
	 */
	public BigDecimal getNchPrimaryPayerPaid() {
		return nchPrimaryPayerPaid;
	}

	/**
	 * @param nchPrimaryPayerPaid
	 *            the new value for {@link #getNchPrimaryPayerPaid()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setNchPrimaryPayerPaid(BigDecimal nchPrimaryPayerPaid) {
		this.nchPrimaryPayerPaid = nchPrimaryPayerPaid;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getAttendingPhysicianNpi() {
		return attendingPhysicianNpi;
	}

	/**
	 * @param attendingPhysicianNpi
	 *            the new value for {@link #getAttendingPhysicianNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setAttendingPhysicianNpi(Long attendingPhysicianNpi) {
		this.attendingPhysicianNpi = attendingPhysicianNpi;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getOperatingPhysicianNpi() {
		return operatingPhysicianNpi;
	}

	/**
	 * @param operatingPhysicianNpi
	 *            the new value for {@link #getOperatingPhysicianNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setOperatingPhysicianNpi(Long operatingPhysicianNpi) {
		this.operatingPhysicianNpi = operatingPhysicianNpi;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getOtherPhysicianNpi() {
		return otherPhysicianNpi;
	}

	/**
	 * @param otherPhysicianNpi
	 *            the new value for {@link #getOtherPhysicianNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setOtherPhysicianNpi(Long otherPhysicianNpi) {
		this.otherPhysicianNpi = otherPhysicianNpi;
		return this;
	}

	/**
	 * @return the ICD-9 diagnosis code that the beneficiary was admitted for
	 */
	public String getAdmittingDiagnosisCode() {
		return admittingDiagnosisCode;
	}

	/**
	 * @param admittingDiagnosisCode
	 *            the new value for {@link #getBeneficiary()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimFact setAdmittingDiagnosisCode(String admittingDiagnosisCode) {
		this.admittingDiagnosisCode = admittingDiagnosisCode;
		return this;
	}

	/**
	 * @return the {@link PartAClaimRevLineFact}s associated with this
	 *         {@link PartAClaimFact}
	 */
	public List<PartAClaimRevLineFact> getClaimLines() {
		return claimLines;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartAClaimFact [id=");
		builder.append(id);
		builder.append(", beneficiary.id=");
		builder.append(beneficiary != null ? beneficiary.getId() : "null");
		builder.append(", claimProfile=");
		builder.append(claimProfile);
		builder.append(", diagnosisGroup=");
		builder.append(diagnosisGroup);
		builder.append(", dateAdmission=");
		builder.append(dateAdmission);
		builder.append(", dateFrom=");
		builder.append(dateFrom);
		builder.append(", dateThrough=");
		builder.append(dateThrough);
		builder.append(", dateDischarge=");
		builder.append(dateDischarge);
		builder.append(", providerAtTimeOfClaimNpi=");
		builder.append(providerAtTimeOfClaimNpi);
		builder.append(", utilizationDayCount=");
		builder.append(utilizationDayCount);
		builder.append(", payment=");
		builder.append(payment);
		builder.append(", passThroughPerDiemAmount=");
		builder.append(passThroughPerDiemAmount);
		builder.append(", nchBeneficiaryBloodDeductibleLiability=");
		builder.append(nchBeneficiaryBloodDeductibleLiability);
		builder.append(", nchBeneficiaryInpatientDeductible=");
		builder.append(nchBeneficiaryInpatientDeductible);
		builder.append(", nchBeneficiaryPartACoinsuranceLiability=");
		builder.append(nchBeneficiaryPartACoinsuranceLiability);
		builder.append(", nchBeneficiaryPartBDeductible=");
		builder.append(nchBeneficiaryPartBDeductible);
		builder.append(", nchBeneficiaryPartBCoinsurance=");
		builder.append(nchBeneficiaryPartBCoinsurance);
		builder.append(", nchPrimaryPayerPaid=");
		builder.append(nchPrimaryPayerPaid);
		builder.append(", attendingPhysicianNpi=");
		builder.append(attendingPhysicianNpi);
		builder.append(", operatingPhysicianNpi=");
		builder.append(operatingPhysicianNpi);
		builder.append(", otherPhysicianNpi=");
		builder.append(otherPhysicianNpi);
		builder.append(", admittingDiagnosisCode=");
		builder.append(admittingDiagnosisCode);
		builder.append(", claimLines=");
		builder.append(claimLines);
		builder.append("]");
		return builder.toString();
	}
}
