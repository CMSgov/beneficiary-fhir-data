package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.DroppedConnectionException;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.StreamInterruptedException;
import io.grpc.ClientCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link GrpcResponseStream}. */
@ExtendWith(MockitoExtension.class)
public class GrpcResponseStreamTest {
  /** Mock iterator used by the response stream. */
  @Mock private Iterator<Integer> iterator;
  /** Mock client call used by the response stream. */
  @Mock private ClientCall<Integer, Integer> clientCall;
  /** The response stream under test. */
  private GrpcResponseStream<Integer> stream;

  /** Sets up the test stream and mocks. */
  @BeforeEach
  public void setUp() {
    stream = new GrpcResponseStream<>(clientCall, iterator);
  }

  /** Closes the stream created by {@link #setUp}. */
  @AfterEach
  void tearDown() {
    stream.close();
  }

  /** Verify that {@link GrpcResponseStream#hasNext()} passes through non-interrupt exceptions. */
  @Test
  public void testThatHasNextPassesThroughNonInterrupts() {
    StatusRuntimeException status = Status.INVALID_ARGUMENT.asRuntimeException();
    doThrow(status).when(iterator).hasNext();
    try {
      stream.hasNext();
      fail("exception should have been thrown");
    } catch (Throwable ex) {
      assertSame(status, ex);
    }
  }

  /** Verify that {@link GrpcResponseStream#next()} passes through non-interrupt exceptions. */
  @Test
  public void testThatNextPassesThroughNonInterrupts() {
    StatusRuntimeException status = Status.INTERNAL.asRuntimeException();
    doThrow(status).when(iterator).next();
    try {
      stream.next();
      fail("exception should have been thrown");
    } catch (Throwable ex) {
      assertSame(status, ex);
    }
  }

  /**
   * Verify that {@link GrpcResponseStream#hasNext()} wraps {@link InterruptedException}s.
   *
   * @throws DroppedConnectionException pass through checked exception
   */
  @Test
  public void testThatHasNextWrapsInterrupts() throws DroppedConnectionException {
    StatusRuntimeException status =
        Status.CANCELLED.withCause(new InterruptedException()).asRuntimeException();
    doThrow(status).when(iterator).hasNext();
    try {
      stream.hasNext();
      fail("exception should have been thrown");
    } catch (StreamInterruptedException ex) {
      assertSame(status, ex.getCause());
    }
  }

  /**
   * Verify that {@link GrpcResponseStream#next()} wraps {@link InterruptedException}s.
   *
   * @throws DroppedConnectionException pass through checked exception
   */
  @Test
  public void testThatNextWrapsInterrupts() throws DroppedConnectionException {
    StatusRuntimeException status =
        Status.CANCELLED.withCause(new InterruptedException()).asRuntimeException();
    doThrow(status).when(iterator).next();
    try {
      stream.next();
      fail("exception should have been thrown");
    } catch (StreamInterruptedException ex) {
      assertSame(status, ex.getCause());
    }
  }

  /**
   * Verify that {@link GrpcResponseStream#hasNext()} wraps exceptions that indicate a supported
   * type of dropped connection.
   *
   * @throws StreamInterruptedException pass through checked exception
   */
  @Test
  public void testThatHasNextWrapsDroppedConnections() throws StreamInterruptedException {
    StatusRuntimeException status =
        new StatusRuntimeException(
            Status.INTERNAL.withDescription(GrpcResponseStream.STREAM_RESET_ERROR_MESSAGE));
    doThrow(status).when(iterator).hasNext();
    try {
      stream.hasNext();
      fail("exception should have been thrown");
    } catch (DroppedConnectionException ex) {
      assertSame(status, ex.getCause());
    }
  }

  /**
   * Verify that {@link GrpcResponseStream#hasNext()} wraps exceptions that indicate a supported
   * type of dropped connection.
   *
   * @throws StreamInterruptedException pass through checked exception
   */
  @Test
  public void testThatNextWrapsDroppedConnections() throws StreamInterruptedException {
    StatusRuntimeException status = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);
    doThrow(status).when(iterator).next();
    try {
      stream.next();
      fail("exception should have been thrown");
    } catch (DroppedConnectionException ex) {
      assertSame(status, ex.getCause());
    }
  }

  /**
   * Verify that criteria for recognizing need to throw {@link
   * gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.DroppedConnectionException} works
   * properly.
   */
  @Test
  public void testThatDroppedConnectionCriteriaWorkCorrectly() {
    var exception =
        new StatusRuntimeException(Status.INTERNAL.withDescription("some other message"));
    assertFalse(GrpcResponseStream.isStreamResetException(exception));

    exception =
        new StatusRuntimeException(
            Status.INTERNAL.withDescription(GrpcResponseStream.STREAM_RESET_ERROR_MESSAGE));
    assertTrue(GrpcResponseStream.isStreamResetException(exception));

    exception = new StatusRuntimeException(Status.DEADLINE_EXCEEDED);
    assertTrue(GrpcResponseStream.isStreamResetException(exception));
  }

  /**
   * Verify that close cancels an unfinished stream. Also that calling close multiple times does
   * nothing after the first.
   *
   * @throws Exception pass through from method calls
   */
  @Test
  public void testThatCloseCancelsIncompleteStream() throws Exception {
    doReturn(true).when(iterator).hasNext();
    stream.hasNext();
    stream.close();
    stream.close();
    stream.close();
    verify(clientCall, times(1)).cancel(anyString(), isNull());
  }

  /**
   * Verify that close does nothing for a completed stream.
   *
   * @throws Exception pass through from method calls
   */
  @Test
  public void testThatCloseIgnoresCompletedStream() throws Exception {
    doReturn(false).when(iterator).hasNext();
    stream.hasNext();
    stream.close();
    stream.close();
    stream.close();
    verify(clientCall, times(0)).cancel(anyString(), isNull());
  }

  /**
   * Verify that close does nothing for a stream that threw an exception during {#link {@link
   * GrpcResponseStream#next}}.
   *
   * @throws Exception pass through from method calls
   */
  @Test
  public void testThatCloseIgnoresFailedStreamOnHasNext() throws Exception {
    doThrow(new StatusRuntimeException(Status.ABORTED)).when(iterator).hasNext();
    assertThrows(StatusRuntimeException.class, () -> stream.hasNext());
    stream.close();
    stream.close();
    stream.close();
    verify(clientCall, times(0)).cancel(anyString(), isNull());
  }

  /**
   * Verify that close does nothing for a stream that threw an exception during {#link {@link
   * GrpcResponseStream#next}}.
   *
   * @throws Exception pass through from method calls
   */
  @Test
  public void testThatCloseIgnoresFailedStreamOnNext() throws Exception {
    doThrow(new StatusRuntimeException(Status.ABORTED)).when(iterator).next();
    assertThrows(StatusRuntimeException.class, () -> stream.next());
    stream.close();
    stream.close();
    stream.close();
    verify(clientCall, times(0)).cancel(anyString(), isNull());
  }
}
