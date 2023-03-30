package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.NoSuchElementException;

/**
 * A ClaimSource implementation that generates and returns random McsClaim objects. The random
 * number seed and number of claims to return are specified in the constructor.
 */
public class RandomMcsClaimSource implements MessageSource<McsClaim> {
  /** The claim generator. */
  private final RandomMcsClaimGenerator generator;
  /** The maximum number of claims to send. */
  private final int maxToSend;
  /** The number of claim sent. */
  private int sent;

  /**
   * Creates a new instance.
   *
   * @param seed the seed for randomization
   * @param maxToSend the maximum number of claims to send
   */
  public RandomMcsClaimSource(long seed, int maxToSend) {
    this(RandomClaimGeneratorConfig.builder().seed(seed).build(), maxToSend);
  }

  /**
   * Creates a new instance.
   *
   * @param config the random generator configuration
   * @param maxToSend the maximum number of claims to send
   */
  public RandomMcsClaimSource(RandomClaimGeneratorConfig config, int maxToSend) {
    generator = new RandomMcsClaimGenerator(config);
    sent = 0;
    generator.setSequence(sent);
    this.maxToSend = maxToSend;
  }

  @Override
  public MessageSource<McsClaim> skip(long numberToSkip) throws Exception {
    sent += numberToSkip;
    generator.incrementSequence(numberToSkip);
    return this;
  }

  @Override
  public boolean hasNext() {
    return sent < maxToSend;
  }

  @Override
  public McsClaim next() {
    if (sent >= maxToSend) {
      throw new NoSuchElementException();
    }
    sent += 1;
    return generator.randomClaim();
  }

  @Override
  public void close() {}

  /**
   * Wraps the generator such that a message source is returned.
   *
   * @return the message source
   */
  public MessageSource<McsClaimChange> toClaimChanges() {
    return WrappedClaimSource.wrapMcsClaims(this, generator.getClock(), RdaChange.MIN_SEQUENCE_NUM);
  }
}
