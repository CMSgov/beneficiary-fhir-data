package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.util.function.Function;

/**
 * Wrapper for a FissClaim or McsClaim source that promotes it to return a ClaimChange containing
 * the actual FiSS or MCS claim with a ChangeType of CHANGE_TYPE_UPDATE.
 *
 * @param <T> either FissClaim or McsClaim
 */
public class WrappedClaimSource<TChange, TClaim> implements MessageSource<TChange> {
  // Cache used to select whether to return CHANGE_TYPE_UPDATE or CHANGE_TYPE_INSERT.
  private static final int KEY_CACHE_SIZE = 20_000;

  private final MessageSource<TClaim> source;
  private final Clock clock;
  private long sequenceNumber;
  private final Function<TClaim, String> keyExtractor;
  private final ChangeFactory<TChange, TClaim> changeFactory;
  private final Cache<String, String> knownKeys;

  /**
   * Creates a wrapper object to promote each claim from source into a ClaimChange object. A cache
   * is used to decide which ChangeType to use. The cache is imperfect but since this class is only
   * intended for testing with a random claim source it is suitable for testing purposes.
   *
   * @param source the actual source of FISS/MCS claims
   * @param setter lambda to add the claim to the appropriate field in the ClaimChange builder
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

  public static MessageSource<FissClaimChange> wrapFissClaims(
      MessageSource<FissClaim> source, Clock clock, long startingSequenceNumber) {
    return new WrappedClaimSource<>(
        source,
        clock,
        startingSequenceNumber,
        KEY_CACHE_SIZE,
        FissClaim::getDcn,
        (timestamp, type, seq, claim) ->
            FissClaimChange.newBuilder()
                .setTimestamp(timestamp)
                .setChangeType(type)
                .setSeq(seq)
                .setClaim(claim)
                .build());
  }

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
                .build());
  }

  @FunctionalInterface
  public interface ChangeFactory<TChange, TClaim> {
    TChange create(Timestamp timestamp, ChangeType type, long sequenceNumber, TClaim claim);
  }
}
