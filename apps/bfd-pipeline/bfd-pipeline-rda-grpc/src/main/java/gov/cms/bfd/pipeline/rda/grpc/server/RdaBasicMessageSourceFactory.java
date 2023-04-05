package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import lombok.AllArgsConstructor;

/**
 * Implementation of {@link RdaMessageSourceFactory} that uses predefined values for version and
 * claim factories.
 */
@AllArgsConstructor
public class RdaBasicMessageSourceFactory implements RdaMessageSourceFactory {
  /** The version returned by {@link RdaService#getVersion}. */
  private final RdaService.Version version;
  /** Source of records for {@link RdaService#getFissClaims}. */
  private final MessageSource.Factory<FissClaimChange> fissFactory;
  /** Source of records for {@link RdaService#getMcsClaims}. */
  private final MessageSource.Factory<McsClaimChange> mcsFactory;

  @Override
  public RdaService.Version getVersion() {
    return version;
  }

  @Override
  public MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber)
      throws Exception {
    return fissFactory.apply(startingSequenceNumber);
  }

  @Override
  public MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber)
      throws Exception {
    return mcsFactory.apply(startingSequenceNumber);
  }

  @Override
  public void close() throws Exception {}
}
