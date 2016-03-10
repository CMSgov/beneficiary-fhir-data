package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_BENE_CRNT_VW</code> table, which represent
 * current Medicare beneficiaries.
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
	@Column(name = "DGNS_1_CD")
	private String admittingDiagnosisCode;

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
}
