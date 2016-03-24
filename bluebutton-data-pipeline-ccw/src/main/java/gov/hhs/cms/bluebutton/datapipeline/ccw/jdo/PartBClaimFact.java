package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_PTB_FACT</code> table, which represents
 * Part B claims.
 */
@PersistenceCapable(table = "CCW_PTB_FACT", detachable = "true")
public class PartBClaimFact {
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
	@Column(name = "CARR_CLM_CNTL_NUM")
	private Long carrierControlNumber;

	@Persistent
	@Column(name = "DGNS_1_CD")
	private String diagnosisCode1;

	@Persistent
	@Column(name = "DGNS_2_CD")
	private String diagnosisCode2;

	@Persistent
	@Column(name = "DGNS_3_CD")
	private String diagnosisCode3;

	@Persistent
	@Column(name = "DGNS_4_CD")
	private String diagnosisCode4;

	@Persistent
	@Column(name = "DGNS_5_CD")
	private String diagnosisCode5;

	@Persistent
	@Column(name = "DGNS_6_CD")
	private String diagnosisCode6;

	@Persistent
	@Column(name = "DGNS_7_CD")
	private String diagnosisCode7;

	@Persistent
	@Column(name = "DGNS_8_CD")
	private String diagnosisCode8;

	@Persistent
	@Column(name = "PRVDR_ID")
	private Long providerNpi;

	@Persistent(mappedBy = "claim")
	@Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "lineNumber ASC"))
	private List<PartBClaimLineFact> claimLines = new ArrayList<>();

	/**
	 * Constructs a new {@link PartBClaimFact} instance.
	 */
	public PartBClaimFact() {
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
	public PartBClaimFact setId(Long id) {
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
	public PartBClaimFact setBeneficiary(CurrentBeneficiary beneficiary) {
		this.beneficiary = beneficiary;
		return this;
	}

	/**
	 * @return the {@link AllClaimsProfile} that specifies this
	 *         {@link PartBClaimFact}'s {@link ClaimType} (and other metadata)
	 */
	public AllClaimsProfile getClaimProfile() {
		return claimProfile;
	}

	/**
	 * @param claimProfile
	 *            the new value for {@link #getClaimProfile()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setClaimProfile(AllClaimsProfile claimProfile) {
		this.claimProfile = claimProfile;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getCarrierControlNumber() {
		return carrierControlNumber;
	}

	/**
	 * @param carrierControlNumber
	 *            the new value for {@link #getCarrierControlNumber()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setCarrierControlNumber(Long carrierControlNumber) {
		this.carrierControlNumber = carrierControlNumber;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode1() {
		return diagnosisCode1;
	}

	/**
	 * @param diagnosisCode1
	 *            the new value for {@link #getDiagnosisCode1()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode1(String diagnosisCode1) {
		this.diagnosisCode1 = diagnosisCode1;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode2() {
		return diagnosisCode2;
	}

	/**
	 * @param diagnosisCode2
	 *            the new value for {@link #getDiagnosisCode2()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode2(String diagnosisCode2) {
		this.diagnosisCode2 = diagnosisCode2;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode3() {
		return diagnosisCode3;
	}

	/**
	 * @param diagnosisCode3
	 *            the new value for {@link #getDiagnosisCode3()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode3(String diagnosisCode3) {
		this.diagnosisCode3 = diagnosisCode3;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode4() {
		return diagnosisCode4;
	}

	/**
	 * @param diagnosisCode4
	 *            the new value for {@link #getDiagnosisCode4()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode4(String diagnosisCode4) {
		this.diagnosisCode4 = diagnosisCode4;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode5() {
		return diagnosisCode5;
	}

	/**
	 * @param diagnosisCode5
	 *            the new value for {@link #getDiagnosisCode5()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode5(String diagnosisCode5) {
		this.diagnosisCode5 = diagnosisCode5;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode6() {
		return diagnosisCode6;
	}

	/**
	 * @param diagnosisCode6
	 *            the new value for {@link #getDiagnosisCode6()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode6(String diagnosisCode6) {
		this.diagnosisCode6 = diagnosisCode6;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode7() {
		return diagnosisCode7;
	}

	/**
	 * @param diagnosisCode7
	 *            the new value for {@link #getDiagnosisCode7()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode7(String diagnosisCode7) {
		this.diagnosisCode7 = diagnosisCode7;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode8() {
		return diagnosisCode8;
	}

	/**
	 * @param diagnosisCode8
	 *            the new value for {@link #getDiagnosisCode8()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setDiagnosisCode8(String diagnosisCode8) {
		this.diagnosisCode8 = diagnosisCode8;
		return this;
	}

	/**
	 * @return the NPI of the performing physician
	 */
	public Long getProviderNpi() {
		return providerNpi;
	}

	/**
	 * @param providerNpi
	 *            the new value for {@link #getProviderNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimFact setProviderNpi(Long providerNpi) {
		this.providerNpi = providerNpi;
		return this;
	}

	/**
	 * @return the {@link PartBClaimLineFact}s associated with this
	 *         {@link PartBClaimFact}
	 */
	public List<PartBClaimLineFact> getClaimLines() {
		return claimLines;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartBClaimFact [id=");
		builder.append(id);
		builder.append(", beneficiary=");
		builder.append(beneficiary != null ? beneficiary.getId() : "null");
		builder.append(", claimProfile=");
		builder.append(claimProfile);
		builder.append(", carrierControlNumber=");
		builder.append(carrierControlNumber);
		builder.append(", diagnosisCode1=");
		builder.append(diagnosisCode1);
		builder.append(", diagnosisCode2=");
		builder.append(diagnosisCode2);
		builder.append(", diagnosisCode3=");
		builder.append(diagnosisCode3);
		builder.append(", diagnosisCode4=");
		builder.append(diagnosisCode4);
		builder.append(", diagnosisCode5=");
		builder.append(diagnosisCode5);
		builder.append(", diagnosisCode6=");
		builder.append(diagnosisCode6);
		builder.append(", diagnosisCode7=");
		builder.append(diagnosisCode7);
		builder.append(", diagnosisCode8=");
		builder.append(diagnosisCode8);
		builder.append(", providerNpi=");
		builder.append(providerNpi);
		builder.append(", claimLines=");
		builder.append(claimLines);
		builder.append("]");
		return builder.toString();
	}
}
