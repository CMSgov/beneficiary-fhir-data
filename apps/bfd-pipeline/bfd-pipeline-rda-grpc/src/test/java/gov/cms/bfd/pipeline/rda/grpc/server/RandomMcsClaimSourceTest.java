package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.util.NoSuchElementException;
import org.junit.Test;

public class RandomMcsClaimSourceTest {
  @Test
  public void zeroMaxToReturn() throws Exception {
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 0);
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void oneMaxToReturn() throws Exception {
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 1);
    assertEquals(true, source.hasNext());
    McsClaim claim = source.next();
    assertEquals("08642205", claim.getIdrClmHdIcn());
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void threeMaxToReturn() throws Exception {
    RandomMcsClaimSource source = new RandomMcsClaimSource(0, 3);
    assertEquals(true, source.hasNext());
    McsClaim claim = source.next();
    assertEquals("08642205", claim.getIdrClmHdIcn());

    assertEquals(true, source.hasNext());
    claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);

    assertEquals(true, source.hasNext());
    claim = source.next();
    assertTrue(claim.getIdrClmHdIcn().length() > 0);

    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void sequenceNumbers() throws Exception {
    MessageSource<McsClaimChange> source = new RandomMcsClaimSource(0, 6).toClaimChanges().skip(3);
    assertEquals(3L, source.next().getSeq());
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertEquals(false, source.hasNext());
  }

  private void assertNextPastEndOfDataThrowsException(MessageSource source) throws Exception {
    try {
      source.next();
      fail("expected exception");
    } catch (NoSuchElementException ignored) {
      // expected
    }
    // ensures calling hasNext() multiple times past the end is safe
    assertEquals(false, source.hasNext());
    assertEquals(false, source.hasNext());
  }
}
