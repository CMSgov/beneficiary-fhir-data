package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.NoSuchElementException;

/**
 * A ClaimSource implementation that generates and returns random McsClaim objects. The random
 * number seed and number of claims to return are specified in the constructor.
 */
public class RandomMcsClaimSource implements MessageSource<McsClaim> {
  private final RandomMcsClaimGenerator generator;
  private final int maxToSend;
  private int sent;

  public RandomMcsClaimSource(long seed, int maxToSend) {
    generator = new RandomMcsClaimGenerator(seed);
    this.maxToSend = maxToSend;
    sent = 0;
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

  public MessageSource<ClaimChange> toClaimChanges() {
    return WrappedClaimSource.wrapMcsClaims(this, generator.getClock());
  }
}
