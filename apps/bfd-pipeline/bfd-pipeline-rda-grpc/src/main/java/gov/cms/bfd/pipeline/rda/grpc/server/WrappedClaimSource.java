package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.io.IOException;
import java.time.Clock;

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
  private final ChangeFactory<TChange, TClaim> changeFactory;

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
      ChangeFactory<TChange, TClaim> changeFactory) {
    this.source = source;
    this.clock = clock;
    this.sequenceNumber = sequenceNumber;
    this.changeFactory = changeFactory;
  }

  @Override
  public boolean hasNext() throws Exception {
    return source.hasNext();
  }

  @Override
  public TChange next() throws Exception {
    final Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(clock.instant().getEpochSecond()).build();
    return changeFactory.create(
        timestamp, ChangeType.CHANGE_TYPE_UPDATE, sequenceNumber++, source.next());
  }

  @Override
  public void close() throws IOException {
    source.close();
  }

  public static MessageSource<FissClaimChange> wrapFissClaims(
      MessageSource<FissClaim> source, Clock clock, long startingSequenceNumber) {
    return new WrappedClaimSource<>(
        source,
        clock,
        startingSequenceNumber,
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
