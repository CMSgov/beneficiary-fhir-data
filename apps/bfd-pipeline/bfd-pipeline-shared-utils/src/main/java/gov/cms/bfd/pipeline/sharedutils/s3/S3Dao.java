package gov.cms.bfd.pipeline.sharedutils.s3;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.exceptions.UncheckedIOException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
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
   * Closes the {@link #s3TransferManager}.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void close() {
    s3TransferManager.close();
  }

  /**
   * Sends a HEAD request for the specified object and returns true if the result is a 200.
   *
   * @param s3Bucket the bucket containing the objects
   * @param s3Key the S3 object key
   * @return true if the object exists in S3
   */
  public boolean objectExists(String s3Bucket, String s3Key) {
    try {
      HeadObjectRequest headObjectRequest =
          HeadObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
      return s3Client.headObject(headObjectRequest).sdkHttpResponse().statusCode()
          == HTTP_STATUS_OK;
    } catch (NoSuchBucketException | NoSuchKeyException ex) {
      return false;
    }
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
   * @param metaData key value pairs serving as meta data for the uploaded object
   * @return response from the S3 API
   * @throws NoSuchBucketException for bad bucket name
   */
  public S3ObjectSummary putObject(
      String s3Bucket, String s3Key, byte[] objectBytes, Map<String, String> metaData) {
    RequestBody requestBody = RequestBody.fromBytes(objectBytes);
    return putObjectImpl(s3Bucket, s3Key, objectBytes.length, requestBody, metaData);
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
   * @throws NoSuchBucketException for bad bucket name
   * @throws UncheckedIOException for errors reading object data from URL
   */
  public S3ObjectSummary putObject(
      String s3Bucket, String s3Key, URL objectContentsUrl, Map<String, String> metaData) {
    try {
      final long objectSize = objectContentsUrl.openConnection().getContentLength();
      try (InputStream objectStream = objectContentsUrl.openStream()) {
        RequestBody requestBody = RequestBody.fromInputStream(objectStream, objectSize);
        return putObjectImpl(s3Bucket, s3Key, objectSize, requestBody, metaData);
      }
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /**
   * Provides common implementation for the {@link #putObject} methods. Uploads an object with the
   * given bucket, key, size, meta data, and body.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @param objectSize size of object in bytes
   * @param requestBody source of the object's byte data
   * @param metaData key value pairs serving as meta data for the uploaded object
   * @return response from the S3 API
   * @throws NoSuchBucketException for bad bucket name
   */
  private S3ObjectSummary putObjectImpl(
      String s3Bucket,
      String s3Key,
      long objectSize,
      RequestBody requestBody,
      Map<String, String> metaData) {
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(s3Bucket)
            .key(s3Key)
            .contentLength(objectSize)
            .metadata(metaData)
            .build();
    PutObjectResponse putObjectResponse = s3Client.putObject(putObjectRequest, requestBody);
    return new S3ObjectSummary(s3Key, objectSize, Instant.now(), putObjectResponse);
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param s3Bucket the bucket containing the objects
   * @param keyPrefix optional prefix string that all objects must match (usually a directory path)
   * @param pageSize optional max number of objects returned per page (not per stream)
   * @return a {@link Stream} of objects
   * @throws NoSuchBucketException for bad bucket name
   */
  public Stream<S3ObjectSummary> listObjects(
      String s3Bucket, Optional<String> keyPrefix, Optional<Integer> pageSize) {
    ListObjectsV2Request.Builder objectRequestBuilder =
        ListObjectsV2Request.builder().bucket(s3Bucket);
    keyPrefix.ifPresent(objectRequestBuilder::prefix);
    pageSize.ifPresent(objectRequestBuilder::maxKeys);
    ListObjectsV2Request objectRequest = objectRequestBuilder.build();
    ListObjectsV2Iterable objectPaginator = s3Client.listObjectsV2Paginator(objectRequest);
    return objectPaginator.stream().flatMap(s -> s.contents().stream()).map(S3ObjectSummary::new);
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param s3Bucket the bucket containing the objects
   * @param keyPrefix a prefix string that all objects must match (usually a directory path)
   * @return a {@link Stream} of objects
   * @throws NoSuchBucketException for bad bucket name
   */
  public Stream<S3ObjectSummary> listObjects(String s3Bucket, String keyPrefix) {
    return listObjects(s3Bucket, Optional.of(keyPrefix), Optional.empty());
  }

  /**
   * Reads a list of objects and returns a lazy {@link Stream} of all of the objects. Lookups are
   * paginated so not all objects are read at once.
   *
   * @param s3Bucket the bucket containing the objects
   * @return a {@link Stream} of objects
   * @throws NoSuchBucketException for bad bucket name
   */
  public Stream<S3ObjectSummary> listObjects(String s3Bucket) {
    return listObjects(s3Bucket, Optional.empty(), Optional.empty());
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
  public S3ObjectDetails readObjectMetaData(String s3Bucket, String s3Key) {
    HeadObjectRequest headObjectRequest =
        HeadObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    return new S3ObjectDetails(s3Key, s3Client.headObject(headObjectRequest));
  }

  /**
   * This downloads the entire file into memory and then returns an {@link InputStream} for reading
   * the bytes. Do not use this for large files.
   *
   * @param s3Bucket the bucket containing the objects
   * @param s3Key the S3 object key
   * @return an {@link InputStream} suitable for processing the bytes
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
   */
  public InputStream readObject(String s3Bucket, String s3Key) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(s3Bucket).key(s3Key).build();
    return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
  }

  /**
   * Download S3 object and return its {@link GetObjectResponse}. Uses a {@link S3TransferManager}
   * for higher throughput and reliability than {@link #readObject}.
   *
   * @param s3Bucket the bucket containing the object
   * @param s3Key the S3 object key
   * @param dataFile where to store the downloaded object
   * @return the meta data
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
   */
  public S3ObjectDetails downloadObject(String s3Bucket, String s3Key, Path dataFile) {
    try {
      DownloadFileRequest downloadFileRequest =
          DownloadFileRequest.builder()
              .getObjectRequest(requestBuilder -> requestBuilder.bucket(s3Bucket).key(s3Key))
              .destination(dataFile)
              .addTransferListener(LoggingTransferListener.create())
              .build();

      FileDownload downloadFile = s3TransferManager.downloadFile(downloadFileRequest);
      GetObjectResponse getObjectResponse = downloadFile.completionFuture().join().response();
      return new S3ObjectDetails(s3Key, getObjectResponse);
    } catch (CompletionException e) {
      throw extractCompletionExceptionCause(e);
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
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
   */
  public void copyObject(
      String s3SourceBucket, String s3SourceKey, String s3TargetBucket, String s3TargetKey) {
    // Multipart copy can prevent the copying of meta data so we read meta data from source object
    // and add it to the copy request to ensure it is preserved.
    final var metaData = readObjectMetaData(s3SourceBucket, s3SourceKey).getMetaData();

    CopyRequest copyRequest =
        CopyRequest.builder()
            .copyObjectRequest(
                requestBuilder ->
                    requestBuilder
                        .sourceBucket(s3SourceBucket)
                        .sourceKey(s3SourceKey)
                        .destinationBucket(s3TargetBucket)
                        .destinationKey(s3TargetKey)
                        .metadata(metaData))
            .build();
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
   * @throws NoSuchKeyException for bad key
   * @throws NoSuchBucketException for bad bucket name
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
      throw new BadCodeMonkeyException("only buckets created by this class can be deleted");
    }

    // delete the bucket contents
    listObjects(s3Bucket).forEach(s3Object -> deleteObject(s3Bucket, s3Object.getKey()));

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
  @VisibleForTesting
  RuntimeException extractCompletionExceptionCause(CompletionException e) {
    final var cause = e.getCause();
    if (cause instanceof RuntimeException runtimeException) {
      return runtimeException;
    } else if (cause instanceof IOException ioException) {
      return new UncheckedIOException(ioException);
    } else {
      return new RuntimeException(cause);
    }
  }

  /**
   * Data object containing the few fields we use from an {@link S3Object} or {@link
   * PutObjectResponse}.
   *
   * <p>Using this class removes a dependency on underlying API responses and simplifies use of the
   * {@link S3Dao}.
   */
  @Data
  @AllArgsConstructor
  public static class S3ObjectSummary {
    /** The object's key. */
    private final String key;

    /** The object's eTag. */
    private final String eTag;

    /** The object's size. */
    private final long size;

    /** The object's last modified date. */
    private Instant lastModified;

    /**
     * Initializes an instance from a {@link S3Object}.
     *
     * @param object object returned by S3 API
     */
    private S3ObjectSummary(S3Object object) {
      key = object.key();
      eTag = object.eTag();
      size = object.size();
      lastModified = object.lastModified();
    }

    /**
     * Initializes an instance from a {@link PutObjectResponse}.
     *
     * @param key The object's key
     * @param size The object's size
     * @param lastModified the object's last modified date
     * @param response object returned by S3 API
     */
    private S3ObjectSummary(
        String key, long size, Instant lastModified, PutObjectResponse response) {
      this.key = key;
      eTag = response.eTag();
      this.size = size;
      this.lastModified = lastModified;
    }
  }

  /**
   * Data object containing the few fields we use from an {@link HeadObjectResponse} or {@link
   * GetObjectResponse}. Similar to {@link S3ObjectSummary} but also includes an immutable {@link
   * Map} of meta data key/value pairs.
   *
   * <p>Using this class removes a dependency on underlying API responses and simplifies use of the
   * {@link S3Dao}.
   */
  @Data
  @AllArgsConstructor
  public static class S3ObjectDetails {
    /** The object's key. */
    private final String key;

    /** The object's eTag. */
    private final String eTag;

    /** The object's size. */
    private final long size;

    /** The object's last modified date. */
    private final Instant lastModified;

    /** Key/value pairs of metadata from the object. */
    private final Map<String, String> metaData;

    /**
     * Initializes an instance from a {@link HeadObjectResponse}.
     *
     * @param key The object's key
     * @param response object returned by S3 API
     */
    private S3ObjectDetails(String key, HeadObjectResponse response) {
      this.key = key;
      eTag = response.eTag();
      size = response.contentLength();
      lastModified = response.lastModified();
      metaData = Map.copyOf(response.metadata());
    }

    /**
     * Initializes an instance from a {@link GetObjectResponse}.
     *
     * @param key The object's key
     * @param response object returned by S3 API
     */
    private S3ObjectDetails(String key, GetObjectResponse response) {
      this.key = key;
      eTag = response.eTag();
      size = response.contentLength();
      lastModified = response.lastModified();
      metaData = Map.copyOf(response.metadata());
    }
  }
}
