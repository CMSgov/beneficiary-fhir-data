package gov.hhs.cms.bluebutton.datapipeline.benchmarks;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Enumerates the expected record counts by {@link RifFileType}.
 */
public enum ExpectedRecordCount {
	BENEFICIARY(RifFileType.BENEFICIARY, 29000000),

	CARRIER(RifFileType.CARRIER, 29000000 * 40),

	DME(RifFileType.DME, 0),

	HHA(RifFileType.HHA, 0),

	HOSPICE(RifFileType.HOSPICE, 0),

	INPATIENT(RifFileType.INPATIENT, 0),

	OUTPATIENT(RifFileType.OUTPATIENT, 0),

	PDE(RifFileType.PDE, 0),

	SNF(RifFileType.SNF, 0);

	private final RifFileType recordType;
	private final long initialLoad;

	/**
	 * Enum constant constructor.
	 * 
	 * @param recordType
	 *            the value to use for {@link #getRecordType()}
	 * @param initialLoad
	 *            the value to use for {@link #getInitialLoad()}
	 */
	private ExpectedRecordCount(RifFileType recordType, long initialLoad) {
		this.recordType = recordType;
		this.initialLoad = initialLoad;
	}

	/**
	 * @return the {@link RifFileType} of the records
	 */
	public RifFileType getRecordType() {
		return recordType;
	}

	/**
	 * @return the number of records
	 */
	public long getInitialLoad() {
		return initialLoad;
	}
}
