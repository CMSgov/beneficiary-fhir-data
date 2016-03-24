package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_PTA_REV_LINE_FACT</code> table, which
 * represents revenue center trailers/lines for inpatient (Part A) and
 * outpatient (Part B, confusingly enough) claims.
 */
@PersistenceCapable(table = "CCW_PTA_REV_LINE_FACT", objectIdClass = PartAClaimRevLineFactPk.class, detachable = "true")
public class PartAClaimRevLineFact {
	@PrimaryKey
	@Persistent
	@Column(name = "CLM_ID")
	private PartAClaimFact claim;

	@PrimaryKey
	@Persistent
	@Column(name = "CLM_LINE_NUM")
	private long lineNumber;

	@Persistent
	@Column(name = "HCPCS_ID")
	private Procedure revenueCenter;

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
	@Column(name = "DGNS_9_CD")
	private String diagnosisCode9;

	@Persistent
	@Column(name = "DGNS_10_CD")
	private String diagnosisCode10;

	@Persistent
	@Column(name = "PRCDR_1_CD")
	private String procedureCode1;

	@Persistent
	@Column(name = "PRCDR_2_CD")
	private String procedureCode2;

	@Persistent
	@Column(name = "PRCDR_3_CD")
	private String procedureCode3;

	@Persistent
	@Column(name = "PRCDR_4_CD")
	private String procedureCode4;

	@Persistent
	@Column(name = "PRCDR_5_CD")
	private String procedureCode5;

	@Persistent
	@Column(name = "PRCDR_6_CD")
	private String procedureCode6;

	/**
	 * Constructs a new {@link PartAClaimRevLineFact} instance.
	 */
	public PartAClaimRevLineFact() {
	}

	/**
	 * @return the {@link PartAClaimFact} that this
	 *         {@link PartAClaimRevLineFact} is part of
	 */
	public PartAClaimFact getClaim() {
		return claim;
	}

	/**
	 * @param claim
	 *            the new value for {@link #getClaim()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setClaim(PartAClaimFact claim) {
		this.claim = claim;
		return this;
	}

	/**
	 * @return the line number of this {@link PartAClaimRevLineFact} in its
	 *         parent {@link PartAClaimFact}
	 */
	public long getLineNumber() {
		return lineNumber;
	}

	/**
	 * @param lineNumber
	 *            the new value for {@link #getLineNumber()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setLineNumber(long lineNumber) {
		this.lineNumber = lineNumber;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Procedure getRevenueCenter() {
		return revenueCenter;
	}

	/**
	 * @param revenueCenter
	 *            the new value for {@link #getRevenueCenter()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setRevenueCenter(Procedure revenueCenter) {
		this.revenueCenter = revenueCenter;
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
	public PartAClaimRevLineFact setDiagnosisCode1(String diagnosisCode1) {
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
	public PartAClaimRevLineFact setDiagnosisCode2(String diagnosisCode2) {
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
	public PartAClaimRevLineFact setDiagnosisCode3(String diagnosisCode3) {
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
	public PartAClaimRevLineFact setDiagnosisCode4(String diagnosisCode4) {
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
	public PartAClaimRevLineFact setDiagnosisCode5(String diagnosisCode5) {
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
	public PartAClaimRevLineFact setDiagnosisCode6(String diagnosisCode6) {
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
	public PartAClaimRevLineFact setDiagnosisCode7(String diagnosisCode7) {
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
	public PartAClaimRevLineFact setDiagnosisCode8(String diagnosisCode8) {
		this.diagnosisCode8 = diagnosisCode8;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode9() {
		return diagnosisCode9;
	}

	/**
	 * @param diagnosisCode9
	 *            the new value for {@link #getDiagnosisCode9()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setDiagnosisCode9(String diagnosisCode9) {
		this.diagnosisCode9 = diagnosisCode9;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getDiagnosisCode10() {
		return diagnosisCode10;
	}

	/**
	 * @param diagnosisCode10
	 *            the new value for {@link #getDiagnosisCode10()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setDiagnosisCode10(String diagnosisCode10) {
		this.diagnosisCode10 = diagnosisCode10;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode1() {
		return procedureCode1;
	}

	/**
	 * @param procedureCode1
	 *            the new value for {@link #getProcedureCode1()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode1(String procedureCode1) {
		this.procedureCode1 = procedureCode1;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode2() {
		return procedureCode2;
	}

	/**
	 * @param procedureCode2
	 *            the new value for {@link #getProcedureCode2()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode2(String procedureCode2) {
		this.procedureCode2 = procedureCode2;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode3() {
		return procedureCode3;
	}

	/**
	 * @param procedureCode3
	 *            the new value for {@link #getProcedureCode3()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode3(String procedureCode3) {
		this.procedureCode3 = procedureCode3;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode4() {
		return procedureCode4;
	}

	/**
	 * @param procedureCode4
	 *            the new value for {@link #getProcedureCode4()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode4(String procedureCode4) {
		this.procedureCode4 = procedureCode4;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode5() {
		return procedureCode5;
	}

	/**
	 * @param procedureCode5
	 *            the new value for {@link #getProcedureCode5()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode5(String procedureCode5) {
		this.procedureCode5 = procedureCode5;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcedureCode6() {
		return procedureCode6;
	}

	/**
	 * @param procedureCode6
	 *            the new value for {@link #getProcedureCode6()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartAClaimRevLineFact setProcedureCode6(String procedureCode6) {
		this.procedureCode6 = procedureCode6;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartAClaimRevLineFact [claim.id=");
		builder.append(claim != null ? claim.getId() : "null");
		builder.append(", lineNumber=");
		builder.append(lineNumber);
		builder.append(", revenueCenter=");
		builder.append(revenueCenter);
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
		builder.append(", diagnosisCode9=");
		builder.append(diagnosisCode9);
		builder.append(", diagnosisCode10=");
		builder.append(diagnosisCode10);
		builder.append(", procedureCode1=");
		builder.append(procedureCode1);
		builder.append(", procedureCode2=");
		builder.append(procedureCode2);
		builder.append(", procedureCode3=");
		builder.append(procedureCode3);
		builder.append(", procedureCode4=");
		builder.append(procedureCode4);
		builder.append(", procedureCode5=");
		builder.append(procedureCode5);
		builder.append(", procedureCode6=");
		builder.append(procedureCode6);
		builder.append("]");
		return builder.toString();
	}
}
