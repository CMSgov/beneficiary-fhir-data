package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.base.Strings;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an {@link AmazonS3} client and a bucket name to simplify creation of {@link MessageSource}s
 * that read FISS or MCS claims from the bucket.
 */
public class S3JsonMessageSources {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3JsonMessageSources.class);
  public static final String FISS_OBJECT_KEY_PREFIX = "fiss";
  public static final String MCS_OBJECT_KEY_PREFIX = "mcs";
  private static final String FILE_SUFFIX = "ndjson";

  private final AmazonS3 s3Client;
  @Getter private final String bucketName;
  private final String fissPrefix;
  private final String mcsPrefix;

  /**
   * Creates an instance using the specified S3, bucket, and optional directory within the bucket. A
   * path is only added if the directoryPath is non-empty.
   *
   * @param s3Client the S3 client to use
   * @param bucketName the bucket files are stored in
   * @param directoryPath the path within the bucket
   */
  public S3JsonMessageSources(AmazonS3 s3Client, String bucketName, String directoryPath) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
    fissPrefix = createCompoundKeyPrefix(directoryPath, FISS_OBJECT_KEY_PREFIX);
    mcsPrefix = createCompoundKeyPrefix(directoryPath, MCS_OBJECT_KEY_PREFIX);
  }

  /**
   * Creates a {@link MessageSource.Factory} instance that creates {@link MessageSource}s for FISS
   * claim change records from objects in our bucket.
   *
   * @return the factory
   */
  public MessageSource.Factory<FissClaimChange> fissClaimChangeFactory() {
    return new S3BucketMessageSourceFactory<>(
        s3Client,
        bucketName,
        fissPrefix,
        FILE_SUFFIX,
        this::readFissClaimChanges,
        FissClaimChange::getSeq);
  }

  /**
   * Creates a {@link MessageSource.Factory} instance that creates {@link MessageSource}s for MCS
   * claim change records from objects in our bucket.
   *
   * @return the factory
   */
  public MessageSource.Factory<McsClaimChange> mcsClaimChangeFactory() {
    return new S3BucketMessageSourceFactory<>(
        s3Client,
        bucketName,
        mcsPrefix,
        FILE_SUFFIX,
        this::readMcsClaimChanges,
        McsClaimChange::getSeq);
  }

  /**
   * Creates a {@link MessageSource} that reads {@link FissClaimChange} from an object in the
   * bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  public MessageSource<FissClaimChange> readFissClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource::parseFissClaimChange);
  }

  /**
   * Creates a {@link MessageSource} that reads {@link McsClaimChange} from an object in the bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  public MessageSource<McsClaimChange> readMcsClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource::parseMcsClaimChange);
  }

  /**
   * Creates an object key compatible with the MessageSources returned by this object. Intended for
   * use in integration tests that need to upload files without using hard coded keys.
   *
   * @return a valid object key for FISS claims data
   */
  public String createFissObjectKey() {
    return S3BucketMessageSourceFactory.createValidObjectKey(fissPrefix, FILE_SUFFIX);
  }

  /**
   * Creates an object key compatible with the MessageSources returned by this object. Allows the
   * range of sequence numbers within the file to be added to the key in a compatible way. Intended
   * for use in integration tests that need to upload files without using hard coded keys.
   *
   * @return a valid object key for FISS claims data
   */
  public String createFissObjectKey(long minSeq, long maxSeq) {
    return S3BucketMessageSourceFactory.createValidObjectKey(
        fissPrefix, FILE_SUFFIX, minSeq, maxSeq);
  }

  /**
   * Creates an object key compatible with the MessageSources returned by this object. Intended for
   * use in integration tests that need to upload files without using hard coded keys.
   *
   * @return a valid object key for FISS claims data
   */
  public String createMcsObjectKey() {
    return S3BucketMessageSourceFactory.createValidObjectKey(mcsPrefix, FILE_SUFFIX);
  }

  /**
   * Creates an object key compatible with the MessageSources returned by this object. Allows the
   * range of sequence numbers within the file to be added to the key in a compatible way. Intended
   * for use in integration tests that need to upload files without using hard coded keys.
   *
   * @return a valid object key for MCS claims data
   */
  public String createMcsObjectKey(long minSeq, long maxSeq) {
    return S3BucketMessageSourceFactory.createValidObjectKey(
        mcsPrefix, FILE_SUFFIX, minSeq, maxSeq);
  }

  private <T> MessageSource<T> createMessageSource(
      String ndjsonObjectKey, JsonMessageSource.Parser<T> parser) {
    LOGGER.info(
        "creating S3JsonMessageSource from S3: bucket={} key={}", bucketName, ndjsonObjectKey);
    return new S3JsonMessageSource<>(s3Client.getObject(bucketName, ndjsonObjectKey), parser);
  }

  private static String createCompoundKeyPrefix(String directoryPath, String keyPrefix) {
    if (Strings.isNullOrEmpty(directoryPath)) {
      return keyPrefix;
    } else if (directoryPath.endsWith("/")) {
      return directoryPath + keyPrefix;
    } else {
      return String.format("%s/%s", directoryPath, keyPrefix);
    }
  }
}
