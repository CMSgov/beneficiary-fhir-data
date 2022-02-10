package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class RandomMcsClaimSourceTest {
  @Test
  public void zeroMaxToReturn() throws Exception {
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 0);
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void oneMaxToReturn() throws Exception {
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 1);
    assertTrue(source.hasNext());
    McsClaim claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

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

  @Test
  public void sequenceNumbers() throws Exception {
    MessageSource<McsClaimChange> source = new RandomMcsClaimSource(0, 6).toClaimChanges().skip(3);
    assertEquals(3L, source.next().getSeq());
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertFalse(source.hasNext());
  }

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
