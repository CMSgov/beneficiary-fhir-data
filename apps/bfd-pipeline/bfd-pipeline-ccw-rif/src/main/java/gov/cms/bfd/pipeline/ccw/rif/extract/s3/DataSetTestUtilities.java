package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.exceptions.ChecksumException;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.task.ManifestEntryDownloadTask;
import gov.cms.bfd.pipeline.sharedutils.s3.S3Dao;
import gov.cms.bfd.sharedutils.exceptions.UncheckedJaxbException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Contains utilities that are useful when running tests that involve working with data sets in S3.
 *
 * <p>This is being left in {@code src/main} so that it can be used from other modules' tests,
 * without having to delve into classpath dark arts.
 */
public class DataSetTestUtilities {
  /**
   * Computes the key prefix for a given manifest's contents within an S3 bucket.
   *
   * @param location the location to find the object, should be {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS}, {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS}, or {@link
   *     CcwRifLoadJob#S3_PREFIX_COMPLETED_DATA_SETS}
   * @param manifest the {@link DataSetManifest} to push as an object
   * @return the computed key prefix
   */
  public static String keyPrefixForManifest(String location, DataSetManifest manifest) {
    return String.format("%s/%s", location, manifest.getTimestampText());
  }

  /**
   * Creates a put request for the specified S3 bucket.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to push as an object
   * @return the uploaded object's s3 key
   */
  public static String putObject(S3Dao s3Dao, String bucket, DataSetManifest manifest) {
    String keyPrefix = keyPrefixForManifest(CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS, manifest);
    return putObject(s3Dao, bucket, keyPrefix, manifest);
  }

  /**
   * Create put request within a given bucket for a manifest at a keyed location.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to push as an object
   * @param location the location to store the manifest, should be {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} or {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS}
   * @return the uploaded object's s3 key
   */
  public static String putObject(
      S3Dao s3Dao, String bucket, DataSetManifest manifest, String location) {
    String keyPrefix = keyPrefixForManifest(location, manifest);
    return putObject(s3Dao, bucket, keyPrefix, manifest);
  }

  /**
   * Create put request within a given bucket for a manifest at a keyed location.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param keyPrefix the key prefix of the object
   * @param manifest the {@link DataSetManifest} to push as an object
   * @return the uploaded object's s3 key
   */
  public static String putObject(
      S3Dao s3Dao, String bucket, String keyPrefix, DataSetManifest manifest) {
    String objectKey =
        String.format("%s/%d_%s", keyPrefix, manifest.getSequenceId(), "manifest.xml");

    try {
      // Serialize the manifest to a byte array.
      JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      ByteArrayOutputStream manifestOutputStream = new ByteArrayOutputStream();
      marshaller.marshal(manifest, manifestOutputStream);

      byte[] manifestByteArray = manifestOutputStream.toByteArray();
      s3Dao.putObject(bucket, objectKey, manifestByteArray, Map.of());
      return objectKey;
    } catch (JAXBException e) {
      throw new UncheckedJaxbException(e);
    }
  }

  /**
   * Creates a put request, placing the items within the {@link
   * CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} key inside the given bucket.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   * @return the uploaded object's s3 key
   */
  public static String putObject(
      S3Dao s3Dao,
      String bucket,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl) {
    return putObject(
        s3Dao,
        bucket,
        manifest,
        manifestEntry,
        objectContentsUrl,
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS);
  }

  /**
   * Creates a put request, placing the items within the location key inside the given bucket.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   * @param incomingLocation the incoming location, should be {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_DATA_SETS} or {@link
   *     CcwRifLoadJob#S3_PREFIX_PENDING_SYNTHETIC_DATA_SETS}
   * @return the uploaded object's s3 key
   */
  public static String putObject(
      S3Dao s3Dao,
      String bucket,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl,
      String incomingLocation) {
    String keyPrefix = String.format("%s/%s", incomingLocation, manifest.getTimestampText());
    return putObject(s3Dao, bucket, keyPrefix, manifest, manifestEntry, objectContentsUrl);
  }

  /**
   * Creates a put request for the specified S3 bucket.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to place the new object in
   * @param keyPrefix the S3 key prefix to store the new object under
   * @param manifest the {@link DataSetManifest} to create an object for
   * @param manifestEntry the {@link DataSetManifestEntry} to create an object for
   * @param objectContentsUrl a {@link URL} to the data to push as the new object's content
   * @return the uploaded object's s3 key
   */
  public static String putObject(
      S3Dao s3Dao,
      String bucket,
      String keyPrefix,
      DataSetManifest manifest,
      DataSetManifestEntry manifestEntry,
      URL objectContentsUrl) {
    String objectKey = String.format("%s/%s", keyPrefix, manifestEntry.getName());

    try {
      String md5ChkSum = ManifestEntryDownloadTask.computeMD5ChkSum(objectContentsUrl.openStream());
      Map<String, String> metaData = Map.of("md5chksum", md5ChkSum);
      s3Dao.putObject(bucket, objectKey, objectContentsUrl, metaData);
      return objectKey;
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
   * Waits for the number of objects in the specified bucket to equal the specified count.
   *
   * <p>This is needed because Amazon's S3 API is only <em>eventually</em> consistent for deletes,
   * per <a href="https://aws.amazon.com/s3/faqs/">Amazon S3 FAQs</a>.
   *
   * @param s3Dao the {@link S3Dao} client to use
   * @param bucket the name of the bucket to check
   * @param keyPrefix the S3 object key prefix of the objects to include in the count
   * @param expectedObjectCount the number of objects that should be in the specified bucket
   * @param waitDuration the length of time to wait for the condition to be met before throwing an
   *     error
   */
  public static void waitForBucketObjectCount(
      S3Dao s3Dao,
      String bucket,
      String keyPrefix,
      int expectedObjectCount,
      Duration waitDuration) {
    Instant endTime = Instant.now().plus(waitDuration);

    final String prefix = String.format("%s/", keyPrefix);
    long actualObjectCount = -1L;
    while (Instant.now().isBefore(endTime)) {
      actualObjectCount = s3Dao.listObjects(bucket, prefix).count();
      if (expectedObjectCount == actualObjectCount) {
        return;
      }

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
