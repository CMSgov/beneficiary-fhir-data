package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import java.time.Clock;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;

public class WrappedClaimSourceTest {
  @Test
  public void emptySource() throws Exception {
    final MessageSource<FissClaim> realSource = new EmptyMessageSource<>();
    final MessageSource<FissClaimChange> wrapped =
        WrappedClaimSource.wrapFissClaims(realSource, Clock.systemUTC(), 1000L);
    assertEquals(false, wrapped.hasNext());
  }

  @Test
  public void allClaims() throws Exception {
    final List<FissClaim> claims =
        ImmutableList.of(
            FissClaim.newBuilder().setDcn("a").build(),
            FissClaim.newBuilder().setDcn("b").build(),
            FissClaim.newBuilder().setDcn("c").build());
    final MessageSource<FissClaim> realSource = fromList(claims);
    final MessageSource<FissClaimChange> wrapped =
        WrappedClaimSource.wrapFissClaims(realSource, Clock.systemUTC(), 1000L);

    for (int index = 0; index < claims.size(); ++index) {
      assertEquals(true, wrapped.hasNext());
      FissClaimChange change = wrapped.next();
      assertEquals(1000L + index, change.getSeq());
      assertSame(claims.get(index), change.getClaim());
    }
    assertEquals(false, wrapped.hasNext());
  }

  @Test
  public void skipStartingClaims() throws Exception {
    final List<McsClaim> claims =
        ImmutableList.of(
            McsClaim.newBuilder().setIdrHic("a").build(),
            McsClaim.newBuilder().setIdrHic("b").build(),
            McsClaim.newBuilder().setIdrHic("c").build(),
            McsClaim.newBuilder().setIdrHic("d").build(),
            McsClaim.newBuilder().setIdrHic("e").build(),
            McsClaim.newBuilder().setIdrHic("f").build(),
            McsClaim.newBuilder().setIdrHic("g").build());
    final MessageSource<McsClaim> realSource = fromList(claims);
    final MessageSource<McsClaimChange> wrapped =
        WrappedClaimSource.wrapMcsClaims(realSource, Clock.systemUTC(), 1000L).skip(3);

    for (int index = 3; index < claims.size(); ++index) {
      assertEquals(true, wrapped.hasNext());
      McsClaimChange change = wrapped.next();
      assertEquals(1000L + index, change.getSeq());
      assertSame(claims.get(index), change.getClaim());
    }
    assertEquals(false, wrapped.hasNext());
  }

  @Test
  public void testChangeType() throws Exception {
    final List<McsClaim> claims =
        ImmutableList.of(
            McsClaim.newBuilder().setIdrHic("a").build(),
            McsClaim.newBuilder().setIdrHic("b").build(),
            McsClaim.newBuilder().setIdrHic("c").build(),
            McsClaim.newBuilder().setIdrHic("a").build(),
            McsClaim.newBuilder().setIdrHic("e").build(),
            McsClaim.newBuilder().setIdrHic("f").build(),
            McsClaim.newBuilder().setIdrHic("g").build(),
            McsClaim.newBuilder().setIdrHic("a").build());
    final MessageSource<McsClaim> realSource = fromList(claims);
    final MessageSource<McsClaimChange> wrapped =
        new WrappedClaimSource<>(
            realSource,
            Clock.systemUTC(),
            1000L,
            3, // set a cache size of 3 so we can verify updates are reported correctly
            String::valueOf,
            (timestamp, type, seq, claim) ->
                McsClaimChange.newBuilder()
                    .setTimestamp(timestamp)
                    .setChangeType(type)
                    .setSeq(seq)
                    .setClaim(claim)
                    .build());
    assertEquals(true, wrapped.hasNext());
    McsClaimChange change = wrapped.next();
    assertEquals("a", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("b", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("c", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    // a is not an UPDATE since it's in the cache
    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("a", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_UPDATE, change.getChangeType());

    // reading three more will cause a to fall out of the cache
    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("e", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("f", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("g", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());

    // now a is an INSERT again since it's not cached
    assertEquals(true, wrapped.hasNext());
    change = wrapped.next();
    assertEquals("a", change.getClaim().getIdrHic());
    assertEquals(ChangeType.CHANGE_TYPE_INSERT, change.getChangeType());
  }

  private static <T> MessageSource<T> fromList(List<T> claims) {
    return new MessageSource<T>() {
      final Iterator<T> iterator = claims.iterator();

      @Override
      public boolean hasNext() throws Exception {
        return iterator.hasNext();
      }

      @Override
      public T next() throws Exception {
        return iterator.next();
      }

      @Override
      public void close() throws Exception {}
    };
  }
}
