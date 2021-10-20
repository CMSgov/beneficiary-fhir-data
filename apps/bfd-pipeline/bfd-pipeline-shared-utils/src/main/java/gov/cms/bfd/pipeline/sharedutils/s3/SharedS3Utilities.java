package gov.cms.bfd.pipeline.sharedutils.s3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.waiters.WaiterParameters;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/** Contains utility/helper methods for AWS S3 that can be used in application and test code. */
public final class SharedS3Utilities {
  /** The default AWS {@link Region} to interact with. */
  public static final Regions REGION_DEFAULT = Regions.US_EAST_1;

  private static final String BUCKET_NAME_PREFIX = "bb-test";

  /**
   * Creates a AmazonS3 that connects to either a local Minio or real Amazon S3 based on the
   * MinioConfig singleton's useMinio value.
   *
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @return the {@link AmazonS3} client to use
   */
  public static AmazonS3 createS3Client(Regions awsS3Region) {
    S3MinioConfig minioConfig = S3MinioConfig.Singleton();

    if (minioConfig.useMinio) {
      return createS3MinioClient(awsS3Region, minioConfig);
    }
    AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(awsS3Region).build();
    return s3Client;
  }

  /**
   * @param awsS3Region the AWS {@link Regions} that should be used when interacting with S3
   * @param minioConfig passes the minioConfig to use
   * @return the {@link AmazonS3} minio client to use
   */
  public static AmazonS3 createS3MinioClient(Regions awsS3Region, S3MinioConfig minioConfig) {
    // Uses BasicCredentials to connect to the minio client and gets the username,password, and
    // address from the minioconfig
    AWSCredentials credentials =
        new BasicAWSCredentials(minioConfig.minioUserName, minioConfig.minioPassword);

    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientConfiguration.setSignerOverride("AWSS3V4SignerType");

    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
                minioConfig.minioEndpointAddress, awsS3Region.name()))
        .withPathStyleAccessEnabled(true)
        .withClientConfiguration(clientConfiguration)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }

  /**
   * Creates a new test bucket with a name based on the current user and time.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @return a new, random {@link Bucket} for use in an integration test
   */
  public static Bucket createTestBucket(AmazonS3 s3Client) {
    String username = System.getProperty("user.name");
    if (Strings.isNullOrEmpty(username)) {
      username = "anonymous";
    } else {
      username = username.replaceAll("[@\\\\]", "-");
    }
    Instant now = Instant.now();
    String bucketName =
        String.format(
            "%s-%s-%d-%09d", BUCKET_NAME_PREFIX, username, now.getEpochSecond(), now.getNano());

    Bucket bucket = s3Client.createBucket(bucketName);
    waitForBucketToExist(s3Client, bucketName);

    return bucket;
  }

  /**
   * Deletes a bucket created by {@link createTestBucket} along with all of its contents.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucket the {@link Bucket} client to delete along with all of its contents
   */
  public static void deleteTestBucket(AmazonS3 s3Client, Bucket bucket) {
    if (bucket == null) {
      return;
    }
    final String bucketName = bucket.getName();
    if (!bucketName.startsWith(BUCKET_NAME_PREFIX)) {
      throw new IllegalArgumentException("only buckets created by this class can be deleted");
    }

    ListObjectsV2Request s3BucketListRequest = new ListObjectsV2Request();
    s3BucketListRequest.setBucketName(bucketName);
    ListObjectsV2Result s3ObjectListing;
    do {
      s3ObjectListing = s3Client.listObjectsV2(s3BucketListRequest);

      for (S3ObjectSummary objectSummary : s3ObjectListing.getObjectSummaries()) {
        s3Client.deleteObject(bucketName, objectSummary.getKey());
        waitForObjectToNotExist(s3Client, bucketName, objectSummary.getKey());
      }

      s3BucketListRequest.setContinuationToken(s3ObjectListing.getNextContinuationToken());
    } while (s3ObjectListing.isTruncated());
    s3Client.deleteBucket(bucketName);
  }

  /**
   * Uploads a JSON resource to an S3 bucket and waits for it to be fully available.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucketName the name of the bucket to store the object in
   * @param objectKey the key for the object
   * @param bytes a {@link ByteSource} referencing the json text
   */
  public static void uploadJsonToBucket(
      AmazonS3 s3Client, String bucketName, String objectKey, ByteSource bytes) throws IOException {
    try (InputStream input = bytes.openStream()) {
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType("application/json");
      metadata.setContentLength(bytes.size());
      s3Client.putObject(bucketName, objectKey, input, metadata);
    }
    waitForObjectToExist(s3Client, bucketName, objectKey);
  }

  /**
   * Note: S3's API is eventually consistent. This method waits for a new object to longer exist.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucketName the name of the bucket to store the object in
   * @param objectKey the key for the object
   */
  public static void waitForObjectToExist(AmazonS3 s3Client, String bucketName, String objectKey) {
    s3Client
        .waiters()
        .objectExists()
        .run(new WaiterParameters<>(new GetObjectMetadataRequest(bucketName, objectKey)));
  }

  /**
   * Note: S3's API is eventually consistent. This method waits for a deleted object to no longer
   * exist.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucketName the name of the bucket to store the object in
   * @param objectKey the key for the object
   */
  public static void waitForObjectToNotExist(
      AmazonS3 s3Client, String bucketName, String objectKey) {
    s3Client
        .waiters()
        .objectNotExists()
        .run(new WaiterParameters<>(new GetObjectMetadataRequest(bucketName, objectKey)));
  }

  /**
   * Note: S3's API is eventually consistent. This method waits for a new bucket to exist.
   *
   * @param s3Client the {@link AmazonS3} client to use
   * @param bucketName the name of the bucket to store the object in
   */
  public static void waitForBucketToExist(AmazonS3 s3Client, String bucketName) {
    s3Client
        .waiters()
        .bucketExists()
        .run(new WaiterParameters<>(new HeadBucketRequest(bucketName)));
  }
}
