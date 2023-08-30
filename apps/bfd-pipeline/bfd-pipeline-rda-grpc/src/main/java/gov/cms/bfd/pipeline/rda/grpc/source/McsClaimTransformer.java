package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.McsClaimRdaSink;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.util.List;
import lombok.Getter;

/**
 * Transforms a gRPC McsClaim object into a Hibernate {@link RdaMcsClaim} object. Note that the gRPC
 * data objects are not proper java beans since their optional field getters should only be called
 * if their corresponding &quot;has&quot; methods return true. Optional fields are ignored when not
 * present. All other fields are validated and copied into a new {@link RdaMcsClaim} object. A
 * lastUpdated time stamp is set using a Clock (for easier testing) and the MBI is hashed using a
 * {@link MbiCache}.
 */
public class McsClaimTransformer extends AbstractClaimTransformer {
  /** Used to generate time stamps. */
  private final Clock clock;

  /**
   * Used to convert MBI strings into {@link Mbi} instances containing the corresponding hashed
   * value.
   */
  @Getter private final MbiCache mbiCache;

  /** Used to perform actual transformation. */
  @Getter private final McsClaimParser claimParser;

  /**
   * Constructs a new instance with the given parameters.
   *
   * @param clock used to create time stamps
   * @param mbiCache used to lookup MBI hashes
   */
  public McsClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
    claimParser = new McsClaimParser(EnumStringExtractor::new, this::lookupMbiInCache);
  }

  /**
   * Hook to allow the {@link McsClaimRdaSink} to install an alternative {@link MbiCache}
   * implementation that supports caching MBI values in a database table.
   *
   * @param mbiCache alternative {@link MbiCache} to use for obtaining {@link Mbi} instances
   * @return a new transformer using the provided cache
   */
  public McsClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new McsClaimTransformer(clock, mbiCache);
  }

  /**
   * Transform and validate an incoming {@link McsClaimChange} message object to produce a
   * corresponding {@link RdaChange} containing a valid {@link RdaMcsClaim} instance.
   *
   * @param change the {@link McsClaimChange} to transform
   * @return a valid and populated {@link RdaChange}
   * @throws gov.cms.model.dsl.codegen.library.DataTransformer.TransformationException if the
   *     message could not be transformed
   */
  public RdaChange<RdaMcsClaim> transformClaim(McsClaimChange change)
      throws DataTransformer.TransformationException {
    McsClaim from = change.getClaim();

    final DataTransformer transformer = new DataTransformer();
    final RdaMcsClaim to = claimParser.transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());
    final RdaChange.Source source = transformSource(change.getSource(), transformer);

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
        transformer.instant(change.getTimestamp()),
        source);
  }

  /**
   * A function compatible with the {@link gov.cms.model.dsl.codegen.library.ExternalTransformation}
   * interface that can be passed to the {@link #claimParser} constructor. This method will be
   * called for every {@link McsClaim} message object to invoke the {@link #mbiCache} with the MBI
   * from the message and set the {@link RdaMcsClaim#getMbiRecord()} field appropriately.
   *
   * @param transformer {@link DataTransformer} used to validate/copy data
   * @param namePrefix prefix for field name used in error reporting
   * @param message {@link McsClaim} being transformed
   * @param entity {@link RdaMcsClaim} being populated
   */
  private void lookupMbiInCache(
      DataTransformer transformer, String namePrefix, McsClaim message, RdaMcsClaim entity) {
    if (message.hasIdrClaimMbi()) {
      transformer.validateString(
          namePrefix + RdaMcsClaim.Fields.idrClaimMbi, false, 1, 11, message.getIdrClaimMbi());
      entity.setMbiRecord(mbiCache.lookupMbi(message.getIdrClaimMbi()));
    }
  }
}
