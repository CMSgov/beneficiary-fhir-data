package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;

/**
 * Uses an AmazonS3 client and a bucket name to simplify creation of MessageSources that ready FISS
 * or MCS claims from the bucket.
 */
public class S3JsonMessageSources {
  private final AmazonS3 s3Client;
  private final String bucketName;

  public S3JsonMessageSources(AmazonS3 s3Client, String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
  }

  /**
   * Creates a MessageSource that reads FissClaimChanges from an object in the bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  public MessageSource<FissClaimChange> readFissClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource::parseFissClaimChange);
  }

  /**
   * Creates a MessageSource that reads McsClaimChanges from an object in the bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  public MessageSource<McsClaimChange> readMcsClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource::parseMcsClaimChange);
  }

  private <T> MessageSource<T> createMessageSource(
      String ndjsonObjectKey, JsonMessageSource.Parser<T> parser) {
    return new S3JsonMessageSource<>(s3Client.getObject(bucketName, ndjsonObjectKey), parser);
  }
}
