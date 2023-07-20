package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;

/** Contains utility/helper methods for AWS S3 that can be used in application and test code. */
public final class SharedS3Utilities {
  /** The default AWS {@link Region} to interact with. */
  public static final Region REGION_DEFAULT = Region.US_EAST_1;
  /** The bucket prefix for AWS. */
  private static final String BUCKET_NAME_PREFIX = "bb-test";

  /**
   * Creates a new test bucket with a name based on the current user and a random number.
   *
   * @param s3Client the {@link S3Client} client to use
   * @return the bucket name of a new, random {@link Bucket} for use in an integration test
   */
  public static String createTestBucket(S3Client s3Client) {
    final int randomId = ThreadLocalRandom.current().nextInt(100000);
    final String bucketName = String.format("%s-%d", BUCKET_NAME_PREFIX, randomId);

    // Ensure that we are not about to create a bucket in the cloud.
    if (s3Client.serviceClientConfiguration().endpointOverride().isEmpty()) {
      throw new BadCodeMonkeyException(
          "s3Client has no endpoint override - it might be a real S3 service client");
    }
    CreateBucketRequest createBucketRequest =
        CreateBucketRequest.builder().bucket(bucketName).build();
    s3Client.createBucket(createBucketRequest);
    S3Waiter s3Waiter = s3Client.waiter();
    HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder().bucket(bucketName).build();
    s3Waiter.waitUntilBucketExists(bucketRequestWait);
    return bucketName;
  }

  /**
   * Deletes a bucket created by {@link #createTestBucket} along with all of its contents.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucketName the name of the bucket to delete along with all of its contents
   */
  public static void deleteTestBucket(S3Client s3Client, String bucketName) {
    if (Strings.isNullOrEmpty(bucketName)) {
      return;
    }
    if (!bucketName.startsWith(BUCKET_NAME_PREFIX)) {
      throw new IllegalArgumentException("only buckets created by this class can be deleted");
    }

    ListObjectsV2Request listObjectsV2Request =
        ListObjectsV2Request.builder().bucket(bucketName).build();
    Consumer<S3Object> deleteObject =
        s3Object -> {
          DeleteObjectRequest request =
              DeleteObjectRequest.builder().bucket(bucketName).key(s3Object.key()).build();
          s3Client.deleteObject(request);
        };
    onListObjectsV2Stream(s3Client, listObjectsV2Request, deleteObject);

    DeleteBucketRequest deleteBucketRequest =
        DeleteBucketRequest.builder().bucket(bucketName).build();
    s3Client.deleteBucket(deleteBucketRequest);
  }

  /**
   * Auto-paginates through the S3Objects returned from the given {@link ListObjectsV2Request} and
   * consumes them via a {@link Consumer}.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param listObjectsV2Request the {@link ListObjectsV2Request} request to retrieve the s3 objects
   *     over which to paginate and consume
   * @param s3ObjectConsumer the {@link Consumer} to consume the s3Objects
   */
  public static void onListObjectsV2Stream(
      S3Client s3Client,
      ListObjectsV2Request listObjectsV2Request,
      Consumer<S3Object> s3ObjectConsumer) {
    ListObjectsV2Iterable listObjectsV2Paginator =
        s3Client.listObjectsV2Paginator(listObjectsV2Request);

    listObjectsV2Paginator.stream()
        .flatMap(s -> s.contents().stream())
        .forEach(k -> s3ObjectConsumer.accept(k));
  }

  /**
   * Uploads a JSON resource to an S3 bucket and waits for it to be fully available.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucketName the name of the bucket to store the object in
   * @param objectKey the key for the object
   * @param bytes a {@link ByteSource} referencing the json text
   * @throws IOException if there is an issue opening the input byte source stream
   */
  public static void uploadJsonToBucket(
      S3Client s3Client, String bucketName, String objectKey, ByteSource bytes) throws IOException {
    try (InputStream input = bytes.openStream()) {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .contentType("application/json")
              .contentLength(bytes.size())
              .bucket(bucketName)
              .key(objectKey)
              .build();
      s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(input, bytes.size()));
    }
    S3Waiter s3Waiter = s3Client.waiter();
    HeadObjectRequest headRequestWait =
        HeadObjectRequest.builder().bucket(bucketName).key(objectKey).build();
    s3Waiter.waitUntilObjectExists(headRequestWait);
  }
}
