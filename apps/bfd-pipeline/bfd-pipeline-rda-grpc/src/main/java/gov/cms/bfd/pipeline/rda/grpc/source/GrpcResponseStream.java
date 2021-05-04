package gov.cms.bfd.pipeline.rda.grpc.source;

import io.grpc.ClientCall;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;

/**
 * Wrapper around a gRPC blocking iterator and ClientCall that allows the client to traverse the
 * response stream in a blocking Iterator-like manner and also to cancel the call at any time.
 * InterruptedExceptions thrown by the gRPC call are extracted and rethrown so that they can be
 * handled in a more natural way by the caller.
 *
 * @param <TResponse> the type of objects returned by the RPC
 */
public class GrpcResponseStream<TResponse> {
  private final ClientCall<?, TResponse> clientCall;
  private final Iterator<TResponse> resultsIterator;

  public GrpcResponseStream(
      ClientCall<?, TResponse> clientCall, Iterator<TResponse> resultsIterator) {
    this.clientCall = clientCall;
    this.resultsIterator = resultsIterator;
  }

  boolean hasNext() throws StreamInterruptedException {
    try {
      return resultsIterator.hasNext();
    } catch (StatusRuntimeException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof InterruptedException) {
        throw new StreamInterruptedException(ex);
      } else {
        throw ex;
      }
    }
  }

  TResponse next() throws StreamInterruptedException {
    try {
      return resultsIterator.next();
    } catch (StatusRuntimeException ex) {
      if (ex.getCause() != null && ex.getCause() instanceof InterruptedException) {
        throw new StreamInterruptedException(ex);
      } else {
        throw ex;
      }
    }
  }

  void cancelStream(String reason) {
    clientCall.cancel(reason, null);
  }

  /**
   * Unfortunately InterruptedException does not accept a cause in its constructor so this wrapper
   * allows us to preserve the original StatusRuntimeException while making it easy for a caller to
   * detect when an InterruptedException was thrown down stream.
   */
  public static class StreamInterruptedException extends Exception {
    public StreamInterruptedException(StatusRuntimeException cause) {
      super(cause);
    }

    @Override
    public synchronized StatusRuntimeException getCause() {
      return (StatusRuntimeException) super.getCause();
    }
  }
}
