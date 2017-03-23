package gov.hhs.cms.bluebutton.datapipeline.rif.extract;

import java.io.Serializable;
import java.util.function.Predicate;

import com.amazonaws.regions.Region;

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.S3Utilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Models the user-configurable options for extraction of RIF data from S3.
 */
public final class ExtractionOptions implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String s3BucketName;
	private final RifFileType allowedRifFileType;

	/**
	 * Constructs a new {@link ExtractionOptions} instance.
	 * 
	 * @param s3BucketName
	 *            the value to use for {@link #getS3BucketName()}
	 * @param allowedRifFileType
	 *            the value to use for {@link #getDataSetFilter()}
	 */
	public ExtractionOptions(String s3BucketName, RifFileType allowedRifFileType) {
		this.s3BucketName = s3BucketName;
		this.allowedRifFileType = allowedRifFileType;
	}

	/**
	 * Constructs a new {@link ExtractionOptions} instance, with a
	 * {@link #getDataSetFilter()} that doesn't skip anything.
	 * 
	 * @param s3BucketName
	 *            the value to use for {@link #getS3BucketName()}
	 */
	public ExtractionOptions(String s3BucketName) {
		this(s3BucketName, null);
	}

	/**
	 * @return the AWS {@link Region} that should be used when interacting with
	 *         S3
	 */
	public Region getS3Region() {
		/*
		 * NOTE: This is hardcoded for now, unless/until we have a need to
		 * support other regions. If that happens, just make the region a field
		 * and add a new constructor param here for it.
		 */

		return S3Utilities.REGION_DEFAULT;
	}

	/**
	 * @return the name of the AWS S3 bucket to monitor
	 */
	public String getS3BucketName() {
		return s3BucketName;
	}

	/**
	 * @return the single {@link RifFileType} that the application should
	 *         process, or <code>null</code> if it should process all
	 *         {@link RifFileType}s (when set, any data sets that do not
	 *         <strong>only</strong> contain the specified {@link RifFileType}
	 *         will be skipped by the application)
	 */
	public RifFileType getAllowedRifFileType() {
		return allowedRifFileType;
	}

	/**
	 * @return a {@link Predicate} that returns <code>true</code> for
	 *         {@link Predicate#test(Object)} if the specified
	 *         {@link DataSetManifest} matches the
	 *         {@link #getAllowedRifFileType()} value, and <code>false</code> if
	 *         it does not (and thus should be skipped)
	 */
	public Predicate<DataSetManifest> getDataSetFilter() {
		if (allowedRifFileType != null)
			return d -> d.getEntries().stream().map(e -> e.getType()).allMatch(t -> allowedRifFileType == t);
		else
			return e -> true;
	}
}
