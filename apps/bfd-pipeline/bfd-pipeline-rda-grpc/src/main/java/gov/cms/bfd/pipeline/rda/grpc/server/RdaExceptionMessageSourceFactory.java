package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import java.io.IOException;
import lombok.AllArgsConstructor;

/**
 * Decorator for another {@link RdaMessageSourceFactory} that wraps every {@link MessageSource} with
 * a {@link ExceptionMessageSource}.
 */
@AllArgsConstructor
public class RdaExceptionMessageSourceFactory implements RdaMessageSourceFactory {
  /** The {@link RdaMessageSourceFactory} being decorated. */
  private final RdaMessageSourceFactory realFactory;

  /**
   * Number of valid messages to be returned before the exception is thrown. {@see
   * ExceptionMessageSource#countBeforeThrow}
   */
  private final int countBeforeThrow;

  @Override
  public RdaService.Version getVersion() {
    return realFactory.getVersion();
  }

  /**
   * Returns the wrapped message source.
   *
   * <p>{@inheritDoc}
   *
   * @param startingSequenceNumber first sequence number to send to the client
   * @return the wrapped message source
   * @throws Exception if creating the real message source fails
   */
  @Override
  public MessageSource<FissClaimChange> createFissMessageSource(long startingSequenceNumber)
      throws Exception {
    return new ExceptionMessageSource<>(
        realFactory.createFissMessageSource(startingSequenceNumber),
        countBeforeThrow,
        IOException::new);
  }

  /**
   * Returns the wrapped message source.
   *
   * <p>{@inheritDoc}
   *
   * @param startingSequenceNumber first sequence number to send to the client
   * @return the wrapped message source
   * @throws Exception if creating the real message source fails
   */
  @Override
  public MessageSource<McsClaimChange> createMcsMessageSource(long startingSequenceNumber)
      throws Exception {
    return new ExceptionMessageSource<>(
        realFactory.createMcsMessageSource(startingSequenceNumber),
        countBeforeThrow,
        IOException::new);
  }

  @Override
  public void close() throws Exception {
    realFactory.close();
  }
}
