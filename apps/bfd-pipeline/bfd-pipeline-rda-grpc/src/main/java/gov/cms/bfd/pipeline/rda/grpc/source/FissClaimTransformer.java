package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.FissClaimRdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.parsers.FissClaimParser;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.time.Clock;
import java.util.List;
import lombok.Getter;

/**
 * Transforms a gRPC FissClaim object into a Hibernate {@link RdaFissClaim} object. Note that the
 * gRPC data objects are not proper java beans since their optional field getters should only be
 * called if their corresponding &quot;has&quot; methods return true. Optional fields are ignored
 * when not present. All other fields are validated and copied into a new {@link RdaFissClaim}
 * object. A lastUpdated time stamp is set using a Clock (for easier testing) and the MBI is hashed
 * using a {@link MbiCache}.
 */
public class FissClaimTransformer extends AbstractClaimTransformer {
  /** Used to generate time stamps. */
  private final Clock clock;

  /**
   * Used to convert MBI strings into {@link Mbi} instances containing the corresponding hashed
   * value.
   */
  @Getter private final MbiCache mbiCache;

  /** Used to perform actual transformation. */
  @Getter private final FissClaimParser claimParser;

  /**
   * Constructs a new instance with the given parameters.
   *
   * @param clock used to create time stamps
   * @param mbiCache used to lookup MBI hashes
   */
  public FissClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
    claimParser = new FissClaimParser(EnumStringExtractor::new, this::lookupMbiInCache);
  }

  /**
   * Hook to allow the {@link FissClaimRdaSink} to install an alternative {@link MbiCache}
   * implementation that supports caching MBI values in a database table.
   *
   * @param mbiCache alternative {@link MbiCache} to use for obtaining {@link Mbi} instances
   * @return a new transformer using the provided cache
   */
  public FissClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new FissClaimTransformer(clock, mbiCache);
  }

  /**
   * Transform and validate an incoming {@link FissClaimChange} message object to produce a
   * corresponding {@link RdaChange} containing a valid {@link RdaFissClaim} instance.
   *
   * @param change the {@link FissClaimChange} to transform
   * @return a valid and populated {@link RdaChange}
   * @throws gov.cms.model.dsl.codegen.library.DataTransformer.TransformationException if the
   *     message could not be transformed
   */
  public RdaChange<RdaFissClaim> transformClaim(FissClaimChange change)
      throws DataTransformer.TransformationException {
    FissClaim from = change.getClaim();
    final DataTransformer transformer = new DataTransformer();
    final RdaFissClaim to = claimParser.transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());
    final RdaChange.Source source = transformSource(change.getSource(), transformer);

    final List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: seq=%d rdaClaimKey=%s errors=%s",
              errors.size(), change.getSeq(), from.getRdaClaimKey(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }

    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()),
        source);
  }

  /**
   * A function compatible with the {@link gov.cms.model.dsl.codegen.library.ExternalTransformation}
   * interface that can be passed to the {@link #claimParser} constructor. This method will be
   * called for every {@link FissClaim} message object to invoke the {@link #mbiCache} with the MBI
   * from the message and set the {@link RdaFissClaim#getMbiRecord()} field appropriately.
   *
   * @param transformer {@link DataTransformer} used to validate/copy data
   * @param namePrefix prefix for field name used in error reporting
   * @param message {@link FissClaim} being transformed
   * @param entity {@link RdaFissClaim} being populated
   */
  private void lookupMbiInCache(
      DataTransformer transformer, String namePrefix, FissClaim message, RdaFissClaim entity) {
    if (message.hasMbi()) {
      transformer.validateString(
          namePrefix + RdaFissClaim.Fields.mbi, false, 1, 11, message.getMbi());
      entity.setMbiRecord(mbiCache.lookupMbi(message.getMbi()));
    }
  }
}
