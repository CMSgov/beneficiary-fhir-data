package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import gov.cms.bfd.pipeline.rda.grpc.sink.concurrent.ReportingCallback.ProcessedBatch;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ClaimWriterThreadTest {
  private static final String VERSION = "Version";

  private TestDatabase database;
  private RdaSink<TestDatabase.Message, TestDatabase.Claim> sink;
  private List<ProcessedBatch<TestDatabase.Message>> callbacks;
  private ClaimWriterThread<TestDatabase.Message, TestDatabase.Claim> thread;
  private ClaimWriterThread.Buffer<TestDatabase.Message, TestDatabase.Claim> buffer;

  @Before
  public void setUp() throws Exception {
    database = new TestDatabase();
    sink = spy(database.createSink());
    callbacks = new ArrayList<>();
    thread = new ClaimWriterThread<>(() -> sink, 3, callbacks::add);
    buffer = new ClaimWriterThread.Buffer<>();
  }

  @Test
  public void queueIsEmpty() throws Exception {
    var running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(Collections.emptyList(), callbacks);
    assertEquals(Collections.emptyList(), database.getClaims());
    assertEquals(0, buffer.getFullCount());
  }

  @Test
  public void incompleteBatch() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    final var claimA2 = new TestDatabase.Claim("a", "b2", 3, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.add(VERSION, claimA2.toMessage());
    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds claimA2 which replaces claimA1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(
        ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimA2.toMessage()),
        buffer.getMessages());
    assertEquals(ImmutableList.of(claimA2, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);
  }

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
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds claimA2 which replaces claimA1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(
        ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimA2.toMessage()),
        buffer.getMessages());
    assertEquals(ImmutableList.of(claimA2, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // fourth loop finds claimC1 which creates full batch and triggers write
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
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

  @Test
  public void shutdownFlushesBuffer() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());
    thread.close();

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), buffer.getMessages());
    assertEquals(ImmutableList.of(claimA1, claimB1), buffer.getClaims());
    assertEquals(ImmutableList.of(), database.getClaims());
    assertEquals(Collections.emptyList(), callbacks);

    // third loop finds shutdown token so flushes buffer and returns false to end loop
    running = thread.runOnce(sink, buffer);
    assertEquals(false, running);
    assertEquals(ImmutableList.of(), buffer.getMessages());
    assertEquals(ImmutableList.of(), buffer.getClaims());
    assertEquals(ImmutableList.of(claimA1, claimB1), database.getClaims());
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                2, ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), null)),
        callbacks);
  }

  @Test
  public void loopWithNoNewEntriesFlushesBuffer() throws Exception {
    final var claimA1 = new TestDatabase.Claim("a", "a1", 1, VERSION);
    final var claimB1 = new TestDatabase.Claim("b", "b1", 2, VERSION);
    thread.add(VERSION, claimA1.toMessage());
    thread.add(VERSION, claimB1.toMessage());

    // first loop finds claimA1
    var running = thread.runOnce(sink, buffer);
    assertEquals(true, running);

    // second loop finds claimB1
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);

    // third loop finds no new entries and flushes buffer but does not shut down
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    assertEquals(ImmutableList.of(), buffer.getMessages());
    assertEquals(ImmutableList.of(), buffer.getClaims());
    assertEquals(ImmutableList.of(claimA1, claimB1), database.getClaims());
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                2, ImmutableList.of(claimA1.toMessage(), claimB1.toMessage()), null)),
        callbacks);
  }

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
    assertEquals(true, running);
    running = thread.runOnce(sink, buffer);
    assertEquals(true, running);
    running = thread.runOnce(sink, buffer);
    assertEquals(false, running);
    assertEquals(
        ImmutableList.of(
            new ProcessedBatch<>(
                0,
                ImmutableList.of(claimA1.toMessage(), claimB1.toMessage(), claimC1.toMessage()),
                error)),
        callbacks);
  }
}
