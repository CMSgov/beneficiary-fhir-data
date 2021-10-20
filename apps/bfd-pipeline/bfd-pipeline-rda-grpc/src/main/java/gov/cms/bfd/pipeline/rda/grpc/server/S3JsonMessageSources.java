package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.AmazonS3;
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

  public S3JsonMessageSources(AmazonS3 s3Client, String bucketName) {
    this.s3Client = s3Client;
    this.bucketName = bucketName;
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
        FISS_OBJECT_KEY_PREFIX,
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
        MCS_OBJECT_KEY_PREFIX,
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

  public static String createFissObjectKey() {
    return S3BucketMessageSourceFactory.createValidObjectKey(FISS_OBJECT_KEY_PREFIX, FILE_SUFFIX);
  }

  public static String createFissObjectKey(long minSeq, long maxSeq) {
    return S3BucketMessageSourceFactory.createValidObjectKey(
        FISS_OBJECT_KEY_PREFIX, FILE_SUFFIX, minSeq, maxSeq);
  }

  public static String createMcsObjectKey() {
    return S3BucketMessageSourceFactory.createValidObjectKey(MCS_OBJECT_KEY_PREFIX, FILE_SUFFIX);
  }

  public static String createMcsObjectKey(long minSeq, long maxSeq) {
    return S3BucketMessageSourceFactory.createValidObjectKey(
        MCS_OBJECT_KEY_PREFIX, FILE_SUFFIX, minSeq, maxSeq);
  }

  private <T> MessageSource<T> createMessageSource(
      String ndjsonObjectKey, JsonMessageSource.Parser<T> parser) {
    LOGGER.info(
        "creating S3JsonMessageSource from S3: bucket={} key={}", bucketName, ndjsonObjectKey);
    return new S3JsonMessageSource<>(s3Client.getObject(bucketName, ndjsonObjectKey), parser);
  }
}
