package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests for the {@link FilteredMessageSource}. */
public class FilteredMessageSourceTest {

  /**
   * Verifies that a message filter properly filters out a message based on a function.
   *
   * @throws Exception indicates test failure
   */
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

  /**
   * Verifies that the filtered message source properly passes the close command to the primary
   * message source.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void closePassesThrough() throws Exception {
    MessageSource<Integer> source = createSource();
    MessageSource<Integer> filtered = new FilteredMessageSource<>(source, i -> i != 2);
    filtered.close();
    verify(source).close();
  }

  /**
   * Verifies that calling {@link FilteredMessageSource#hasNext()} multiple times does not increment
   * the actual next entry pointer when {@link FilteredMessageSource#next()} is called.
   *
   * @throws Exception indicates test failure
   */
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

  /**
   * Verifies that calling {@link FilteredMessageSource#next()} without first calling {@link
   * FilteredMessageSource#hasNext()} throws a {@link NoSuchElementException}, even when there are
   * next elements.
   *
   * @throws Exception indicates test failure (expected exception is caught)
   */
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

  /**
   * Verifies that when there are no next elements, calling {@link FilteredMessageSource#hasNext()}
   * returns {@code false} and calling {@link FilteredMessageSource#next()} throws a {@link
   * NoSuchElementException}.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void nextPastTheEndFails() throws Exception {
    MessageSource<Integer> source = createSource();
    assertThrows(
        NoSuchElementException.class,
        () -> {
          MessageSource<Integer> filtered =
              new FilteredMessageSource<>(source, i -> i < 10).skipTo(10);
          assertFalse(filtered.hasNext());
          filtered.next();
        });
  }

  /**
   * Creates a mock message source for testing, and sets up the relevant mock returns.
   *
   * @return the configured mock message source
   * @throws Exception if there is a mocking issue
   */
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
