package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
import com.google.common.io.ByteSource;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses an {@link AmazonS3} client and a bucket name to simplify creation of {@link MessageSource}s
 * that read FISS or MCS claims from the bucket.
 */
public class S3JsonMessageSources {
  private static final Logger LOGGER = LoggerFactory.getLogger(S3JsonMessageSources.class);
  /** S3 key prefix for Fiss files. */
  public static final String FISS_OBJECT_KEY_PREFIX = "fiss";
  /** S3 key prefix for MCS files. */
  public static final String MCS_OBJECT_KEY_PREFIX = "mcs";
  /** S3 suffix for files. */
  private static final String FILE_SUFFIX = "ndjson";

  /** Used to access data from S3 bucket. */
  private final S3DirectoryDao s3Dao;
  /** S3 key prefix for Fiss files. */
  private final String fissPrefix;
  /** S3 key prefix for MCS files. */
  private final String mcsPrefix;

  /**
   * Creates an instance using the specified S3, bucket, and optional directory within the bucket. A
   * path is only added if the directoryPath is non-empty.
   *
   * @param s3Dao the S3 cache to use
   */
  public S3JsonMessageSources(S3DirectoryDao s3Dao) {
    this.s3Dao = s3Dao;
    fissPrefix = FISS_OBJECT_KEY_PREFIX;
    mcsPrefix = MCS_OBJECT_KEY_PREFIX;
  }

  /**
   * Creates a {@link MessageSource.Factory} instance that creates {@link MessageSource}s for FISS
   * claim change records from objects in our bucket.
   *
   * @return the factory
   */
  public MessageSource.Factory<FissClaimChange> fissClaimChangeFactory() {
    return new S3BucketMessageSourceFactory<>(
        s3Dao, fissPrefix, FILE_SUFFIX, this::readFissClaimChanges, FissClaimChange::getSeq);
  }

  /**
   * Creates a {@link MessageSource.Factory} instance that creates {@link MessageSource}s for MCS
   * claim change records from objects in our bucket.
   *
   * @return the factory
   */
  public MessageSource.Factory<McsClaimChange> mcsClaimChangeFactory() {
    return new S3BucketMessageSourceFactory<>(
        s3Dao, mcsPrefix, FILE_SUFFIX, this::readMcsClaimChanges, McsClaimChange::getSeq);
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
   * @param minSeq the min sequence number
   * @param maxSeq the max sequence number
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
   * @param minSeq the min sequence number
   * @param maxSeq the max sequence number
   * @return a valid object key for MCS claims data
   */
  public String createMcsObjectKey(long minSeq, long maxSeq) {
    return S3BucketMessageSourceFactory.createValidObjectKey(
        mcsPrefix, FILE_SUFFIX, minSeq, maxSeq);
  }

  /**
   * Creates a message source from the object at the specified key location and parser.
   *
   * @param <T> the type parameter
   * @param ndjsonObjectKey the key of the object to read
   * @param parser the parser to parse the object
   * @return a message source created from the parsed object
   */
  private <T> MessageSource<T> createMessageSource(
      String ndjsonObjectKey, JsonMessageSource.Parser<T> parser) {
    LOGGER.info(
        "creating S3JsonMessageSource from S3: bucket={} key={}",
        s3Dao.getS3BucketName(),
        ndjsonObjectKey);
    try {
      ByteSource byteSource = s3Dao.downloadFile(ndjsonObjectKey);
      if (byteSource == null) {
        throw new RuntimeException(
            String.format("failed to download file from S3 bucket: key=%s", ndjsonObjectKey));
      }
      return new JsonMessageSource<>(byteSource, parser);
    } catch (IOException ex) {
      throw new RuntimeException(
          String.format("error while downloading file from S3 bucket: key=%s", ndjsonObjectKey),
          ex);
    }
  }
}
