package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.internal.DefaultS3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CopyRequest;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

/**
 * Data access object that encapsulates the AWS S3 API. All primitive S3 related operations in BFD
 * are implemented through calls to an instance of this class.
 */
@Slf4j
@AllArgsConstructor
public class S3Dao implements AutoCloseable {
  /** The test bucket prefix for AWS. */
  private static final String BUCKET_NAME_PREFIX = "bb-test";

  /** HTTP status code from S3 API HEAD request that indicates the request was successful. */
  public static final int HTTP_STATUS_OK = 200;

  /** The client for interacting with AWS S3 buckets and files. */
  private final S3Client s3Client;
  /** The client for interacting with AWS S3 buckets and files. */
  private final S3AsyncClient s3AsyncClient;
  /** Used to perform high throughput downloads. */
  private final S3TransferManager s3TransferManager;

  /**
   * Initializes an instance using the provided factory.
   *
   * @param s3ClientFactory used to create necessary S3 related objects
   */
  public S3Dao(S3ClientFactory s3ClientFactory) {
    s3Client = s3ClientFactory.createS3Client();
    s3AsyncClient = s3ClientFactory.createS3AsyncClient();
    s3TransferManager = DefaultS3TransferManager.builder().s3Client(s3AsyncClient).build();
  }

  /**
   * Closes the {@link #s3TransferManager}.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void close() {
    s3TransferManager.close();
  }

  /**
   * This downloads the entire file into memory and then returns an {@link InputStream} for reading
   * the bytes. Do not use this for large files.
   *
   * @param s3Bucket the bucket containing the objects
   * @param s3Key the S3 object key
   * @return an {@link InputStream} suitable for processing the bytes
   */
  public InputStream readObject(String s3Bucket, String s3Key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
  }

  /**
   * Sends a HEAD request for the specified object and returns true if the result is a 200.
   *
   * @param s3Bucket the bucket containing the objects
   * @param s3Key the S3 object key
   * @return true if the object exists in S3
   */
  public boolean objectExists(String s3Bucket, String s3Key) {
    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    return s3Client.headObject(headObjectRequest).sdkHttpResponse().statusCode() == HTTP_STATUS_OK;
  }

  /**
   * Gets the display name of the owner reported by {@link S3Client#listBuckets()}.
   *
   * @return the display name
   */
  public String readListBucketsOwner() {
    return s3Client.listBuckets().owner().displayName();
  }

  /**
   * Uploads an object with the given key and bucket. The byte array contains the binary data for
   * the uploaded object.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @param objectBytes binary data contents of the object
   * @return response from the S3 API
   */
  public PutObjectResponse putObject(String s3Bucket, String s3Key, byte[] objectBytes) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(s3Bucket)
            .key(s3Key)
            .contentLength((long) objectBytes.length)
            .build();

