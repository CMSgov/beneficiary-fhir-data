package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests the {@link ConcurrentRdaSink}. */
public class ConcurrentRdaSinkTest {
  /** Test value for version. */
  private static final String VERSION = "Version";

  /** Mock write thread pool to pull from for the test. */
  @Mock private WriterThreadPool<TestDatabase.Message, TestDatabase.Claim> pool;
  /** The concurrent sink under test. */
  private ConcurrentRdaSink<TestDatabase.Message, TestDatabase.Claim> sink;

  /**
   * Sets the test mocks and sink up.
   *
   * @throws Exception if there is an issue during test setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    sink = new ConcurrentRdaSink<>(pool);
  }

  /**
   * Verifies that when writing multiple messages, each message is only enqueued to the pool once
   * and the correct total number of messages are processed.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void testWriteMessagesSuccessNoSequenceNumberChanges() throws Exception {
    final TestDatabase.Message messageA1 = new TestDatabase.Message("a", "a1", 1);
    final TestDatabase.Message messageB1 = new TestDatabase.Message("b", "b1", 2);
    final TestDatabase.Message messageA2 = new TestDatabase.Message("a", "a2", 3);
    final TestDatabase.Message messageC1 = new TestDatabase.Message("c", "c1", 4);
    doReturn(0).when(pool).getProcessedCount();

    int count = sink.writeMessages(VERSION, List.of(messageA1, messageB1, messageA2, messageC1));
    assertEquals(0, count);
    verify(pool).addToQueue(VERSION, messageA1);
    verify(pool).addToQueue(VERSION, messageB1);
    verify(pool).addToQueue(VERSION, messageA2);
    verify(pool).addToQueue(VERSION, messageC1);
    verify(pool).getProcessedCount();
    verifyNoMoreInteractions(pool);
  }

  /**
   * Verifies that when writing multiple messages, each message is only enqueued to the pool once
   * and the correct total number of messages are processed and the sequence numbers are updated.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void testWriteMessagesSuccessUpdatesSequenceNumbers() throws Exception {
    final TestDatabase.Message messageA1 = new TestDatabase.Message("a", "a1", 1);
    final TestDatabase.Message messageB1 = new TestDatabase.Message("b", "b1", 2);
    final TestDatabase.Message messageA2 = new TestDatabase.Message("a", "a2", 3);
    final TestDatabase.Message messageC1 = new TestDatabase.Message("c", "c1", 4);
    doReturn(3).when(pool).getProcessedCount();

    int count = sink.writeMessages(VERSION, List.of(messageA1, messageB1, messageA2, messageC1));
    assertEquals(3, count);
    verify(pool).addToQueue(VERSION, messageA1);
    verify(pool).addToQueue(VERSION, messageB1);
    verify(pool).addToQueue(VERSION, messageA2);
    verify(pool).addToQueue(VERSION, messageC1);
    verify(pool).getProcessedCount();
    verify(pool).updateSequenceNumbers();
    verifyNoMoreInteractions(pool);
  }

  /**
   * Verifies that if there is an exception during updating the sequence number, the message is
   * still enqueued to the pool.
   *
   * @throws Exception the exception
   */
  @Test
  public void testWriteMessagesThrows() throws Exception {
    Exception error = new RuntimeException("oops");
    final TestDatabase.Message messageA1 = new TestDatabase.Message("a", "a1", 1);
    doReturn(1).when(pool).getProcessedCount();
    doThrow(error).when(pool).updateSequenceNumbers();

    try {
      sink.writeMessages(VERSION, List.of(messageA1));
      fail("should have thrown an exception");
    } catch (ProcessingException ex) {
      assertEquals(1, ex.getProcessedCount());
      assertSame(error, ex.getOriginalCause());
    }
    verify(pool).addToQueue(VERSION, messageA1);
    verify(pool).getProcessedCount();
    verify(pool).updateSequenceNumbers();
    verifyNoMoreInteractions(pool);
  }
}
