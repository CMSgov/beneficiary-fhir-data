package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ReportingCallback.ProcessedBatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the {@link ClaimWriterThread}. */
public class ClaimWriterThreadTest {
  /** Test value for version. */
  private static final String VERSION = "Version";

  /** The database to use to test the claims were written. */
  private TestDatabase database;
  /** The test sink. */
  private RdaSink<TestDatabase.Message, TestDatabase.Claim> sink;
  /** Keeps track of the callbacks made during the test. */
  private List<ProcessedBatch<TestDatabase.Message>> callbacks;
  /** The main test claim writer thread under test. */
  private ClaimWriterThread<TestDatabase.Message, TestDatabase.Claim> thread;
  /** The buffer to use for the claim writer. */
  private ClaimWriterThread.Buffer<TestDatabase.Message, TestDatabase.Claim> buffer;

  /**
   * Sets up the test resources before each test.
   *
   * @throws Exception if there is an issue with setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    database = new TestDatabase();
    sink = spy(database.createSink());
    callbacks = new ArrayList<>();
    thread = new ClaimWriterThread<>(() -> sink, 3, callbacks::add);
    buffer = new ClaimWriterThread.Buffer<>();
  }

  /**
   * Verifies that the thread properly handles an empty queue during a loop iteration.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void queueIsEmpty() throws Exception {
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(Collections.emptyList(), callbacks);
    assertEquals(Collections.emptyList(), database.getClaims());
    assertEquals(0, buffer.getFullCount());
  }

  /**
   * Verifies that the thread properly updates an existing claim in its buffer if a new one is added
   * with the same key.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void newClaimOverwriteOldClaimWithSameKeyInBuffer() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    final var claimA2 = new TestDatabase.Claim("a", "b2", 3, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.add(VERSION, claimA2.toMessage());
    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds claimA2 which replaces claimA1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(
        ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimA2.toMessage()),
        buffer.getMessages());
    assertEquals(ImmutableList.of(claimA2, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);
  }

  /**
   * Verifies that the thread properly writes to database once it has a full buffer.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void completeBatch() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    final var claimA2 = new TestDatabase.Claim("a", "b2", 3, VERSION);
    final var claimC1 = new TestDatabase.Claim("c", "c1", 4, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.add(VERSION, claimA2.toMessage());
    thread.add(VERSION, claimC1.toMessage());

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds claimA2 which replaces claimA1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(
        ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimA2.toMessage()),
        buffer.getMessages());
    assertEquals(ImmutableList.of(claimA2, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // fourth loop finds claimC1 which creates full batch and triggers write
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(), buffer.getMessages());
    assertEquals(ImmutableList.of(), buffer.getClaims());
    assertEquals(ImmutableList.of(claimA2, claimB1, claimC1), database.getClaims());
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                3,
                ImmutableList.of(
                    claimA1.toMessage(),
                    claimB1.toMessage(),
                    claimA2.toMessage(),
                    claimC1.toMessage()),
                null)),
        callbacks);
  }

  /**
   * Verifies that the thread properly flushes its buffer when shutting down.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void shutdownFlushesBuffer() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.close();

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds shutdown requested so flushes buffer and returns false to end loop
    running = thread.runOnce(sink, buffer);
    assertFalse(running);
    assertEquals(ImmutableList.of(), buffer.getMessages());
    assertEquals(ImmutableList.of(), buffer.getClaims());
    assertEquals(ImmutableList.of(claimA1, claimB1), database.getClaims());
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                2, ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), null)),
        callbacks);
  }

  /**
   * Verifies that the thread properly flushes partial buffer if claims arrive slowly.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void loopWithNoNewEntriesFlushesBuffer() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertTrue(running);

    // third loop finds no new entries and flushes buffer but does not shut down
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    assertEquals(ImmutableList.of(), buffer.getMessages());
    assertEquals(ImmutableList.of(), buffer.getClaims());
    assertEquals(ImmutableList.of(claimA1, claimB1), database.getClaims());
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                2, ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), null)),
        callbacks);
  }

  /**
   * Verifies that the thread properly reports an error through the callback and shuts down.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void writeErrorReportsExceptionAndShutsDown() throws Exception {
    final ProcessingException error = new ProcessingException(new IOException("oops"), 0);
    doThrow(error).when(sink).writeClaims(any());

    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    final var claimC1 = new TestDatabase.Claim("c", "c1", 3, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.add(VERSION, claimC1.toMessage());

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertTrue(running);
    running = thread.runOnce(sink, buffer);
    assertTrue(running);
    running = thread.runOnce(sink, buffer);
    assertFalse(running);
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                0,
                ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimC1.toMessage()),
                error)),
        callbacks);
  }

  /**
   * Tests that queue is drained until close is called.
   *
   * @throws Exception passes through any exceptions thrown during test
   */
  @Test
  public void testDrainQueueUntilStoppedFlagIsSet() throws Exception {
    final AtomicBoolean interrupted = new AtomicBoolean();
    final Thread runningThread =
        new Thread(
            () -> {
              try {
                thread.drainQueueUntilStoppedFlagIsSet();
              } catch (InterruptedException ex) {
                interrupted.set(true);
              }
            });

    // Start the thread and add some claims.
    runningThread.start();
    for (int i = 1; i <= 10; ++i) {
      thread.add(VERSION, new TestDatabase.Claim("a", "a" + i, i, VERSION).toMessage());
      Thread.sleep(25L);
    }

    // Thread should still be running because we have not yet called close().
    assertTrue(runningThread.isAlive());

    // Tell the thread to stop and give it plenty of time to exit.
    thread.close();
    runningThread.join(30_000);

    // Thread should now be stopped and none of the claims should have been written to the database.
    assertFalse(runningThread.isAlive());
    assertFalse(interrupted.get());
    assertTrue(database.getClaims().isEmpty());
  }
}
