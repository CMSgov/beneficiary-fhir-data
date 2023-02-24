package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.ChecksumException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask;
import gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Contains utilities that are useful when running tests that involve working with data sets in S3.
 *
 * <p>This is being left in <code>src/main</code> so that it can be used from other modules' tests,
 * without having to delve into classpath dark arts.
 */
public class DataSetTestUtilities {
  /**
   * Creates an S3 test bucket.
   *
   * @param s3Client the {@link S3Client} client to use
   * @return the bucket name of the new, random {@link Bucket} for use in an integration test
   */
  public static String createTestBucket(S3Client s3Client) {
    return SharedS3Utilities.createTestBucket(s3Client);
  }

  /**
   * Deletes the specified {@link Bucket} and all objects in it.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to empty and delete
   */
  public static void deleteObjectsAndBucket(S3Client s3Client, String bucket) {
    SharedS3Utilities.deleteTestBucket(s3Client, bucket);
  }

  private static void putObjectHelper(
      S3Client s3Client, PutObjectRequest putObjectRequest, DataSetManifest manifest) {
    try {
      // Serialize the manifest to a byte array.
      JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      ByteArrayOutputStream manifestOutputStream = new ByteArrayOutputStream();
      marshaller.marshal(manifest, manifestOutputStream);

      byte[] manifestByteArray = manifestOutputStream.toByteArray();
      InputStream manifestInputStream = new ByteArrayInputStream(manifestByteArray);

      s3Client.putObject(
          putObjectRequest,
          RequestBody.fromInputStream(manifestInputStream, Long.valueOf(manifestByteArray.length)));
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a put request for the specified S3 bucket.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to push as an object
   */
  public static void putObject(S3Client s3Client, String bucket, DataSetManifest manifest) {
    String keyPrefix =
        String.format(
            "%s/%s", CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, manifest.getTimestampText());
    putObject(s3Client, bucket, keyPrefix, manifest);
  }

  /**
   * Create put request within a given bucket for a manifest at a keyed location.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to push as an object
   * @param location the location to store the manifest, should be {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} or {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS}
   */
  public static void putObject(
      S3Client s3Client, String bucket, DataSetManifest manifest, String location) {
    String keyPrefix = String.format("%s/%s", location, manifest.getTimestampText());
    putObject(s3Client, bucket, keyPrefix, manifest);
  }

  public static void putObject(
      S3Client s3Client, String bucket, String keyPrefix, DataSetManifest manifest) {
    String objectKey =
        String.format("%s/%d_%s", keyPrefix, manifest.getSequenceId(), "manifest.xml");

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucket).key(objectKey).build();

    putObjectHelper(s3Client, putObjectRequest, manifest);
  }

  /**
   * Creates a put request, placing the items within the {@link
   * CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} key inside the given bucket.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   */
  public static void putObject(
      S3Client s3Client,
      String bucket,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl) {
    putObject(
        s3Client,
        bucket,
        manifest,
        manifestEntry,
        objectContentsUrl,
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS);
  }

  /**
   * Creates a put request, placing the items within the location key inside the given bucket.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   * @param incomingLocation the incoming location, should be {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} or {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS}
   */
  public static void putObject(
      S3Client s3Client,
      String bucket,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl,
      String incomingLocation) {
    String keyPrefix = String.format("%s/%s", incomingLocation, manifest.getTimestampText());
    putObject(s3Client, bucket, keyPrefix, manifest, manifestEntry, objectContentsUrl);
  }

  /**
   * Creates a put request for the specified S3 bucket.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param keyPrefix the S3 key prefix to store the new object under
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   */
  public static void putObject(
      S3Client s3Client,
      String bucket,
      String keyPrefix,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl) {
    String objectKey = String.format("%s/%s", keyPrefix, manifestEntry.getName());

    try {
      // If this isn't specified, the AWS API logs annoying warnings.
      Map<String, String> metadata = new HashMap<>();
      metadata.put("x-amz-meta-myVal", "test");

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucket)
              .key(objectKey)
              .metadata(metadata)
              .contentMD5(
                  ManifestEntryDownloadTask.computeMD5ChkSum(objectContentsUrl.openStream()))
              .build();

      putObjectHelper(s3Client, putObjectRequest, manifest);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new ChecksumException(
          "NoSuchAlgorithmException on file "
              + manifest.getTimestampText()
              + manifestEntry.getName()
              + "trying to build md5chksum",
          e);
    }
  }

  /**
   * Waits for the number of objects in the specified {@link Bucket} to equal the specified count.
   *
   * <p>This is needed because Amazon's S3 API is only <em>eventually</em> consistent for deletes,
   * per <a href="https://aws.amazon.com/s3/faqs/">Amazon S3 FAQs</a>.
   *
   * @param s3Client the {@link S3Client} client to use
   * @param bucket the name of the bucket to check
   * @param keyPrefix the S3 object key prefix of the objects to include in the count
   * @param expectedObjectCount the number of objects that should be in the specified {@link Bucket}
   * @param waitDuration the length of time to wait for the condition to be met before throwing an
   *     error
   */
  public static void waitForBucketObjectCount(
      S3Client s3Client,
      String bucket,
      String keyPrefix,
      int expectedObjectCount,
      Duration waitDuration) {
    Instant endTime = Instant.now().plus(waitDuration);

    int actualObjectCount = -1;
    while (Instant.now().isBefore(endTime)) {
      ListObjectsRequest listObjectsRequest =
          ListObjectsRequest.builder()
              .bucket(bucket)
              .prefix(String.format("%s/", keyPrefix))
              .build();
      actualObjectCount = s3Client.listObjects(listObjectsRequest).contents().size();
      if (expectedObjectCount == actualObjectCount) return;

      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // Shouldn't happen, as we're not using interrupts for anything.
        throw new IllegalStateException(e);
      }
    }

    throw new IllegalStateException(
        String.format(
            "S3 object count count incorrect. Expected '%d', but actual is '%d'.",
            expectedObjectCount, actualObjectCount));
  }
}
