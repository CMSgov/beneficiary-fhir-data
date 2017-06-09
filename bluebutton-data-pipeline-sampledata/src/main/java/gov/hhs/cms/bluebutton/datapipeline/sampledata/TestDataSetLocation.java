package gov.hhs.cms.bluebutton.datapipeline.sampledata;

/**
 * Enumerates the locations of various test data sets in S3.
 */
public enum TestDataSetLocation {
	DUMMY_DATA_1000000_BENES("data-random/1000000-beneficiaries-2017-04-18T05:03:30Z"),

	DUMMY_DATA_100000_BENES("data-random/100000-beneficiaries-2017-06-08T04:42:41.601Z"),

	DUMMY_DATA_10000_BENES("data-random/10000-beneficiaries-2017-06-08T17:48:51.293Z"),

	DUMMY_DATA_1000_BENES("data-random/1000-beneficiaries-2017-06-08T19:02:02.652Z"),

	DUMMY_DATA_100_BENES("data-random/100-beneficiaries-2017-06-08T19:14:30.246Z"),

	DUMMY_DATA_10_BENES("data-random/10-beneficiaries-2017-06-08T19:15:28.988Z"),

	DUMMY_DATA_1_BENES("data-random/1-beneficiaries-2017-06-08T19:16:50.658Z");

	/**
	 * The {@link TestDataSetLocation} used by all of the "SAMPLE_B"
	 * {@link StaticRifResource}s.
	 */
	static final TestDataSetLocation SAMPLE_B_LOCATION = TestDataSetLocation.DUMMY_DATA_100_BENES;

	/**
	 * The {@link TestDataSetLocation} used by all of the "SAMPLE_C"
	 * {@link StaticRifResource}s.
	 */
	static final TestDataSetLocation SAMPLE_C_LOCATION = TestDataSetLocation.DUMMY_DATA_1000000_BENES;

	/**
	 * The S3 bucket that the project's ETL test data is stored in.
	 */
	private static final String S3_BUCKET_TEST_DATA = "gov-hhs-cms-bluebutton-sandbox-etl-test-data";

	private final String s3BucketName;
	private final String s3KeyPrefix;

	/**
	 * Enum constant constructor.
	 * 
	 * @param s3BucketName
	 *            the value to use for {@link #getS3BucketName()}
	 * @param s3KeyPrefix
	 *            the value to use for {@link #getS3KeyPrefix()}
	 */
	private TestDataSetLocation(String s3BucketName, String s3KeyPrefix) {
		this.s3BucketName = s3BucketName;
		this.s3KeyPrefix = s3KeyPrefix;
	}

	/**
	 * Enum constant constructor. Assumes {@value #S3_BUCKET_TEST_DATA} for
	 * {@link #getS3BucketName()}.
	 * 
	 * @param s3KeyPrefix
	 *            the value to use for {@link #getS3KeyPrefix()}
	 */
	private TestDataSetLocation(String s3KeyPrefix) {
		this(S3_BUCKET_TEST_DATA, s3KeyPrefix);
	}

	/**
	 * @return the name of the S3 bucket that the test data is stored in
	 */
	public String getS3BucketName() {
		return s3BucketName;
	}

	/**
	 * @return the key prefix of all the (publicly accessible) S3 objects
	 *         containing the test data
	 */
	public String getS3KeyPrefix() {
		return s3KeyPrefix;
	}
}
