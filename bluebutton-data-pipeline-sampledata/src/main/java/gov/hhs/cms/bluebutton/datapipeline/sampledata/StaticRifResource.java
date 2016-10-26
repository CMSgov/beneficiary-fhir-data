package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.net.URL;
import java.util.function.Supplier;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Enumerates the sample RIF resources available on the classpath.
 */
public enum StaticRifResource {
	/**
	 * This file was manually created by copying a single beneficiary from
	 * {@link #SAMPLE_B_BENES}.
	 */
	SAMPLE_A_BENES(resourceUrl("rif-static-samples/sample-a-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_CARRIER}, and adjusting its beneficiary
	 * to match {@link #SAMPLE_A_BENES}.
	 */
	SAMPLE_A_CARRIER(resourceUrl("rif-static-samples/sample-a-bcarrier.txt"), RifFileType.CARRIER, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_INPATIENT}, and adjusting its
	 * beneficiary to match {@link #SAMPLE_A_INPATIENT}.
	 */
	SAMPLE_A_INPATIENT(resourceUrl("rif-static-samples/sample-a-inpatient.txt"), RifFileType.INPATIENT, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_OUTPATIENT}, and adjusting its
	 * beneficiary to match {@link #SAMPLE_A_OUTPATIENT}.
	 */
	SAMPLE_A_OUTPATIENT(resourceUrl("rif-static-samples/sample-a-outpatient.txt"), RifFileType.OUTPATIENT, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_SNF}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_SNF}.
	 */
	SAMPLE_A_SNF(resourceUrl("rif-static-samples/sample-a-snf.txt"), RifFileType.SNF, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_HOSPICE}, and adjusting its beneficiary
	 * to match {@link #SAMPLE_A_HOSPICE}.
	 */
	SAMPLE_A_HOSPICE(resourceUrl("rif-static-samples/sample-a-hospice.txt"), RifFileType.HOSPICE, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_A_HHA}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_HHA}.
	 */
	SAMPLE_A_HHA(resourceUrl("rif-static-samples/sample-a-hha.txt"), RifFileType.HHA, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_B_DME}, and adjusting its beneficiary to
	 * match {@link #SAMPLE_A_BENES}.
	 */
	SAMPLE_A_DME(resourceUrl("rif-static-samples/sample-a-dme.txt"), RifFileType.DME, 1),

	/**
	 * This file was manually created by copying a single claim from
	 * {@link StaticRifResource#SAMPLE_B_CARRIER}, adjusting its beneficiary to
	 * match {@link #SAMPLE_A_BENES}, and editing some of the values to be
	 * better suited for testing against.
	 */
	SAMPLE_A_PDE(resourceUrl("rif-static-samples/sample-a-pde.txt"), RifFileType.PDE, 1),

	SAMPLE_B_BENES(resourceUrl("rif-static-samples/sample-b-beneficiaries.txt"), RifFileType.BENEFICIARY, 1000),

	/**
	 * The record count here was verified with the following shell command:
	 * <code>$ awk -F '|' '{print $4}' bluebutton-data-pipeline-sampledata/src/main/resources/rif-static-samples/sample-b-bcarrier-1440.txt | tail -n +2 | sort | uniq -c | wc -l</code>
	 * .
	 */
	SAMPLE_B_CARRIER(resourceUrl("rif-static-samples/sample-b-bcarrier.txt"), RifFileType.CARRIER, 1477),

	SAMPLE_B_INPATIENT(resourceUrl("rif-static-samples/sample-b-inpatient.txt"), RifFileType.INPATIENT, 27),

	SAMPLE_B_OUTPATIENT(resourceUrl("rif-static-samples/sample-b-outpatient.txt"), RifFileType.OUTPATIENT, 340),

	SAMPLE_B_SNF(resourceUrl("rif-static-samples/sample-b-snf.txt"), RifFileType.SNF, 78),

	SAMPLE_B_HOSPICE(resourceUrl("rif-static-samples/sample-b-hospice.txt"), RifFileType.HOSPICE, 9),

	SAMPLE_B_HHA(resourceUrl("rif-static-samples/sample-b-hha.txt"), RifFileType.HHA, 22),

	SAMPLE_B_DME(resourceUrl("rif-static-samples/sample-b-dme.txt"), RifFileType.DME, 82),

	SAMPLE_B_PDE(resourceUrl("rif-static-samples/sample-b-pde.txt"), RifFileType.PDE, 1195);

	private final Supplier<URL> resourceUrlSupplier;
	private final RifFileType rifFileType;
	private final int recordCount;

	private URL resourceUrl;

	/**
	 * Enum constant constructor.
	 * 
	 * @param resourceUrlSupplier
	 *            the value to use for {@link #getResourceSupplier()}
	 * @param rifFileType
	 *            the value to use for {@link #getRifFileType()}
	 * @param recordCount
	 *            the value to use for {@link #getRecordCount()}
	 */
	private StaticRifResource(Supplier<URL> resourceUrlSupplier, RifFileType rifFileType, int recordCount) {
		this.resourceUrlSupplier = resourceUrlSupplier;
		this.rifFileType = rifFileType;
		this.recordCount = recordCount;
	}

	/**
	 * @return the {@link URL} to the resource's contents
	 */
	public synchronized URL getResourceUrl() {
		if (resourceUrl == null)
			resourceUrl = resourceUrlSupplier.get();

		return resourceUrl;
	}

	/**
	 * @return the {@link RifFileType} of the RIF file
	 */
	public RifFileType getRifFileType() {
		return rifFileType;
	}

	/**
	 * @return the number of beneficiaries/claims/drug events in the RIF file
	 */
	public int getRecordCount() {
		return recordCount;
	}

	/**
	 * @return a {@link RifFile} based on this {@link StaticRifResource}
	 */
	public RifFile toRifFile() {
		return new StaticRifFile(this);
	}

	/**
	 * @param resourceName
	 *            the name of the resource on the classpath (as might be passed
	 *            to {@link ClassLoader#getResource(String)})
	 * @return a {@link Supplier} for the {@link URL} to the resource's contents
	 */
	private static Supplier<URL> resourceUrl(String resourceName) {
		return () -> Thread.currentThread().getContextClassLoader().getResource(resourceName);
	}
}
