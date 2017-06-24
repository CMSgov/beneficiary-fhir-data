package gov.hhs.cms.bluebutton.data.model.rif;

/**
 * Enumerates the various types of RIF files.
 */
public enum RifFileType {
	BENEFICIARY(BeneficiaryColumn.BENE_ID),

	CARRIER(CarrierClaimColumn.CLM_ID),

	DME(DMEClaimColumn.CLM_ID),

	HHA(HHAClaimColumn.CLM_ID),

	HOSPICE(HospiceClaimColumn.CLM_ID),

	INPATIENT(InpatientClaimColumn.CLM_ID),

	OUTPATIENT(OutpatientClaimColumn.CLM_ID),

	PDE(PartDEventColumn.PDE_ID),

	SNF(SNFClaimColumn.CLM_ID);

	private final Enum<?> idColumn;

	/**
	 * Enum constant constructor.
	 * 
	 * @param idColumn
	 *            the value to use for {@link #getIdColumn()}
	 */
	private RifFileType(Enum<?> idColumn) {
		this.idColumn = idColumn;
	}

	/**
	 * @return the <code>Column</code> enum constant for this
	 *         {@link RifFileType} ID/grouping column, e.g.
	 *         {@link BeneficiaryColumn#BENE_ID}
	 */
	public Enum<?> getIdColumn() {
		return idColumn;
	}
}
