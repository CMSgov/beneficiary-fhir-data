package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents an extracted DE-SynPUF sample bundle.
 */
public final class SynpufSample {
	private final Path sampleDir;
	private final SynpufArchive archive;

	/**
	 * Constructs a new {@link SynpufSample}.
	 * 
	 * @param sampleDir
	 *            the directory that the files for this {@link SynpufSample} can
	 *            be found in
	 * @param archive
	 *            the value to use for {@link #getArchive()}
	 */
	public SynpufSample(Path sampleDir, SynpufArchive archive) {
		this.sampleDir = sampleDir;
		this.archive = archive;
	}

	/**
	 * @return the {@link SynpufArchive} that this {@link SynpufSample}
	 *         represents
	 */
	public SynpufArchive getArchive() {
		return archive;
	}

	/**
	 * @return the {@link Path}s to the extracted
	 *         {@link SynpufFile#BENE_SUMMARY_2008},
	 *         {@link SynpufFile#BENE_SUMMARY_2009}, and
	 *         {@link SynpufFile#BENE_SUMMARY_2010} files
	 */
	public Path[] getBeneficiarySummaries() {
		return new Path[] { resolve(SynpufFile.BENE_SUMMARY_2008), resolve(SynpufFile.BENE_SUMMARY_2009),
				resolve(SynpufFile.BENE_SUMMARY_2010) };
	}

	/**
	 * @return the {@link Path} to the extracted
	 *         {@link SynpufFile#CLAIMS_INPATIENT}
	 */
	public Path getInpatientClaimsFile() {
		return resolve(SynpufFile.CLAIMS_INPATIENT);
	}

	/**
	 * @return the {@link Path} to the extracted
	 *         {@link SynpufFile#CLAIMS_OUTPATIENT}
	 */
	public Path getOutpatientClaimsFile() {
		return resolve(SynpufFile.CLAIMS_OUTPATIENT);
	}

	/**
	 * @return the {@link Path}s to the extracted
	 *         {@link SynpufFile#CLAIMS_CARRIER_FIRST} and
	 *         {@link SynpufFile#CLAIMS_CARRIER_SECOND} files
	 */
	public Path[] getCarrierClaimsFiles() {
		return new Path[] { resolve(SynpufFile.CLAIMS_CARRIER_FIRST), resolve(SynpufFile.CLAIMS_CARRIER_SECOND) };
	}

	/**
	 * @return the {@link Path}s to the extracted
	 *         {@link SynpufFile#CLAIMS_PART_D} file
	 */
	public Path getPartDClaimsFile() {
		return resolve(SynpufFile.CLAIMS_PART_D);
	}

	/**
	 * @param file
	 *            the {@link SynpufFile} to resolve the path of
	 * @return the {@link Path} for the specified {@link SynpufFile} in this
	 *         {@link SynpufSample}
	 */
	public Path resolve(SynpufFile file) {
		return sampleDir.resolve(file.getFileName(archive));
	}

	/**
	 * @return <code>true</code> if all files for this {@link SynpufSample}
	 *         exist, <code>false</code> if they do not
	 */
	public boolean allFilesExist() {
		for (SynpufFile file : SynpufFile.values())
			if (!Files.exists(sampleDir.resolve(file.getFileName(archive))))
				return false;

		return true;
	}
}
