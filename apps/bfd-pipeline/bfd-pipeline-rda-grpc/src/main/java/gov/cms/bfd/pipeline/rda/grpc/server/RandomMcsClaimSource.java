package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

/**
 * A ClaimSource implementation that generates and returns random {@link McsClaimChange} objects.
 * The random number seed and number of claims to return are specified in the constructor.
 */
public class RandomMcsClaimSource implements MessageSource<McsClaimChange> {
  /** The claim generator. */
  private final RandomMcsClaimGenerator generator;
  /** The maximum number of claims to send. */
  private final long maxToSend;
  /** Used to generate timestamps. */
  private final Clock clock;
  /** The number of claim sent. */
  private long sent;

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
    this.generator = new RandomMcsClaimGenerator(config);
    this.maxToSend = maxToSend;
    clock = config.getClock();
  }

  @Override
  public RandomMcsClaimSource skipTo(long startingSequenceNumber) {
    sent += generator.skipTo(startingSequenceNumber);
    return this;
  }

  @Override
  public boolean hasNext() {
    return sent < maxToSend;
  }

  @Override
  public McsClaimChange next() {
    if (sent >= maxToSend) {
      throw new NoSuchElementException();
    }
    sent += 1;

    final Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(clock.instant().getEpochSecond()).build();
    final var claim = generator.randomClaim();
    final var source =
        RecordSource.newBuilder()
            .setPhase("P1")
            .setPhaseSeqNum(1)
            .setExtractDate(LocalDate.now(clock).minusDays(2).toString())
            .setTransmissionTimestamp(clock.instant().minus(1, ChronoUnit.DAYS).toString())
            .build();
    final var change =
        McsClaimChange.newBuilder()
            .setTimestamp(timestamp)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setSeq(generator.getPreviousSequenceNumber())
            .setIcn(claim.getIdrClmHdIcn())
            .setClaim(claim)
            .setSource(source)
            .build();
    return change;
  }

  @Override
  public void close() {}
}
