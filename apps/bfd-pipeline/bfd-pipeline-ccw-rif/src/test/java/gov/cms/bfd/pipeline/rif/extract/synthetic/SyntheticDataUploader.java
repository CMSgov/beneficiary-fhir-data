package gov.cms.bfd.pipeline.rif.extract.synthetic;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import gov.cms.bfd.model.rif.samples.TestDataSetLocation;
import gov.cms.bfd.pipeline.rif.extract.ExtractionOptions;
import gov.cms.bfd.pipeline.rif.extract.s3.DataSetManifest;
import gov.cms.bfd.pipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.rif.extract.s3.DataSetTestUtilities;
import gov.cms.bfd.pipeline.rif.extract.s3.S3Utilities;
import gov.cms.bfd.pipeline.rif.extract.synthetic.SyntheticDataFixer.SyntheticDataFile;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Small one-off helper app that uploads the synthetic data to the appropriate place in S3. */
public final class SyntheticDataUploader {
  private static final Logger LOGGER = LoggerFactory.getLogger(SyntheticDataUploader.class);

  /**
   * Pushes the synthetic data up to S3, replacing any versions that are already there.
   *
   * @param args (not used)
   * @throws Exception Any {@link Exception}s encountered will be bubbled up, halting the
   *     application.
   */
  public static void main(String[] args) throws Exception {
    ExtractionOptions options =
        new ExtractionOptions(String.format("bb-test-%d", new Random().nextInt(1000)));
    AmazonS3 s3Client = S3Utilities.createS3Client(options);

    LOGGER.info("Uploading original data...");
    uploadSyntheticData(
        s3Client,
        TestDataSetLocation.SYNTHETIC_DATA.getS3KeyPrefix().replace("-fixed", ""),
        syntheticDataFile -> syntheticDataFile.getOriginalFilePath());
    LOGGER.info("Uploading fixed data...");
    uploadSyntheticData(
        s3Client,
        TestDataSetLocation.SYNTHETIC_DATA.getS3KeyPrefix(),
        syntheticDataFile -> syntheticDataFile.getFixedFilePath());
    LOGGER.info("Uploaded all data.");
  }

  /**
   * @param s3Client the {@link AmazonS3} client to use
   * @param s3KeyPrefix the S3 key prefix to upload all objects under/into
   * @param syntheticDataPathGrabber the {@link Function} that returns the {@link Path} to upload
   *     from, for a given {@link SyntheticDataFile}
   * @throws MalformedURLException Any {@link MalformedURLException}s encountered will be bubbled
   *     up.
   */
  private static void uploadSyntheticData(
      AmazonS3 s3Client,
      String s3KeyPrefix,
      Function<SyntheticDataFile, Path> syntheticDataPathGrabber)
      throws MalformedURLException {
    Bucket bucket = new Bucket(TestDataSetLocation.S3_BUCKET_TEST_DATA);

    // Build a DataSetManifest for the data to be uploaded.
    List<DataSetManifestEntry> manifestEntries = new LinkedList<>();
    for (SyntheticDataFile syntheticDataFile : SyntheticDataFile.values())
      manifestEntries.add(
          new DataSetManifestEntry(
              syntheticDataPathGrabber.apply(syntheticDataFile).getFileName().toString(),
              syntheticDataFile.getRifFile().getFileType()));
    DataSetManifest manifest = new DataSetManifest(Instant.now(), 0, manifestEntries);

    // Upload the manifest and every file in it.
    PutObjectRequest manifestRequest =
        DataSetTestUtilities.createPutRequest(bucket, s3KeyPrefix, manifest);
    manifestRequest.setCannedAcl(CannedAccessControlList.PublicRead);
    s3Client.putObject(manifestRequest);
    LOGGER.info("Uploaded: manifest");
    for (SyntheticDataFile syntheticDataFile : SyntheticDataFile.values()) {
      DataSetManifestEntry manifestEntry =
          manifest.getEntries().stream()
              .filter(
                  e ->
                      e.getName()
                          .equals(
                              syntheticDataPathGrabber
                                  .apply(syntheticDataFile)
                                  .getFileName()
                                  .toString()))
              .findFirst()
              .get();

      PutObjectRequest fileRequest =
          DataSetTestUtilities.createPutRequest(
              bucket,
              s3KeyPrefix,
              manifest,
              manifestEntry,
              syntheticDataPathGrabber.apply(syntheticDataFile).toUri().toURL());
      fileRequest.setCannedAcl(CannedAccessControlList.PublicRead);
      s3Client.putObject(fileRequest);
      LOGGER.info("Uploaded: {}", syntheticDataFile.name());
    }
  }
}
