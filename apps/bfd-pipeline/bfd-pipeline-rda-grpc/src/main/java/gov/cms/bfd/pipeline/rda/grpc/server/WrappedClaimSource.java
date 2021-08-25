package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
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
  private final ChangeFactory<TChange, TClaim> changeFactory;

  /**
   * Creates a wrapper object to promote each claim from source into a ClaimChange object with
   * ChangeType of CHANGE_TYPE_UPDATE.
   *
   * @param source the actual source of FISS/MCS claims
   * @param setter lambda to add the claim to the appropriate field in the ClaimChange builder
   */
  private WrappedClaimSource(
      MessageSource<TClaim> source, Clock clock, ChangeFactory<TChange, TClaim> changeFactory) {
    this.source = source;
    this.clock = clock;
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
    return changeFactory.create(timestamp, ChangeType.CHANGE_TYPE_UPDATE, source.next());
  }

  @Override
  public void close() throws Exception {
    source.close();
  }

  public static MessageSource<FissClaimChange> wrapFissClaims(
      MessageSource<FissClaim> source, Clock clock) {
    return new WrappedClaimSource<>(
        source,
        clock,
        (timestamp, type, claim) ->
            FissClaimChange.newBuilder()
                .setTimestamp(timestamp)
                .setChangeType(type)
                .setClaim(claim)
                .build());
  }

  public static MessageSource<McsClaimChange> wrapMcsClaims(
      MessageSource<McsClaim> source, Clock clock) {
    return new WrappedClaimSource<>(
        source,
        clock,
        (timestamp, type, claim) ->
            McsClaimChange.newBuilder()
                .setTimestamp(timestamp)
                .setChangeType(type)
                .setClaim(claim)
                .build());
  }

  @FunctionalInterface
  public interface ChangeFactory<TChange, TClaim> {
    public TChange create(Timestamp timestamp, ChangeType type, TClaim claim);
  }
}
