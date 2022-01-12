package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream.StreamInterruptedException;
import io.grpc.ClientCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GrpcResponseStreamTest {
  private Iterator<Integer> iterator;
  private ClientCall<Integer, Integer> clientCall;
  private GrpcResponseStream<Integer> stream;

  @BeforeEach
  public void setUp() {
    iterator = mock(Iterator.class);
    clientCall = mock(ClientCall.class);
    stream = new GrpcResponseStream<>(clientCall, iterator);
  }

  @Test
  public void hasNextPassesThroughNonInterrupts() {
    StatusRuntimeException status = Status.DEADLINE_EXCEEDED.asRuntimeException();
    doThrow(status).when(iterator).hasNext();
    try {
      stream.hasNext();
      fail("exception should have been thrown");
    } catch (Throwable ex) {
      assertSame(status, ex);
    }
  }

  @Test
  public void nextPassesThroughNonInterrupts() {
    StatusRuntimeException status = Status.DEADLINE_EXCEEDED.asRuntimeException();
    doThrow(status).when(iterator).next();
    try {
      stream.next();
      fail("exception should have been thrown");
    } catch (Throwable ex) {
      assertSame(status, ex);
    }
  }

  @Test
  public void hasNextWrapsInterrupts() {
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

  @Test
  public void nextWrapsInterrupts() {
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
}
