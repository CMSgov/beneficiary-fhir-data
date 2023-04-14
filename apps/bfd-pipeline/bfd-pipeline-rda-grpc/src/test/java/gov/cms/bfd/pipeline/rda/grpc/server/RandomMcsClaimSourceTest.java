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
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 0);
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
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 1);
    assertTrue(source.hasNext());
    McsClaim claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
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
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 3);
    assertTrue(source.hasNext());
    McsClaim claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);

    assertTrue(source.hasNext());
    claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);

    assertTrue(source.hasNext());
    claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);

    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  /**
   * Validates that the sequence numbers generated for claims is sequential.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void sequenceNumbers() throws Exception {
    MessageSource<McsClaimChange> source =
        new RandomMcsClaimSource(0, 6).toClaimChanges().skipTo(3);
    assertEquals(3L, source.next().getSeq());
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertFalse(source.hasNext());
  }

  /**
   * Validate that there is no next item in the source and attempting to go beyond this point throws
   * an exception.
   *
   * @param source the source
   * @throws Exception the exception
   */
  private void assertNextPastEndOfDataThrowsException(MessageSource source) throws Exception {
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
