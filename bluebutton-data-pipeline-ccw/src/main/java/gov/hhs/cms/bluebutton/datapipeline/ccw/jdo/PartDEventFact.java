package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.time.LocalDate;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_PDE_FACT</code> table, which represents
 * Part D events.
 */
@PersistenceCapable(table = "CCW_PDE_FACT", detachable = "true")
public class PartDEventFact {
	@PrimaryKey
	@Persistent
	@Column(name = "PDE_ID")
	private Long id;

	@Persistent
	@Column(name = "PDE_PRSCRBR_PRFL_ID")
	private Long prescriberNpi;

	@Persistent
	@Column(name = "PDE_SRVC_PRVDR_PRFL_ID")
	private Long serviceProviderNpi;

	@Persistent
	@Column(name = "PDE_PROD_PRFL_ID")
	private Long productNdc;

	@Persistent
	@Column(name = "BENE_ID")
	private CurrentBeneficiary beneficiary;

	@Persistent
	@Column(name = "SRVC_DT")
	private LocalDate serviceDate;

	@Persistent
	@Column(name = "QTY_DSPNSD_NUM")
	private Long quantityDispensed;

	@Persistent
	@Column(name = "DAYS_SUPLY_NUM")
	private Long numberDaysSupply;

	@Persistent
	@Column(name = "PTNT_PAY_AMT")
	private Double patientPayAmount;

	@Persistent
	@Column(name = "TOT_RX_CST_AMT")
	private Double totalPrescriptionCost;

	/**
	 * Constructs a new {@link PartDEventFact} instance.
	 */
	public PartDEventFact() {
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
	public PartDEventFact setId(Long id) {
		this.id = id;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getPrescriberNpi() {
		return prescriberNpi;
	}

	/**
	 * @param prescriberNpi
	 *            the new value for {@link #getPrescriberNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setPrescriberNpi(Long prescriberNpi) {
		this.prescriberNpi = prescriberNpi;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getServiceProviderNpi() {
		return serviceProviderNpi;
	}

	/**
	 * @param serviceProviderNpi
	 *            the new value for {@link #getServiceProviderNpi()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setServiceProviderNpi(Long serviceProviderNpi) {
		this.serviceProviderNpi = serviceProviderNpi;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getProductNdc() {
		return productNdc;
	}

	/**
	 * @param productNdc
	 *            the new value for {@link #getProductNdc()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setProductNdc(Long productNdc) {
		this.productNdc = productNdc;
		return this;
	}

	/**
	 * @return TODO
	 */
	public CurrentBeneficiary getBeneficiary() {
		return beneficiary;
	}

	/**
	 * @param beneficiary
	 *            the new value for {@link #getBeneficiary()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setBeneficiary(CurrentBeneficiary beneficiary) {
		this.beneficiary = beneficiary;
		return this;
	}

	/**
	 * @return TODO
	 */
	public LocalDate getServiceDate() {
		return serviceDate;
	}

	/**
	 * @param serviceDate
	 *            the new value for {@link #getServiceDate()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setServiceDate(LocalDate serviceDate) {
		this.serviceDate = serviceDate;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getQuantityDispensed() {
		return quantityDispensed;
	}

	/**
	 * @param quantityDispensed
	 *            the new value for {@link #getQuantityDispensed()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setQuantityDispensed(Long quantityDispensed) {
		this.quantityDispensed = quantityDispensed;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Long getNumberDaysSupply() {
		return numberDaysSupply;
	}

	/**
	 * @param numberDaysSupply
	 *            the new value for {@link #getNumberDaysSupply()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setNumberDaysSupply(Long numberDaysSupply) {
		this.numberDaysSupply = numberDaysSupply;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getPatientPayAmount() {
		return patientPayAmount;
	}

	/**
	 * @param patientPayAmount
	 *            the new value for {@link #getPatientPayAmount()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setPatientPayAmount(Double patientPayAmount) {
		this.patientPayAmount = patientPayAmount;
		return this;
	}

	/**
	 * @return TODO
	 */
	public Double getTotalPrescriptionCost() {
		return totalPrescriptionCost;
	}

	/**
	 * @param totalPrescriptionCost
	 *            the new value for {@link #getTotalPrescriptionCost()}
	 * @return this instance (for call-chaining purposes)
	 */
	public PartDEventFact setTotalPrescriptionCost(Double totalPrescriptionCost) {
		this.totalPrescriptionCost = totalPrescriptionCost;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartDEventFact [id=");
		builder.append(id);
		builder.append(", prescriberNpi=");
		builder.append(prescriberNpi);
		builder.append(", serviceProviderNpi=");
		builder.append(serviceProviderNpi);
		builder.append(", productNdc=");
		builder.append(productNdc);
		builder.append(", beneficiary=");
		builder.append(beneficiary != null ? beneficiary.getId() : "null");
		builder.append(", serviceDate=");
		builder.append(serviceDate);
		builder.append(", quantityDispensed=");
		builder.append(quantityDispensed);
		builder.append(", numberDaysSupply=");
		builder.append(numberDaysSupply);
		builder.append(", patientPayAmount=");
		builder.append(patientPayAmount);
		builder.append(", totalPrescriptionCost=");
		builder.append(totalPrescriptionCost);
		builder.append("]");
		return builder.toString();
	}
}
