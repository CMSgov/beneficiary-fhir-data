package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
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
      public void close() {}
    };
  }
}
