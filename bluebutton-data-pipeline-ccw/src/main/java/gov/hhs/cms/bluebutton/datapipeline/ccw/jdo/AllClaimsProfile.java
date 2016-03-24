package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Convert;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Extensions;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_ALL_CLM_PRFL</code> table, which is used
 * to track metadata about the various types of claims in the CCW.
 */
@PersistenceCapable(table = "CCW_ALL_CLM_PRFL", detachable = "true")
public class AllClaimsProfile {
	@PrimaryKey
	@Persistent
	@Column(name = "CLM_TYPE_ID")
	private Long id;

	@Persistent
	@Column(name = "NCH_CLM_TYPE_CD")
	@Convert(value = ClaimTypeConverter.class)
	private ClaimType claimType;

	/**
	 * Constructs a new {@link AllClaimsProfile} instance.
	 */
	public AllClaimsProfile() {
	}

	/**
	 * @return the {@link AllClaimsProfile}'s ID/PK
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public AllClaimsProfile setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the {@link ClaimType} for this {@link AllClaimsProfile} entry
	 */
	public ClaimType getClaimType() {
		return claimType;
	}

	/**
	 * @param claimType
	 *            the new value for {@link #getClaimType()}
	 * @return this instance (for call-chaining purposes)
	 */
	public AllClaimsProfile setClaimType(ClaimType claimType) {
		this.claimType = claimType;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AllClaimsProfile [id=");
		builder.append(id);
		builder.append(", claimType=");
		builder.append(claimType);
		builder.append("]");
		return builder.toString();
	}
}
