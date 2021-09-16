package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.util.NoSuchElementException;
import org.junit.Test;

public class RandomFissClaimSourceTest {
  @Test
  public void zeroMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 0);
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void oneMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 1);
    assertEquals(true, source.hasNext());
    FissClaim claim = source.next();
    assertEquals("9086422", claim.getDcn());
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void threeMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 3);
    assertEquals(true, source.hasNext());
    FissClaim claim = source.next();
    assertEquals("9086422", claim.getDcn());

    assertEquals(true, source.hasNext());
    claim = source.next();
    assertTrue(claim.getDcn().length() > 0);

    assertEquals(true, source.hasNext());
    claim = source.next();
    assertTrue(claim.getDcn().length() > 0);

    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void sequenceNumbers() throws Exception {
    MessageSource<FissClaimChange> source =
        new RandomFissClaimSource(0, 7).toClaimChanges().skip(4);
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertEquals(6L, source.next().getSeq());
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
