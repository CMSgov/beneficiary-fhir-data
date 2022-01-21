package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class RandomFissClaimSourceTest {
  @Test
  public void zeroMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 0);
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void oneMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 1);
    assertTrue(source.hasNext());
    FissClaim claim = source.next();
    assertEquals("9086422", claim.getDcn());
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void threeMaxToReturn() throws Exception {
    RandomFissClaimSource source = new RandomFissClaimSource(0, 3);
    assertTrue(source.hasNext());
    FissClaim claim = source.next();
    assertEquals("9086422", claim.getDcn());

    assertTrue(source.hasNext());
    claim = source.next();
    assertTrue(claim.getDcn().length() > 0);

    assertTrue(source.hasNext());
    claim = source.next();
    assertTrue(claim.getDcn().length() > 0);

    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
  }

  @Test
  public void sequenceNumbers() throws Exception {
    MessageSource<FissClaimChange> source =
        new RandomFissClaimSource(0, 7).toClaimChanges().skip(4);
    assertEquals(4L, source.next().getSeq());
    assertEquals(5L, source.next().getSeq());
    assertEquals(6L, source.next().getSeq());
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
