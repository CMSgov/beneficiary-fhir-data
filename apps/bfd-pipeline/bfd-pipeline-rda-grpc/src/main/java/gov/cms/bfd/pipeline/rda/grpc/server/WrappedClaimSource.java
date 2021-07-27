package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.function.BiConsumer;

public class WrappedClaimSource<T> implements MessageSource<ClaimChange> {
  private final MessageSource<T> source;
  private final BiConsumer<ClaimChange.Builder, T> setter;

  public WrappedClaimSource(MessageSource<T> source, BiConsumer<ClaimChange.Builder, T> setter) {
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
