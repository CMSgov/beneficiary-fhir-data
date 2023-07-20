package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.AwsClient;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
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
   * Creates a S3Client that connects to either a local Minio or real Amazon S3 based on the
   * MinioConfig singleton's useMinio value.
   *
   * @param awsS3Region the AWS {@link Region} that should be used when interacting with S3
   * @return the {@link S3Client} client to use
   */
  public static S3Client createS3Client(Region awsS3Region) {
    S3MinioConfig minioConfig = S3MinioConfig.Singleton();

    if (minioConfig.useMinio) {
      return createS3MinioClient(awsS3Region, minioConfig, S3Client.class);
    }
    S3Client s3Client =
        S3Client.builder().defaultsMode(DefaultsMode.STANDARD).region(awsS3Region).build();
    return s3Client;
  }

  /**
   * Creates an S3AsyncClient that connects to either a local Minio or real Amazon S3 based on the
   * MinioConfig singleton's useMinio value.
   *
   * @param awsS3Region the AWS {@link Region} that should be used when interacting with S3
   * @return the {@link S3Client} client to use
   */
  public static S3AsyncClient createS3AsyncClient(Region awsS3Region) {
    S3MinioConfig minioConfig = S3MinioConfig.Singleton();

    if (minioConfig.useMinio) {
      return createS3AsyncMinioClient(awsS3Region, minioConfig);
    }
    // Split the file download (automatically using the async client) if its >200 MB
    long mb = 1048576L;
    return S3AsyncClient.crtBuilder().minimumPartSizeInBytes(200 * mb).region(awsS3Region).build();
  }

  /**
   * Creates and returns a new s3 client via minio.
   *
   * @param awsS3Region the AWS {@link Region} that should be used when interacting with S3
   * @param minioConfig passes the minioConfig to use
   * @param <T> the return type of the {@link AwsClient}
   * @param type the class type of the {@link AwsClient}, either {@link S3Client} or {@link
   *     S3AsyncClient}
   * @return the {@link S3Client} minio client to use
   */
  public static <T extends AwsClient> T createS3MinioClient(
      Region awsS3Region, S3MinioConfig minioConfig, Class<T> type) {
    // Uses BasicCredentials to connect to the minio client and gets the
    // username,password, and
    // address from the minioconfig
    AwsCredentials credentials =
        AwsBasicCredentials.create(minioConfig.minioUserName, minioConfig.minioPassword);

    ClientOverrideConfiguration.Builder overrideConfig =
        ClientOverrideConfiguration.builder()
            .putAdvancedOption(SdkAdvancedClientOption.SIGNER, AwsS3V4Signer.create());
    S3Configuration.Builder s3ConfigBuilder =
        S3Configuration.builder().pathStyleAccessEnabled(true);

    if (S3AsyncClient.class.isAssignableFrom(type)) {
      return type.cast(
          S3AsyncClient.builder()
              .serviceConfiguration(s3ConfigBuilder.build())
              .overrideConfiguration(overrideConfig.build())
              .defaultsMode(DefaultsMode.STANDARD)
              .region(awsS3Region)
              .endpointOverride(URI.create(minioConfig.minioEndpointAddress))
              .credentialsProvider(StaticCredentialsProvider.create(credentials))
              .build());
    }
    return type.cast(
        S3Client.builder()
            .serviceConfiguration(s3ConfigBuilder.build())
            .overrideConfiguration(overrideConfig.build())
            .defaultsMode(DefaultsMode.STANDARD)
            .region(awsS3Region)
            .endpointOverride(URI.create(minioConfig.minioEndpointAddress))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build());
  }

  /**
   * Creates and returns a new async s3 client via minio.
   *
   * @param awsS3Region the AWS {@link Region} that should be used when interacting with S3
   * @param minioConfig passes the minioConfig to use
   * @return the {@link S3Client} minio client to use
   */
  public static S3AsyncClient createS3AsyncMinioClient(
      Region awsS3Region, S3MinioConfig minioConfig) {
    // Uses BasicCredentials to connect to the minio client and gets the
    // username,password, and
    // address from the minioconfig
    return createS3MinioClient(awsS3Region, minioConfig, S3AsyncClient.class);
  }

  /**
   * Creates a new test bucket with a name based on the current user and a random number.
   *
   * @param s3Client the {@link S3Client} client to use
   * @return the bucket name of a new, random {@link Bucket} for use in an integration test
   */
  public static String createTestBucket(S3Client s3Client) {
    String username = System.getProperty("user.name");

    if (Strings.isNullOrEmpty(username)) {
      username = "anonymous";
    } else {
      username = username.toLowerCase().replaceAll("[@\\\\]", "-");
    }
    final int randomId = ThreadLocalRandom.current().nextInt(100000);
    final String bucketName =
        String.format("%s-%s-%d", BUCKET_NAME_PREFIX, username.toLowerCase(), randomId);

    // if not running S3 inside minio (i.e., vs. real AWS S3 buckets), then we need
    // to be observant of CMS security constraints; inside minio, not so much!
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
