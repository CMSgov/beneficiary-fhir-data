package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_DRG</code> table, which is used to
 * classify claims' diagnoses in the CCW.
 */
@PersistenceCapable(table = "CCW_DRG", detachable = "true")
public class DiagnosisRelatedGroup {
	@PrimaryKey
	@Persistent
	@Column(name = "DRG_ID")
	private Long id;

	@Persistent
	@Column(name = "CLM_DRG_CD")
	private String code;

	/**
	 * Constructs a new {@link DiagnosisRelatedGroup} instance.
	 */
	public DiagnosisRelatedGroup() {
	}

	/**
	 * @return the {@link DiagnosisRelatedGroup}'s ID/PK
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public DiagnosisRelatedGroup setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the code used to represent this {@link DiagnosisRelatedGroup} in
	 *         claims
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the new value for {@link #getCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public DiagnosisRelatedGroup setCode(String code) {
		this.code = code;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DiagnosisRelatedGroup [id=");
		builder.append(id);
		builder.append(", code=");
		builder.append(code);
		builder.append("]");
		return builder.toString();
	}
}
