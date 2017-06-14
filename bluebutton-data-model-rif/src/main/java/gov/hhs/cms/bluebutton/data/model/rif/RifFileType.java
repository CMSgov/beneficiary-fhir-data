package gov.hhs.cms.bluebutton.data.model.rif;

/**
 * Enumerates the various types of RIF files.
 */
public enum RifFileType {
	BENEFICIARY(BeneficiaryColumn.BENE_ID),

	CARRIER(CarrierClaimColumn.CLM_ID),

	DME(DMEClaimGroup.Column.CLM_ID),

	HHA(HHAClaimGroup.Column.CLM_ID),

	HOSPICE(HospiceClaimGroup.Column.CLM_ID),

	INPATIENT(InpatientClaimGroup.Column.CLM_ID),

	OUTPATIENT(OutpatientClaimGroup.Column.CLM_ID),

	PDE(PartDEventRow.Column.PDE_ID),

	SNF(SNFClaimGroup.Column.CLM_ID);

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
