package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

/**
 * Wrapper for a FissClaim or McsClaim source that promotes it to return a ClaimChange containing
 * the actual FiSS or MCS claim with a ChangeType of CHANGE_TYPE_UPDATE.
 *
 * @param <TChange> the type parameter for the change
 * @param <TClaim> the type parameter for the claim
 */
public class WrappedClaimSource<TChange, TClaim> implements MessageSource<TChange> {
  /** Cache used to select whether to return CHANGE_TYPE_UPDATE or CHANGE_TYPE_INSERT. */
  private static final int KEY_CACHE_SIZE = 20_000;

  /** The original source of FISS/MCS claims. */
  private final MessageSource<TClaim> source;
  /** Clock for writing timestamps. */
  private final Clock clock;
  /** The current sequence number. */
  private long sequenceNumber;
  /** Function for getting the key from a claim. */
  private final Function<TClaim, String> keyExtractor;
  /** Factory for getting changes to a claim. */
  private final ChangeFactory<TChange, TClaim> changeFactory;
  /** A cache of keys seen in claims. */
  private final Cache<String, String> knownKeys;

  /**
   * Creates a wrapper object to promote each claim from source into a ClaimChange object. A cache
   * is used to decide which ChangeType to use. The cache is imperfect but since this class is only
   * intended for testing with a random claim source it is suitable for testing purposes.
   *
   * @param source the actual source of FISS/MCS claims
   * @param clock the clock
   * @param sequenceNumber the sequence number
   * @param keyCacheSize the key cache size
   * @param keyExtractor the key extractor
   * @param changeFactory the change factory
   */
  @VisibleForTesting
  WrappedClaimSource(
      MessageSource<TClaim> source,
      Clock clock,
      long sequenceNumber,
      int keyCacheSize,
      Function<TClaim, String> keyExtractor,
      ChangeFactory<TChange, TClaim> changeFactory) {
    this.source = source;
    this.clock = clock;
    this.sequenceNumber = sequenceNumber;
    this.keyExtractor = keyExtractor;
    this.changeFactory = changeFactory;
    this.knownKeys = CacheBuilder.newBuilder().maximumSize(keyCacheSize).build();
  }

  @Override
  public MessageSource<TChange> skipTo(long startingSequenceNumber) throws Exception {
    sequenceNumber = startingSequenceNumber;
    source.skipTo(startingSequenceNumber);
    return this;
  }

  @Override
  public boolean hasNext() throws Exception {
    return source.hasNext();
  }

  @Override
  public TChange next() throws Exception {
    final Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(clock.instant().getEpochSecond()).build();
    final TClaim claim = source.next();
    final String key = keyExtractor.apply(claim);
    final boolean inserted = knownKeys.asMap().putIfAbsent(key, key) == null;
    final ChangeType changeType =
        inserted ? ChangeType.CHANGE_TYPE_INSERT : ChangeType.CHANGE_TYPE_UPDATE;
    return changeFactory.create(timestamp, changeType, sequenceNumber++, claim);
  }

  @Override
  public void close() throws Exception {
    source.close();
  }

  /**
   * Wraps fiss claims into a {@link MessageSource}.
   *
   * @param source the claim to wrap
   * @param clock the clock for timestamps
   * @param startingSequenceNumber the starting sequence number
   * @return the message source
   */
  public static MessageSource<FissClaimChange> wrapFissClaims(
      MessageSource<FissClaim> source, Clock clock, long startingSequenceNumber) {
    return new WrappedClaimSource<>(
        source,
        clock,
        startingSequenceNumber,
        KEY_CACHE_SIZE,
        FissClaim::getRdaClaimKey,
        (timestamp, type, seq, claim) ->
            FissClaimChange.newBuilder()
                .setTimestamp(timestamp)
                .setChangeType(type)
                .setSeq(seq)
                .setDcn(claim.getDcn())
                .setRdaClaimKey(claim.getRdaClaimKey())
                .setIntermediaryNb(claim.getIntermediaryNb())
                .setClaim(claim)
                .setSource(
                    RecordSource.newBuilder()
                        .setPhase("P1")
                        .setPhaseSeqNum(1)
                        .setExtractDate(LocalDate.now(clock).minusDays(2).toString())
                        .setTransmissionTimestamp(
                            clock.instant().minus(1, ChronoUnit.DAYS).toString())
                        .build())
                .build());
  }

  /**
   * Wraps MCS claims into a {@link MessageSource}.
   *
   * @param source the claim to wrap
   * @param clock the clock for timestamps
   * @param startingSequenceNumber the starting sequence number
   * @return the message source
   */
  public static MessageSource<McsClaimChange> wrapMcsClaims(
      MessageSource<McsClaim> source, Clock clock, long startingSequenceNumber) {
    return new WrappedClaimSource<>(
        source,
        clock,
        startingSequenceNumber,
        KEY_CACHE_SIZE,
        McsClaim::getIdrClmHdIcn,
        (timestamp, type, seq, claim) ->
            McsClaimChange.newBuilder()
                .setTimestamp(timestamp)
                .setChangeType(type)
                .setSeq(seq)
                .setClaim(claim)
                .setSource(
                    RecordSource.newBuilder()
                        .setPhase("P1")
                        .setPhaseSeqNum(1)
                        .setExtractDate(LocalDate.now(clock).minusDays(2).toString())
                        .setTransmissionTimestamp(
                            clock.instant().minus(1, ChronoUnit.DAYS).toString())
                        .build())
                .build());
  }

  /**
   * Gets changes from a claim.
   *
   * @param <TChange> the type for the change
   * @param <TClaim> the type for the claim
   */
  @FunctionalInterface
  public interface ChangeFactory<TChange, TClaim> {
    /**
     * Returns a change given a claim.
     *
     * @param timestamp the timestamp
     * @param type the type
     * @param sequenceNumber the sequence number
     * @param claim the claim
     * @return the change
     */
    TChange create(Timestamp timestamp, ChangeType type, long sequenceNumber, TClaim claim);
  }
}
