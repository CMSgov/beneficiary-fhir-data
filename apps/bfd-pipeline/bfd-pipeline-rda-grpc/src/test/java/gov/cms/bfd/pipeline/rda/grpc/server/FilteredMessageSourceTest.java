package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

public class FilteredMessageSourceTest {
  @Test
  public void filtering() throws Exception {
    MessageSource<Integer> source = createSource();
    MessageSource<Integer> filtered = new FilteredMessageSource<>(source, i -> i != 2);
    assertTrue(filtered.hasNext());
    assertEquals(Integer.valueOf(1), filtered.next());
    assertTrue(filtered.hasNext());
    assertEquals(Integer.valueOf(3), filtered.next());
    assertFalse(filtered.hasNext());
  }

  @Test
  public void closePassesThrough() throws Exception {
    MessageSource<Integer> source = createSource();
    MessageSource<Integer> filtered = new FilteredMessageSource<>(source, i -> i != 2);
    filtered.close();
    verify(source).close();
  }

  @Test
  public void multipleHasNextOk() throws Exception {
    MessageSource<Integer> source = createSource();
    MessageSource<Integer> filtered = new FilteredMessageSource<>(source, i -> i < 10);
    assertTrue(filtered.hasNext());
    assertTrue(filtered.hasNext());
    assertTrue(filtered.hasNext());
    assertEquals(Integer.valueOf(1), filtered.next());

    assertTrue(filtered.hasNext());
    assertTrue(filtered.hasNext());
    assertTrue(filtered.hasNext());
    verify(source, times(2)).hasNext();
  }

  @Test
  public void nextWithoutHasNextFails() throws Exception {
    MessageSource<Integer> source = createSource();
    assertThrows(
        NoSuchElementException.class,
        () -> {
          MessageSource<Integer> filtered = new FilteredMessageSource<>(source, i -> i < 10);
          filtered.next();
        });
  }

  @Test
  public void nextPastTheEndFails() throws Exception {
    MessageSource<Integer> source = createSource();
    assertThrows(
        NoSuchElementException.class,
        () -> {
          MessageSource<Integer> filtered =
              new FilteredMessageSource<>(source, i -> i < 10).skip(10);
          assertFalse(filtered.hasNext());
          filtered.next();
        });
  }

  private MessageSource<Integer> createSource() throws Exception {
    MessageSource<Integer> answer = mock(MessageSource.class);
    doReturn(true).doReturn(true).doReturn(true).doReturn(false).when(answer).hasNext();
    doReturn(1)
        .doReturn(2)
        .doReturn(3)
        .doThrow(new RuntimeException("past the end"))
        .when(answer)
        .next();
    return answer;
  }
}
