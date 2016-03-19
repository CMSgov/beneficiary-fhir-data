package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.io.Serializable;
import java.util.StringTokenizer;

import javax.jdo.annotations.PersistenceCapable;

import org.datanucleus.identity.LongId;

/**
 * The {@link PersistenceCapable#objectIdClass()} implementation for
 * {@link PartBClaimLineFact}. This is used internally by JDO, and is derived
 * from the examples at <a href=
 * "http://www.datanucleus.org/products/accessplatform_5_0/jdo/orm/compound_identity.html">
 * DataNucleus: JDO : Compound Identity Relationships </a>.
 */
public class PartBClaimLineFactPk implements Serializable {
	private static final long serialVersionUID = 3025612233644878711L;

	public LongId claim;
	public long lineNumber;

	/**
	 * Constructs a new {@link PartBClaimLineFactPk} instance.
	 */
	public PartBClaimLineFactPk() {
	}

	/**
	 * Constructs a new {@link PartBClaimLineFactPk} instance.
	 * 
	 * @param serializedState
	 *            the {@link PartBClaimLineFactPk#toString()} value to be
	 *            deserialized
	 */
	public PartBClaimLineFactPk(String serializedState) {
		StringTokenizer token = new StringTokenizer(serializedState, "::");
		this.claim = new LongId(PartBClaimFact.class, token.nextToken());
		this.lineNumber = Long.valueOf(token.nextToken()).longValue();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "" + this.claim.toString() + "::" + this.lineNumber;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return claim.hashCode() ^ ((int) lineNumber);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other != null && (other instanceof PartBClaimLineFactPk)) {
			PartBClaimLineFactPk otherPK = (PartBClaimLineFactPk) other;
			return this.claim.equals(otherPK.claim) && otherPK.lineNumber == this.lineNumber;
		}
		return false;
	}
}