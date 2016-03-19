package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CLM_LINE_MISC_CD</code> table, which
 * represents miscellaneous codes assigned to claim lines.
 */
@PersistenceCapable(table = "CLM_LINE_MISC_CD", detachable = "true")
public class ClaimLineMiscCode {
	@PrimaryKey
	@Persistent
	@Column(name = "CLM_LINE_MISC_CD_ID")
	private String id;

	@Persistent
	@Column(name = "LINE_TYPE_SRVC_CD")
	private String code;

	/**
	 * Constructs a new {@link ClaimLineMiscCode} instance.
	 */
	public ClaimLineMiscCode() {
	}

	/**
	 * @return the {@link ClaimLineMiscCode}'s ID/PK
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public ClaimLineMiscCode setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the text code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the new value for {@link #getCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public ClaimLineMiscCode setCode(String code) {
		this.code = code;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Procedure [id=");
		builder.append(id);
		builder.append(", code=");
		builder.append(code);
		builder.append("]");
		return builder.toString();
	}
}
