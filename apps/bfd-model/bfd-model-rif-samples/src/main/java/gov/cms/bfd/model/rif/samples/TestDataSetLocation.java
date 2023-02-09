package gov.cms.bfd.model.rif.samples;

/** Enumerates the locations of various test data sets in S3. */
public enum TestDataSetLocation {
  /** Synthetic Data S3. */
  SYNTHETIC_DATA("bfd-public-test-data", "data-synthetic/2020-04-10-part-d-enrollment"),
  /** Dummy Data 1000000 Benes in S3. */
  DUMMY_DATA_1000000_BENES("data-random/1000000-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 100000 Benes in S3. */
  DUMMY_DATA_100000_BENES("data-random/100000-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 10000 Benes in S3. */
  DUMMY_DATA_10000_BENES("data-random/10000-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 1000 Benes in S3. */
  DUMMY_DATA_1000_BENES("data-random/1000-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 100 Benes in S3. */
  DUMMY_DATA_100_BENES("data-random/100-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 10 Benes in S3. */
  DUMMY_DATA_10_BENES("data-random/10-beneficiaries-2017-10-21T00:00:00.000Z"),
  /** Dummy Data 1 Benes in S3. */
  DUMMY_DATA_1_BENES("data-random/1-beneficiaries-2017-10-21T00:00:00.000Z");

  /** The {@link TestDataSetLocation} used by all of the "SAMPLE_B" {@link StaticRifResource}s. */
  static final TestDataSetLocation SAMPLE_B_LOCATION = TestDataSetLocation.DUMMY_DATA_100_BENES;

  /** The {@link TestDataSetLocation} used by all of the "SAMPLE_C" {@link StaticRifResource}s. */
  static final TestDataSetLocation SAMPLE_C_LOCATION = TestDataSetLocation.DUMMY_DATA_1000000_BENES;

  /** The S3 bucket that the project's ETL test data is stored in. */
  public static final String S3_BUCKET_TEST_DATA = "gov-hhs-cms-bluebutton-sandbox-etl-test-data";

  /** The S3 bucket with synthetic data. */
  public static final String BFD_BUCKET_TEST_DATA = "bfd-public-test-data";
  /** S3 Bucket Name. */
  private final String s3BucketName;
  /** S3 Key Prefix. */
  private final String s3KeyPrefix;

  /**
   * Enum constant constructor.
   *
   * @param s3BucketName the value to use for {@link #getS3BucketName()}
   * @param s3KeyPrefix the value to use for {@link #getS3KeyPrefix()}
   */
  private TestDataSetLocation(String s3BucketName, String s3KeyPrefix) {
    this.s3BucketName = s3BucketName;
    this.s3KeyPrefix = s3KeyPrefix;
  }

  /**
   * Enum constant constructor. Assumes {@value #S3_BUCKET_TEST_DATA} for {@link
   * #getS3BucketName()}.
   *
   * @param s3KeyPrefix the value to use for {@link #getS3KeyPrefix()}
   */
  private TestDataSetLocation(String s3KeyPrefix) {
    this(S3_BUCKET_TEST_DATA, s3KeyPrefix);
  }

  /**
   * Get S3 bucket name as a {@link String}.
   *
   * @return the name of the S3 bucket that the test data is stored in
   */
  public String getS3BucketName() {
    return s3BucketName;
  }

  /**
   * Get S3 Key Prefix as a {@link String}.
   *
   * @return the key prefix of all the (publicly accessible) S3 objects containing the test data
   */
  public String getS3KeyPrefix() {
    return s3KeyPrefix;
  }
}
