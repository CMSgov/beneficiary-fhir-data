package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.io.CharSource;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import lombok.AllArgsConstructor;

/**
 * Implementation of {@link RdaMessageSourceFactory} that uses predefined value for version and
 * reads claims from predefined {@link CharSource}s containing NDJSON data.
 */
@AllArgsConstructor
public class RdaJsonMessageSourceFactory implements RdaMessageSourceFactory {
  /** The version returned by {@link RdaService#getVersion}. */
  private final RdaService.Version version;
  /** JSON data containing FISS claims. */
  private final CharSource fissJson;
  /** JSON data containing MCS claims. */
  private final CharSource mcsJson;

  @Override
  public RdaService.Version getVersion() {
    return version;
  }

  @Override
  public MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber)
      throws Exception {
    return new JsonMessageSource<>(fissJson, JsonMessageSource.fissParser())
        .skipTo(startingSequenceNumber);
  }

  @Override
  public MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber)
      throws Exception {
    return new JsonMessageSource<>(mcsJson, JsonMessageSource.mcsParser())
        .skipTo(startingSequenceNumber);
  }

  @Override
  public void close() throws Exception {}
}
