package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.MbiUtil;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.model.rda.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.util.List;
import lombok.Getter;

public class McsClaimTransformer {
  private final Clock clock;
  private final McsClaimTransformer2 claimTransformer;
  @Getter private final MbiCache mbiCache;

  public McsClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
    claimTransformer =
        new McsClaimTransformer2(
            this::computeMbiHash, EnumStringExtractor::new, this::lookupMbiInCache);
  }

  /**
   * Hook to allow the McsClaimRdaSink to install an alternative MbiCache implementation that
   * supports caching MBI values in a database table.
   *
   * @param mbiCache alternative MbiCache to use for obtaining Mbi instances
   * @return a new transformer with the same clock but alternative MbiCache
   */
  public McsClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new McsClaimTransformer(clock, mbiCache);
  }

  public RdaChange<PreAdjMcsClaim> transformClaim(McsClaimChange change) {
    McsClaim from = change.getClaim();

    final DataTransformer transformer = new DataTransformer();
    final PreAdjMcsClaim to = claimTransformer.transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());

    final List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: seq=%d clmHdIcn=%s errors=%s",
              errors.size(), change.getSeq(), from.getIdrClmHdIcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()));
  }

  private void lookupMbiInCache(
      DataTransformer transformer, String namePrefix, McsClaim message, PreAdjMcsClaim entity) {
    if (message.hasIdrClaimMbi()) {
      transformer.validateString(
          namePrefix + MbiUtil.McsFields.mbi, false, 1, 11, message.getIdrClaimMbi());
      entity.setMbiRecord(mbiCache.lookupMbi(message.getIdrClaimMbi()));
    }
  }

  private String computeMbiHash(String mbi) {
    return mbiCache.lookupMbi(mbi).getHash();
  }
}