    return s3Client.putObject(putObjectRequest, RequestBody.fromBytes(objectBytes));
  }

  /**
   * Uploads an object with the given key and bucket. The URL contains the binary data for the
   * uploaded object.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @param objectContentsUrl URL form which binary data contents of the object can be obtained
   * @param metaData key value pairs serving as meta data for the uploaded object
   * @return response from the S3 API
   */
  public PutObjectResponse putObject(
      String s3Bucket, String s3Key, URL objectContentsUrl, Map<String, String> metaData) {
    try {
      final long objectContentLength = objectContentsUrl.openConnection().getContentLength();

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(s3Bucket)
              .key(s3Key)
              .contentLength(objectContentLength)
              .metadata(metaData)
              .build();

      try (InputStream objectStream = objectContentsUrl.openStream()) {
        return s3Client.putObject(
            putObjectRequest, RequestBody.fromInputStream(objectStream, objectContentLength));
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param settings {@link ListObjectsSettings} defining how to request the objects
   * @return a {@link Stream} of objects
   */
  public Stream<S3Object> listObjectsAsStream(ListObjectsSettings settings) {
    ListObjectsV2Request.Builder objectRequestBuilder =
        ListObjectsV2Request.builder().bucket(settings.bucket);
    if (settings.prefix.length() > 0) {
      objectRequestBuilder.prefix(settings.prefix);
    }
    if (settings.pageSize > 0) {
      objectRequestBuilder.maxKeys(settings.pageSize);
    }
    ListObjectsV2Request objectRequest = objectRequestBuilder.build();
    ListObjectsV2Iterable objectPaginator = s3Client.listObjectsV2Paginator(objectRequest);
    return objectPaginator.stream().flatMap(s -> s.contents().stream());
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param s3Bucket the bucket containing the objects
   * @param keyPrefix a prefix string that all objects must match (usually a directory path)
   * @return a {@link Stream} of objects
   */
  public Stream<S3Object> listObjectsAsStream(String s3Bucket, String keyPrefix) {
    return listObjectsAsStream(
        ListObjectsSettings.builder().bucket(s3Bucket).prefix(keyPrefix).build());
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param s3Bucket the bucket containing the objects
   * @return a {@link Stream} of objects
   */
  public Stream<S3Object> listObjectsAsStream(String s3Bucket) {
    return listObjectsAsStream(ListObjectsSettings.builder().bucket(s3Bucket).build());
  }

  /**
   * Read {@link HeadObjectResponse} metadata for the given S3 key. Recognize the possible case of
   * object not found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @return the meta data
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
   */
  public HeadObjectResponse readObjectMetaData(String s3Bucket, String s3Key) {
    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    return s3Client.headObject(headObjectRequest);
  }

  /**
   * Download S3 object and return its {@link GetObjectResponse}. Recognize the possible case of
   * object not found (HTTP 404) by throwing more useful {@link FileNotFoundException}.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @param tempDataFile where to store the downloaded object
   * @return the meta data
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
   */
  public GetObjectResponse downloadObject(String s3Bucket, String s3Key, Path tempDataFile) {
    try {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
      DownloadFileRequest downloadFileRequest =
          DownloadFileRequest.builder()
              .getObjectRequest(getObjectRequest)
              .destination(tempDataFile)
              .addTransferListener(LoggingTransferListener.create())
              .build();

      FileDownload downloadFile = s3TransferManager.downloadFile(downloadFileRequest);
      return downloadFile.completionFuture().join().response();
    } catch (CompletionException e) {
      final var cause = extractCompletionExceptionCause(e);
      try {
        // delete the file if it partially exists, we don't care about the boolean result
        tempDataFile.toFile().delete();
      } catch (Exception ex) {
        cause.addSuppressed(ex);
      }
      throw cause;
    }
  }

  /**
   * Copies the object from the given source bucket and key to an object at the provided target
   * bucket and key.
   *
   * @param s3SourceBucket the bucket containing source object
   * @param s3SourceKey the S3 object key of source object
   * @param s3TargetBucket the bucket to contain the target object
   * @param s3TargetKey the S3 object key of the target object
   */
  public void copyObject(
      String s3SourceBucket, String s3SourceKey, String s3TargetBucket, String s3TargetKey) {
    CopyObjectRequest copyObjectRequestRequest =
        CopyObjectRequest.builder()
            .sourceBucket(s3SourceBucket)
            .sourceKey(s3SourceKey)
            .destinationBucket(s3TargetBucket)
            .destinationKey(s3TargetKey)
            .build();
    CopyRequest copyRequest =
        CopyRequest.builder().copyObjectRequest(copyObjectRequestRequest).build();
    try {
      s3TransferManager.copy(copyRequest).completionFuture().join();
    } catch (CompletionException e) {
      throw extractCompletionExceptionCause(e);
    }
  }

  /**
   * Deletes an S3 object given its bucket and key.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   */
  public void deleteObject(String s3Bucket, String s3Key) {
    DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    s3Client.deleteObject(request);

    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    try (S3Waiter waiter = s3Client.waiter()) {
      waiter.waitUntilObjectNotExists(headObjectRequest);
    }
  }

  /**
   * Creates a new test bucket with a name based on a fixed prefix and a random number. Only works
   * when communicating with localstack or minio or other simulated S3 service using an endpoint
   * override.
   *
   * @return the bucket name of a new, random {@link Bucket} for use in an integration test
   * @throws BadCodeMonkeyException if we are communicating with real AWS service
   */
  public String createTestBucket() {
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

    HeadBucketRequest bucketHeadRequest = HeadBucketRequest.builder().bucket(bucketName).build();
    try (S3Waiter waiter = s3Client.waiter()) {
      waiter.waitUntilBucketExists(bucketHeadRequest);
    }
    return bucketName;
  }

  /**
   * Deletes a bucket created by {@link #createTestBucket} along with all of its contents.
   *
   * @param s3Bucket the bucket containing the object
   */
  public void deleteTestBucket(String s3Bucket) {
    if (Strings.isNullOrEmpty(s3Bucket)) {
      return;
    }

    if (!s3Bucket.startsWith(BUCKET_NAME_PREFIX)) {
      throw new IllegalArgumentException("only buckets created by this class can be deleted");
    }

    // delete the bucket contents
    listObjectsAsStream(s3Bucket).forEach(s3Object -> deleteObject(s3Bucket, s3Object.key()));

    // delete the bucket itself
    DeleteBucketRequest deleteBucketRequest =
        DeleteBucketRequest.builder().bucket(s3Bucket).build();
    s3Client.deleteBucket(deleteBucketRequest);

    HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(s3Bucket).build();
    try (S3Waiter waiter = s3Client.waiter()) {
      waiter.waitUntilBucketNotExists(headBucketRequest);
    }
  }

  /**
   * A {@link CompletionException} is just an implementation detail and irrelevant to callers of our
   * methods. This obtains the actual cause and returns a {@link RuntimeException} which is either
   * the actual cause or a wrapper around the cause.
   *
   * @param e exception to unwrap
   * @return the {@link RuntimeException}
   */
  private RuntimeException extractCompletionExceptionCause(CompletionException e) {
    final var cause = e.getCause();
    if (cause instanceof RuntimeException runtimeException) {
      return runtimeException;
    } else {
      return new RuntimeException(cause);
    }
  }

  /** Data object encapsulating all possible settings for listing objects from S3. */
  @Data
  public static class ListObjectsSettings {
    /** Bucket name to list from. */
    private final String bucket;
    /** Optional prefix for keys within the bucket. */
    private final String prefix;
    /** Optional page size. */
    private final int pageSize;

    /**
     * Constructs an instance.
     *
     * @param bucket required bucket name
     * @param prefix optional key prefix (null or empty uses no prefix)
     * @param pageSize optional page size (null, zero or negative uses S3 default value)
     */
    @Builder
    public ListObjectsSettings(String bucket, @Nullable String prefix, @Nullable Integer pageSize) {
      this.bucket = Preconditions.checkNotNull(bucket);
      this.prefix = Strings.nullToEmpty(prefix);
      this.pageSize = pageSize == null ? 0 : pageSize;
    }
  }
}
