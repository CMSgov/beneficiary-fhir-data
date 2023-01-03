package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.util.NoSuchElementException;

/**
 * A ClaimSource implementation that generates and returns random FissClaim objects. The random
 * number seed and number of claims to return are specified in the constructor.
 */
public class RandomFissClaimSource implements MessageSource<FissClaim> {
  private final RandomFissClaimGenerator generator;
  private final int maxToSend;
  private int sent;

  public RandomFissClaimSource(long seed, int maxToSend) {
    generator = new RandomFissClaimGenerator(seed);
    sent = 0;
    generator.setSequence(sent);
    this.maxToSend = maxToSend;
  }

  @Override
  public boolean hasNext() {
    return sent < maxToSend;
  }

  @Override
  public FissClaim next() {
    if (sent >= maxToSend) {
      throw new NoSuchElementException();
    }
    sent += 1;
    return generator.randomClaim();
  }

  @Override
  public void close() {}

  public MessageSource<FissClaimChange> toClaimChanges() {
    return WrappedClaimSource.wrapFissClaims(
        this, generator.getClock(), RdaChange.MIN_SEQUENCE_NUM);
  }
}
