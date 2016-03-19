package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_HCPCS_CD</code> table, which represents
 * healthcare/medical procedures.
 */
@PersistenceCapable(table = "CCW_HCPCS_CD", detachable = "true")
public class Procedure {
	@PrimaryKey
	@Persistent
	@Column(name = "HCPCS_ID")
	private Long id;

	@Persistent
	@Column(name = "HCPCS_CD")
	private String code;

	/**
	 * Constructs a new {@link Procedure} instance.
	 */
	public Procedure() {
	}

	/**
	 * @return the {@link Procedure}'s ID/PK
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public Procedure setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the text code for this procedure
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the new value for {@link #getCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public Procedure setCode(String code) {
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
