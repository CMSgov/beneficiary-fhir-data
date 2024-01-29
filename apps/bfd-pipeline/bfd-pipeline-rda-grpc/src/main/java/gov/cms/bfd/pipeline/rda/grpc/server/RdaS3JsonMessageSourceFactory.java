package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.sharedutils.s3.S3DirectoryDao;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

/**
 * Uses an {@link S3DirectoryDao} client and a bucket name to simplify creation of {@link
 * MessageSource}s that read FISS or MCS claims from the bucket.
 */
@Slf4j
public class RdaS3JsonMessageSourceFactory implements RdaMessageSourceFactory {
  /** S3 key prefix for Fiss files. */
  public static final String FISS_PREFIX = "fiss";

  /** S3 key prefix for MCS files. */
  public static final String MCS_PREFIX = "mcs";

  /** S3 suffix for files. */
  private static final String FILE_SUFFIX = "ndjson";

  /** Used to access data from S3 bucket. */
  private final S3DirectoryDao s3Dao;

  /** The version returned by {@link RdaService#getVersion}. */
  private final RdaService.Version version;

  /** Source of records for {@link RdaService#getFissClaims}. */
  private final S3BucketMessageSourceFactory<FissClaimChange> fissFactory;

  /** Source of records for {@link RdaService#getMcsClaims}. */
  private final S3BucketMessageSourceFactory<McsClaimChange> mcsFactory;

  /**
   * Initialize an instance using the provided version and {@link S3DirectoryDao}.
   *
   * @param version version to return to clients
   * @param s3Dao used to access data in S3
   */
  public RdaS3JsonMessageSourceFactory(RdaService.Version version, S3DirectoryDao s3Dao) {
    this.version = version;
    this.s3Dao = s3Dao;
    fissFactory =
        new S3BucketMessageSourceFactory<>(
            s3Dao, FISS_PREFIX, FILE_SUFFIX, this::readFissClaimChanges);
    mcsFactory =
        new S3BucketMessageSourceFactory<>(
            s3Dao, MCS_PREFIX, FILE_SUFFIX, this::readMcsClaimChanges);
  }

  @Override
  public RdaService.Version getVersion() {
    return version;
  }

  @Override
  public MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber)
      throws Exception {
    return fissFactory.createMessageSource(startingSequenceNumber);
  }

  @Override
  public MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber)
      throws Exception {
    return mcsFactory.createMessageSource(startingSequenceNumber);
  }

  /**
   * Closes our {@link S3DirectoryDao}. {@inheritDoc}
   *
   * @throws Exception pass through
   */
  @Override
  public void close() throws Exception {
    s3Dao.close();
  }

  /**
   * Creates a valid S3 key for tests that upload data into an S3 bucket for testing.
   *
   * @return valid FISS claim data key
   */
  @VisibleForTesting
  public static String createValidFissKeyForTesting() {
    return S3BucketMessageSourceFactory.createValidObjectKey(FISS_PREFIX, FILE_SUFFIX);
  }

  /**
   * Creates a valid S3 key for tests that upload data into an S3 bucket for testing.
   *
   * @return valid MCS claim data key
   */
  @VisibleForTesting
  public static String createValidMcsKeyForTesting() {
    return S3BucketMessageSourceFactory.createValidObjectKey(MCS_PREFIX, FILE_SUFFIX);
  }

  /**
   * Creates a {@link MessageSource} that reads {@link FissClaimChange} from an object in the
   * bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  private MessageSource<FissClaimChange> readFissClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource.fissParser());
  }

  /**
   * Creates a {@link MessageSource} that reads {@link McsClaimChange} from an object in the bucket.
   *
   * @param ndjsonObjectKey identifies the object containing our NDJSON data
   * @return a MessageSource that reads and parses the data
   */
  private MessageSource<McsClaimChange> readMcsClaimChanges(String ndjsonObjectKey) {
    return createMessageSource(ndjsonObjectKey, JsonMessageSource.mcsParser());
  }

  /**
   * Creates a message source from the object with the specified key and parser.
   *
   * @param <T> the type parameter
   * @param ndjsonObjectKey the key of the object to read
   * @param parser the parser to parse the object
   * @return a message source created from the parsed object
   */
  private <T> MessageSource<T> createMessageSource(
      String ndjsonObjectKey, JsonMessageSource.Parser<T> parser) {
    log.info(
        "creating S3JsonMessageSource from S3: bucket={} key={}",
        s3Dao.getS3BucketName(),
        ndjsonObjectKey);
    try {
      final var byteSource = s3Dao.downloadFile(ndjsonObjectKey);
      final var charSource = byteSource.asCharSource(StandardCharsets.UTF_8);
      return new JsonMessageSource<>(charSource, parser);
    } catch (IOException ex) {
      throw new RuntimeException(
          String.format("error while downloading file from S3 bucket: key=%s", ndjsonObjectKey),
          ex);
    }
  }
}
