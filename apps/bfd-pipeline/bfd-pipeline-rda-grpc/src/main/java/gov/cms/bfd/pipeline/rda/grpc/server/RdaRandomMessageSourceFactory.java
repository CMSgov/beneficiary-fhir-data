package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import lombok.AllArgsConstructor;

/**
 * Implementation of {@link RdaMessageSourceFactory} that uses predefined value for version and
 * produces random claims on demand.
 */
@AllArgsConstructor
public class RdaRandomMessageSourceFactory implements RdaMessageSourceFactory {
  /** The version returned by {@link RdaService#getVersion}. */
  private final RdaService.Version version;
  /** Source of records for {@link RdaService#getFissClaims} and {@link RdaService#getMcsClaims}. */
  private final RandomClaimGeneratorConfig config;
  /** Maximum number of claims of each type to return. */
  private final int maxToSend;

  @Override
  public RdaService.Version getVersion() {
    return version;
  }

  @Override
  public MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber) {
    return new RandomFissClaimSource(config, maxToSend).skipTo(startingSequenceNumber);
  }

  @Override
  public MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber) {
    return new RandomMcsClaimSource(config, maxToSend).skipTo(startingSequenceNumber);
  }

  @Override
  public void close() throws Exception {}
}
