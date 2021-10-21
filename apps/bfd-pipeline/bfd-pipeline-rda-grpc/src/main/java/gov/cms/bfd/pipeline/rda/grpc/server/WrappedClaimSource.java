package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Wrapper for a FissClaim or McsClaim source that promotes it to return a ClaimChange containing
 * the actual FiSS or MCS claim with a ChangeType of CHANGE_TYPE_UPDATE.
 *
 * @param <T> either FissClaim or McsClaim
 */
public class WrappedClaimSource<TChange, TClaim> implements MessageSource<TChange> {
  private final MessageSource<TClaim> source;
  private final Clock clock;
  private long sequenceNumber;
  private final Function<TClaim, String> keyExtractor;
  private final ChangeFactory<TChange, TClaim> changeFactory;
  private final Set<String> keys;

  /**
   * Creates a wrapper object to promote each claim from source into a ClaimChange object with
   * ChangeType of CHANGE_TYPE_UPDATE.
   *
   * @param source the actual source of FISS/MCS claims
   * @param setter lambda to add the claim to the appropriate field in the ClaimChange builder
   */
  private WrappedClaimSource(
      MessageSource<TClaim> source,
      Clock clock,
      long sequenceNumber,
      Function<TClaim, String> keyExtractor,
      ChangeFactory<TChange, TClaim> changeFactory) {
    this.source = source;
    this.clock = clock;
    this.sequenceNumber = sequenceNumber;
    this.keyExtractor = keyExtractor;
    this.changeFactory = changeFactory;
    this.keys = Collections.synchronizedSet(new HashSet<>());
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
    final boolean inserted = keys.add(keyExtractor.apply(claim));
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
