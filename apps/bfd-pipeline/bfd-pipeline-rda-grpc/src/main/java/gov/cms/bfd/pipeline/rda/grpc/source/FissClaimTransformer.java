package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.model.rda.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.time.Clock;
import java.util.List;
import lombok.Getter;

/**
 * Transforms a gRPC FissClaim object into a Hibernate PreAdjFissClaim object. Note that the gRPC
 * data objects are not proper java beans since their optional field getters should only be called
 * if their corresponding &quot;has&quot; methods return true. Optional fields are ignored when not
 * present. All other fields are validated and copied into a new PreAdjFissClaim object. A
 * lastUpdated time stamp is set using a Clock (for easier testing) and the MBI is hashed using an
 * IdHasher.
 */
public class FissClaimTransformer {
  private final Clock clock;
  private final FissClaimTransformer2 claimTransformer;
  @Getter private final MbiCache mbiCache;

  public FissClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
    claimTransformer =
        new FissClaimTransformer2(
            this::computeMbiHash, EnumStringExtractor::new, this::lookupMbiInCache);
  }

  /**
   * Hook to allow the FissClaimRdaSink to install an alternative MbiCache implementation that
   * supports caching MBI values in a database table.
   *
   * @param mbiCache alternative MbiCache to use for obtaining Mbi instances
   * @return a new transformer with the same clock but alternative MbiCache
   */
  public FissClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new FissClaimTransformer(clock, mbiCache);
  }

  public RdaChange<PreAdjFissClaim> transformClaim(FissClaimChange change) {
    FissClaim from = change.getClaim();
    final DataTransformer transformer = new DataTransformer();
    final PreAdjFissClaim to =
        claimTransformer.transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());

    final List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: seq=%d dcn=%s errors=%s",
              errors.size(), change.getSeq(), from.getDcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()));
  }

  private void lookupMbiInCache(
      DataTransformer transformer, String namePrefix, FissClaim message, PreAdjFissClaim entity) {
    if (message.hasMbi()) {
      transformer.validateString(namePrefix + Mbi.Fields.mbi, false, 1, 11, message.getMbi());
      entity.setMbiRecord(mbiCache.lookupMbi(message.getMbi()));
    }
  }

  private String computeMbiHash(String mbi) {
    return mbiCache.lookupMbi(mbi).getHash();
  }
}
