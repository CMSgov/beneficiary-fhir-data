package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Timestamp;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.NoSuchElementException;

/**
 * A ClaimSource implementation that generates and returns random {@link FissClaimChange} objects.
 * The random number seed and number of claims to return are specified in the constructor.
 */
public class RandomFissClaimSource implements MessageSource<FissClaimChange> {
  /** The random claim generator. */
  private final RandomFissClaimGenerator generator;

  /** The maximum number of claims to send. */
  private final long maxToSend;

  /** Used to generate timestamps. */
  private final Clock clock;

  /** The number of sent claims. */
  private long sent;

  /**
   * Creates a new instance.
   *
   * @param seed the seed for randomization
   * @param maxToSend the max number of claims to send
   */
  public RandomFissClaimSource(long seed, int maxToSend) {
    this(RandomClaimGeneratorConfig.builder().seed(seed).maxToSend(maxToSend).build());
  }

  /**
   * Creates a new instance.
   *
   * @param config the random generator configuration
   */
  public RandomFissClaimSource(RandomClaimGeneratorConfig config) {
    this.generator = new RandomFissClaimGenerator(config);
    this.maxToSend = config.getMaxToSend();
    clock = config.getClock();
  }

  @Override
  public RandomFissClaimSource skipTo(long startingSequenceNumber) {
    sent += generator.skipTo(startingSequenceNumber);
    return this;
  }

  @Override
  public boolean hasNext() {
    return sent < maxToSend;
  }

  @Override
  public FissClaimChange next() {
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
        FissClaimChange.newBuilder()
            .setTimestamp(timestamp)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setSeq(generator.getPreviousSequenceNumber())
            .setRdaClaimKey(claim.getRdaClaimKey())
            .setDcn(claim.getDcn())
            .setIntermediaryNb(claim.getIntermediaryNb())
            .setClaim(claim)
            .setSource(source)
            .build();
    return change;
  }

  @Override
  public ClaimSequenceNumberRange getSequenceNumberRange() {
    return ClaimSequenceNumberRange.newBuilder().setLower(0).setUpper(maxToSend).build();
  }

  @Override
  public void close() {}
}
