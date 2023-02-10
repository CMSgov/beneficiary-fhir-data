package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests the {@link SequenceNumberWriterThread}. */
public class SequenceNumberWriterThreadTest {
  /** The mock sink to use for the writer. */
  @Mock private RdaSink<Integer, Integer> sink;
  /** The errors recorded during the test. */
  private List<Exception> errors;
  /** The thread under test. */
  private SequenceNumberWriterThread<Integer, Integer> thread;

  /**
   * Sets up mocks and the writer thread before each test.
   *
   * @throws Exception if there is an issue setting up the test
   */
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    errors = new ArrayList<>();
    thread = new SequenceNumberWriterThread<>(() -> sink, batch -> errors.add(batch.getError()));
  }

  /**
   * Verifies if the thread is not set up with any sequence numbers, the thread will run but the
   * sink will never be called.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void queueIsEmpty() throws Exception {
    var running = thread.runOnce(sink);
    assertTrue(running);
    verifyNoInteractions(sink);
  }

  /**
   * Verifies if the thread is closed before it's run, it will return as not running and the sink
   * will never be called.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void closeRequested() throws Exception {
    thread.close();
    var running = thread.runOnce(sink);
    assertFalse(running);
    verifyNoInteractions(sink);
  }

  /**
   * Verifies if the sink is set up with a sequence number, the thread will run and the sink will
   * update the sequence number.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void singleValueInQueue() throws Exception {
    thread.add(1000L);
    var running = thread.runOnce(sink);
    assertTrue(running);
    verify(sink).updateLastSequenceNumber(1000);
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies if the sink is set up with multiple sequential sequence numbers, the thread will run
   * and the sink will update with the largest sequence number.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void multipleValuesInQueue() throws Exception {
    thread.add(1000L);
    thread.add(1001L);
    thread.add(1002L);
    var running = thread.runOnce(sink);
    assertTrue(running);
    verify(sink).updateLastSequenceNumber(1002);
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies if the thread is run with multiple sequential sequence numbers and then closed, the
   * thread reports that it is no longer running and nothing further occurs with the sink.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void shutdownHappensAfterUpdates() throws Exception {
    thread.add(1000L);
    thread.add(1001L);
    thread.add(1002L);
    var running = thread.runOnce(sink);
    assertTrue(running);
    verify(sink).updateLastSequenceNumber(1002);

    thread.close();
    running = thread.runOnce(sink);
    assertFalse(running);
    verifyNoMoreInteractions(sink);
  }

  /**
   * Verifies if the thread encounters an error, it is caught and written to its internal list of
   * errors.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void errorsReportedToCaller() throws Exception {
    final var error = new RuntimeException("oops");
    doThrow(error).when(sink).updateLastSequenceNumber(anyLong());
    thread.add(1000L);

    var running = thread.runOnce(sink);
    assertFalse(running);
    verify(sink).updateLastSequenceNumber(1000L);
    verifyNoMoreInteractions(sink);
    assertEquals(ImmutableList.of(error), errors);
  }
}
