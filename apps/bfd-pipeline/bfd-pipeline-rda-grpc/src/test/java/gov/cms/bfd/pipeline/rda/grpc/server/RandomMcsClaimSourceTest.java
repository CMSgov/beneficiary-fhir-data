package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests the {@link RandomMcsClaimSource} can correctly generate claims. */
public class RandomMcsClaimSourceTest {
  /**
   * Validate that when the max number to send is 0, no claims are loaded into the source.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void zeroMaxToReturn() throws Exception {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(0).build();
    final var source = new RandomMcsClaimSource(config);
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
    final var source = new RandomMcsClaimSource(config);
    assertTrue(source.hasNext());
    McsClaimChange change = source.next();
    McsClaim claim = change.getClaim();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
    assertEquals(change.getIcn(), claim.getIdrClmHdIcn());
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
    final var source = new RandomMcsClaimSource(config);
    assertTrue(source.hasNext());
    McsClaimChange change = source.next();
    McsClaim claim = change.getClaim();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
    assertEquals(change.getIcn(), claim.getIdrClmHdIcn());

    assertTrue(source.hasNext());
    change = source.next();
    claim = change.getClaim();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
    assertEquals(change.getIcn(), claim.getIdrClmHdIcn());

    assertTrue(source.hasNext());
    change = source.next();
    claim = change.getClaim();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
    assertEquals(change.getIcn(), claim.getIdrClmHdIcn());

    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  /** Validates that the sequence numbers generated for claims are sequential and start at 1. */
  @Test
  public void sequenceNumbers() {
    final var config = RandomClaimGeneratorConfig.builder().seed(0).maxToSend(8).build();
    final var source = new RandomMcsClaimSource(config);
    assertEquals(1L, source.next().getSeq());
    assertEquals(2L, source.next().getSeq());
    source.skipTo(5);
    assertEquals(5L, source.next().getSeq());
    assertEquals(6L, source.next().getSeq());
    assertEquals(7L, source.next().getSeq());
    assertEquals(8L, source.next().getSeq());
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
