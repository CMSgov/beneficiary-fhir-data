package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests the {@link RandomFissClaimGenerator} can correctly generate claims. */
public class RandomFissClaimSourceTest {

  /**
   * Validate that when the max number to send is 0, no claims are loaded into the source.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void zeroMaxToReturn() throws Exception {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(0).build();
    final var source = new RandomFissClaimSource(config);
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  /**
   * Validate that when the max number to send is 1, one claim is loaded into the source and that
   * claim has data.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void oneMaxToReturn() throws Exception {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(1).build();
    final var source = new RandomFissClaimSource(config);
    assertTrue(source.hasNext());
    FissClaimChange change = source.next();
    FissClaim claim = change.getClaim();
    assertTrue(claim.getDcn().length() > 0);
    assertEquals(change.getDcn(), claim.getDcn());
    assertEquals(change.getIntermediaryNb(), claim.getIntermediaryNb());
    assertEquals(change.getRdaClaimKey(), claim.getRdaClaimKey());
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  /**
   * Validate that when the max number to send is 3, three claims are loaded into the source and
   * attempting to go beyond that throws an exception.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void threeMaxToReturn() throws Exception {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(3).build();
    final var source = new RandomFissClaimSource(config);
    assertTrue(source.hasNext());
    FissClaimChange change = source.next();
    FissClaim claim = change.getClaim();
    assertTrue(claim.getDcn().length() > 0);
    assertEquals(change.getDcn(), claim.getDcn());
    assertEquals(change.getIntermediaryNb(), claim.getIntermediaryNb());
    assertEquals(change.getRdaClaimKey(), claim.getRdaClaimKey());

    assertTrue(source.hasNext());
    change = source.next();
    claim = change.getClaim();
    assertTrue(claim.getDcn().length() > 0);
    assertEquals(change.getDcn(), claim.getDcn());
    assertEquals(change.getIntermediaryNb(), claim.getIntermediaryNb());
    assertEquals(change.getRdaClaimKey(), claim.getRdaClaimKey());

    assertTrue(source.hasNext());
    change = source.next();
    claim = change.getClaim();
    assertTrue(claim.getDcn().length() > 0);
    assertEquals(change.getDcn(), claim.getDcn());
    assertEquals(change.getIntermediaryNb(), claim.getIntermediaryNb());
    assertEquals(change.getRdaClaimKey(), claim.getRdaClaimKey());

    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  /** Validates that the sequence numbers generated for claims are sequential and start at 1. */
  @Test
  public void sequenceNumbers() {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(7).build();
    final var source = new RandomFissClaimSource(config);
    assertEquals(1L, source.next().getSeq());
    source.skipTo(4);
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertEquals(6L, source.next().getSeq());
    assertEquals(7L, source.next().getSeq());
    assertFalse(source.hasNext());
  }

  /**
   * Validate that there is no next item in the source and attempting to go beyond this point throws
   * an exception.
   *
   * @param source the source
   * @throws Exception the exception
   */
  private void assertNextPastEndOfDataThrowsException(MessageSource<?> source) throws Exception {
    try {
      source.next();
      fail("expected exception");
    } catch (NoSuchElementException ignored) {
      // expected
    }
    // ensures calling hasNext() multiple times past the end is safe
    assertFalse(source.hasNext());
    assertFalse(source.hasNext());
  }
}
