package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.time.LocalDate;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.datanucleus.identity.SingleFieldId;

/**
 * Models rows in the CCW's <code>CCW_PTB_LINE_FACT</code> table, which
 * represents Part B claim lines.
 */
@PersistenceCapable(table = "CCW_PTB_LINE_FACT", objectIdClass = PartBClaimLineFactPk.class, detachable = "true")
public class PartBClaimLineFact {
	@PrimaryKey
	@Persistent
	@Column(name = "CLM_ID")
	private PartBClaimFact claim;

	@PrimaryKey
	@Persistent
	@Column(name = "LINE_NUM")
	private long lineNumber;

	@Persistent
	@Column(name = "BENE_ID")
	private CurrentBeneficiary beneficiary;

	@Persistent
	@Column(name = "HCPCS_ID")
	private Procedure procedure;

	@Persistent
	@Column(name = "CLM_FROM_DT")
	private LocalDate dateFrom;

	@Persistent
	@Column(name = "CLM_THRU_DT")
	private LocalDate dateThrough;

	@Persistent
	@Column(name = "LINE_ALOWD_CHRG_AMT")
	private Double allowedAmount;

	@Persistent
	@Column(name = "LINE_BENE_PTB_DDCTBL_AMT")
	private Double deductibleAmount;

	@Persistent
	@Column(name = "LINE_BENE_PRMRY_PYR_PD_AMT")
	private Double beneficiaryPrimaryPayerPaidAmount;

	@Persistent
	@Column(name = "LINE_COINSRNC_AMT")
	private Double coinsuranceAmount;

	@Persistent
	@Column(name = "LINE_NCH_PMT_AMT")
	private Double nchPaymentAmount;

	@Persistent
	@Column(name = "LINE_DGNS_CD")
	private String lineDiagnosisCode;

	@Persistent
	@Column(name = "CLM_LINE_MISC_CD_ID")
	private ClaimLineMiscCode miscCode;

	@Persistent
	@Column(name = "LINE_PRCSG_IND_CD")
	private String processingIndicationCode;

	/**
	 * Constructs a new {@link PartBClaimLineFact} instance.
	 */
	public PartBClaimLineFact() {
	}

	/**
	 * @return the {@link PartBClaimFact} that this {@link PartBClaimLineFact}
	 *         is part of
	 */
	public PartBClaimFact getClaim() {
		return claim;
	}

	/**
	 * @param claim
	 *            the new value for {@link #getClaim()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setClaim(PartBClaimFact claim) {
		this.claim = claim;
		return this;
	}

	/**
	 * @return the line number of this {@link PartBClaimLineFact} in its parent
	 *         {@link PartBClaimFact}
	 */
	public long getLineNumber() {
		return lineNumber;
	}

	/**
	 * @param lineNumber
	 *            the new value for {@link #getLineNumber()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setLineNumber(long lineNumber) {
		this.lineNumber = lineNumber;
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
	public PartBClaimLineFact setBeneficiary(CurrentBeneficiary beneficiary) {
		this.beneficiary = beneficiary;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Procedure getProcedure() {
		return procedure;
	}

	/**
	 * @param procedure
	 *            the new value for {@link #getProcedure()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setProcedure(Procedure procedure) {
		this.procedure = procedure;
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
	public PartBClaimLineFact setDateFrom(LocalDate dateFrom) {
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
	 * @param dateThrough
	 *            the new value for {@link #getDateThrough()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setDateThrough(LocalDate dateThrough) {
		this.dateThrough = dateThrough;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getAllowedAmount() {
		return allowedAmount;
	}

	/**
	 * @param allowedAmount
	 *            the new value for {@link #getAllowedAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setAllowedAmount(Double allowedAmount) {
		this.allowedAmount = allowedAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getDeductibleAmount() {
		return deductibleAmount;
	}

	/**
	 * @param deductibleAmount
	 *            the new value for {@link #getDeductibleAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setDeductibleAmount(Double deductibleAmount) {
		this.deductibleAmount = deductibleAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getBeneficiaryPrimaryPayerPaidAmount() {
		return beneficiaryPrimaryPayerPaidAmount;
	}

	/**
	 * @param beneficiaryPrimaryPayerPaidAmount
	 *            the new value for
	 *            {@link #getBeneficiaryPrimaryPayerPaidAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setBeneficiaryPrimaryPayerPaidAmount(Double beneficiaryPrimaryPayerPaidAmount) {
		this.beneficiaryPrimaryPayerPaidAmount = beneficiaryPrimaryPayerPaidAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getCoinsuranceAmount() {
		return coinsuranceAmount;
	}

	/**
	 * @param coinsuranceAmount
	 *            the new value for {@link #getCoinsuranceAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setCoinsuranceAmount(Double coinsuranceAmount) {
		this.coinsuranceAmount = coinsuranceAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getNchPaymentAmount() {
		return nchPaymentAmount;
	}

	/**
	 * @param nchPaymentAmount
	 *            the new value for {@link #getNchPaymentAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setNchPaymentAmount(Double nchPaymentAmount) {
		this.nchPaymentAmount = nchPaymentAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getLineDiagnosisCode() {
		return lineDiagnosisCode;
	}

	/**
	 * @param lineDiagnosisCode
	 *            the new value for {@link #getLineDiagnosisCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setLineDiagnosisCode(String lineDiagnosisCode) {
		this.lineDiagnosisCode = lineDiagnosisCode;
		return this;
	}

	/**
	 * @return TODO
	 */
	public ClaimLineMiscCode getMiscCode() {
		return miscCode;
	}

	/**
	 * @param miscCode
	 *            the new value for {@link #getMiscCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setMiscCode(ClaimLineMiscCode miscCode) {
		this.miscCode = miscCode;
		return this;
	}

	/**
	 * @return TODO
	 */
	public String getProcessingIndicationCode() {
		return processingIndicationCode;
	}

	/**
	 * @param processingIndicationCode
	 *            the new value for {@link #getProcessingIndicationCode()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartBClaimLineFact setProcessingIndicationCode(String processingIndicationCode) {
		this.processingIndicationCode = processingIndicationCode;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartBClaimLineFact [claim.id=");
		builder.append(claim != null ? claim.getId() : "null");
		builder.append(", lineNumber=");
		builder.append(lineNumber);
		builder.append(", beneficiary.id=");
		builder.append(beneficiary != null ? beneficiary.getId() : "null");
		builder.append(", procedure=");
		builder.append(procedure);
		builder.append(", dateFrom=");
		builder.append(dateFrom);
		builder.append(", dateThrough=");
		builder.append(dateThrough);
		builder.append(", allowedAmount=");
		builder.append(allowedAmount);
		builder.append(", deductibleAmount=");
		builder.append(deductibleAmount);
		builder.append(", beneficiaryPrimaryPayerPaidAmount=");
		builder.append(beneficiaryPrimaryPayerPaidAmount);
		builder.append(", coinsuranceAmount=");
		builder.append(coinsuranceAmount);
		builder.append(", nchPaymentAmount=");
		builder.append(nchPaymentAmount);
		builder.append(", lineDiagnosisCode=");
		builder.append(lineDiagnosisCode);
		builder.append(", miscCode=");
		builder.append(miscCode);
		builder.append(", processingIndicationCode=");
		builder.append(processingIndicationCode);
		builder.append("]");
		return builder.toString();
	}
}
