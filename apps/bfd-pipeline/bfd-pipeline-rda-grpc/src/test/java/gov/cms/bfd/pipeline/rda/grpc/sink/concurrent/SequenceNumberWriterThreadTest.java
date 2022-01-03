package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.rda.grpc.RdaSink;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SequenceNumberWriterThreadTest {
  @Mock private RdaSink<Integer, Integer> sink;
  private List<Exception> errors;
  private SequenceNumberWriterThread<Integer, Integer> thread;

  @Before
  public void setUp() throws Exception {
    errors = new ArrayList<>();
    thread = new SequenceNumberWriterThread<>(() -> sink, batch -> errors.add(batch.getError()));
  }

  @Test
  public void queueIsEmpty() throws Exception {
    var running = thread.runOnce(sink);
    assertEquals(true, running);
    verifyNoInteractions(sink);
  }

  @Test
  public void closeRequested() throws Exception {
    thread.close();
    var running = thread.runOnce(sink);
    assertEquals(false, running);
    verifyNoInteractions(sink);
  }

  @Test
  public void singleValueInQueue() throws Exception {
    thread.add(1000L);
    var running = thread.runOnce(sink);
    assertEquals(true, running);
    verify(sink).updateLastSequenceNumber(1000);
    verifyNoMoreInteractions(sink);
  }

  @Test
  public void multipleValuesInQueue() throws Exception {
    thread.add(1000L);
    thread.add(1001L);
    thread.add(1002L);
    var running = thread.runOnce(sink);
    assertEquals(true, running);
    verify(sink).updateLastSequenceNumber(1002);
    verifyNoMoreInteractions(sink);
  }

  @Test
  public void shutdownHappensAfterUpdates() throws Exception {
    thread.add(1000L);
    thread.add(1001L);
    thread.add(1002L);
    var running = thread.runOnce(sink);
    assertEquals(true, running);
    verify(sink).updateLastSequenceNumber(1002);

    thread.close();
    running = thread.runOnce(sink);
    assertEquals(false, running);
    verifyNoMoreInteractions(sink);
  }

  @Test
  public void errorsReportedToCaller() throws Exception {
    final var error = new RuntimeException("oops");
    doThrow(error).when(sink).updateLastSequenceNumber(anyLong());
    thread.add(1000L);

    var running = thread.runOnce(sink);
    assertEquals(false, running);
    verify(sink).updateLastSequenceNumber(1000L);
    verifyNoMoreInteractions(sink);
    assertEquals(ImmutableList.of(error), errors);
  }
}
