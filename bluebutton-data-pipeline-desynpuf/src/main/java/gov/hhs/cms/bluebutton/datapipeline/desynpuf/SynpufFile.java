package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Enumerates the various files contained in each {@link SynpufArchive}.
 */
public enum SynpufFile {
	BENE_SUMMARY_2008("DE1_0_2008_Beneficiary_Summary_File_Sample_{}.csv"),

	BENE_SUMMARY_2009("DE1_0_2009_Beneficiary_Summary_File_Sample_{}.csv"),

	BENE_SUMMARY_2010("DE1_0_2010_Beneficiary_Summary_File_Sample_{}.csv"),

	CLAIMS_INPATIENT("DE1_0_2008_to_2010_Inpatient_Claims_Sample_{}.csv"),

	CLAIMS_OUTPATIENT("DE1_0_2008_to_2010_Outpatient_Claims_Sample_{}.csv"),

	CLAIMS_CARRIER_FIRST("DE1_0_2008_to_2010_Carrier_Claims_Sample_{}A.csv"),

	CLAIMS_CARRIER_SECOND("DE1_0_2008_to_2010_Carrier_Claims_Sample_{}B.csv"),

	CLAIMS_PART_D("DE1_0_2008_to_2010_Prescription_Drug_Events_Sample_{}.csv");

	private final String fileNamePattern;

	/**
	 * Enum constant constructor.
	 * 
	 * @param fileNamePattern
	 *            a pattern for the name of this {@link SynpufFile}'s actual
	 *            file, where "<code>{}</code>" can be substituted for each
	 *            {@link SynpufArchive}'s {@link SynpufArchive#getId()}
	 */
	private SynpufFile(String fileNamePattern) {
		this.fileNamePattern = fileNamePattern;
	}

	/**
	 * @param archive
	 *            the {@link SynpufArchive} to the file name for
	 * @return the actual file name for this {@link SynpufFile}, for the
	 *         specified {@link SynpufArchive}
	 */
	public Path getFileName(SynpufArchive archive) {
		return Paths.get(fileNamePattern.replace("{}", archive.getId()));
	}
}
