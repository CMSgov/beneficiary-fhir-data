package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.function.BiConsumer;

/**
 * Wrapper for a FissClaim or McsClaim source that promotes it to return a ClaimChange containing
 * the actual FiSS or MCS claim with a ChangeType of CHANGE_TYPE_UPDATE.
 *
 * @param <T> either FissClaim or McsClaim
 */
public class WrappedClaimSource<T> implements MessageSource<ClaimChange> {
  private final MessageSource<T> source;
  private final BiConsumer<ClaimChange.Builder, T> setter;

  /**
   * Creates a wrapper object to promote each claim from source into a ClaimChange object with
   * ChangeType of CHANGE_TYPE_UPDATE.
   *
   * @param source the actual source of FISS/MCS claims
   * @param setter lambda to add the claim to the appropriate field in the ClaimChange builder
   */
  private WrappedClaimSource(MessageSource<T> source, BiConsumer<ClaimChange.Builder, T> setter) {
    this.source = source;
    this.setter = setter;
  }

  @Override
  public boolean hasNext() throws Exception {
    return source.hasNext();
  }

  @Override
  public ClaimChange next() throws Exception {
    final ClaimChange.Builder builder = ClaimChange.newBuilder();
    builder.setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE);
    setter.accept(builder, source.next());
    return builder.build();
  }

  @Override
  public void close() throws Exception {
    source.close();
  }

  public static WrappedClaimSource<FissClaim> wrapFissClaims(MessageSource<FissClaim> source) {
    return new WrappedClaimSource<>(source, ClaimChange.Builder::setFissClaim);
  }

  public static WrappedClaimSource<McsClaim> wrapMcsClaims(MessageSource<McsClaim> source) {
    return new WrappedClaimSource<>(source, ClaimChange.Builder::setMcsClaim);
  }
}
