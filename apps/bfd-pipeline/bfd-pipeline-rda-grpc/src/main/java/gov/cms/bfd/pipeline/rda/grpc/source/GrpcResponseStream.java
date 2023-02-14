package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import io.grpc.ClientCall;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Iterator;

/**
 * Wrapper around a gRPC blocking iterator and ClientCall that allows the client to traverse the
 * response stream in a blocking Iterator-like manner and also to cancel the call/stream at any
 * time. InterruptedExceptions thrown by the underlying iterator are extracted and rethrown in
 * StreamInterruptedExceptions so that they can be handled in a more natural way by the caller.
 *
 * @param <TResponse> the type of objects returned by the RPC
 */
public class GrpcResponseStream<TResponse> {
  /**
   * When the RDA API drops its connection during idle time an exception is thrown by the gRPC
   * runtime that includes with {@link Status#getCode()} returning {@link Status#INTERNAL} and this
   * string as a {@link Status#getDescription()}.
   */
  public static final String STREAM_RESET_ERROR_MESSAGE =
      "RST_STREAM closed stream. HTTP/2 error code: PROTOCOL_ERROR";

  /** The ClientCall used to invoke the RPC associated with the iterator. */
  private final ClientCall<?, ?> clientCall;
  /** An Iterator over the response stream. */
  private final Iterator<TResponse> resultsIterator;

  /**
   * Constructs a GrpcResponseStream object using the specified ClientCall and Iterator. The
   * iterator could be the same one returned by the RPC or it could be a wrapper that transforms the
   * objects into some other type as long as any exceptions thrown by the stream's iterator are
   * passed through unchanged.
   *
   * @param clientCall the ClientCall used to invoke the RPC associated with the iterator
   * @param resultsIterator an Iterator over the response stream
   */
  public GrpcResponseStream(ClientCall<?, ?> clientCall, Iterator<TResponse> resultsIterator) {
    this.clientCall = clientCall;
    this.resultsIterator = resultsIterator;
  }

  /**
   * Determine if another object is available in the stream. Waits for an object to arrive before
   * returning a result. May pass through any runtime exceptions thrown by the stream.
   *
   * @return true if and only if there is an object ready to be processed by next()
   * @throws StreamInterruptedException if the stream threw an InterruptedException
   * @throws StatusRuntimeException if the stream threw an exception
   */
  public boolean hasNext()
      throws StreamInterruptedException, DroppedConnectionException, StatusRuntimeException {
    try {
      return resultsIterator.hasNext();
    } catch (StatusRuntimeException ex) {
      if (ProcessingException.isInterrupted(ex)) {
        throw new StreamInterruptedException(ex);
      } else if (isStreamResetException(ex)) {
        throw new DroppedConnectionException(ex);
      } else {
        throw ex;
      }
    }
  }

  /**
   * Return the next available object from the stream. Callers must receive a true result from
   * hasNext() before calling this method. May pass through any runtime exceptions thrown by the
   * stream.
   *
   * @return the next available object from the stream
   * @throws StreamInterruptedException if the stream threw an InterruptedException
   * @throws StatusRuntimeException if the stream threw an exception
   */
  public TResponse next()
      throws StreamInterruptedException, DroppedConnectionException, StatusRuntimeException {
    try {
      return resultsIterator.next();
    } catch (StatusRuntimeException ex) {
      if (ProcessingException.isInterrupted(ex)) {
        throw new StreamInterruptedException(ex);
      } else if (isStreamResetException(ex)) {
        throw new DroppedConnectionException(ex);
      } else {
        throw ex;
      }
    }
  }

  /**
   * Cancels the RPC call with the specified reason message. This stops processing of the stream and
   * informs the server of the cancellation.
   *
   * @param reason a description of why the stream is being cancelled
   */
  public void cancelStream(String reason) {
    // the null cause is safe because the gRPC considers it optional
    clientCall.cancel(reason, null);
  }

  /**
   * Determines if the provided {@link StatusRuntimeException} represents a dropped connection.
   *
   * @param exception a {@link StatusRuntimeException} caught while reading from the gRPC connection
   * @return true if the error represents a dropped connection
   */
  @VisibleForTesting
  static boolean isStreamResetException(StatusRuntimeException exception) {
    return exception.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED
        || (exception.getStatus().getCode() == Status.Code.INTERNAL
            && STREAM_RESET_ERROR_MESSAGE.equals(exception.getStatus().getDescription()));
  }

  /**
   * Unfortunately InterruptedException does not accept a cause in its constructor so this wrapper
   * allows us to preserve the original StatusRuntimeException while making it easy for a caller to
   * detect when an InterruptedException was thrown down stream.
   */
  public static class StreamInterruptedException extends Exception {
    /**
     * Instantiates a new stream interrupted exception.
     *
     * @param cause the cause for the exception
     */
    public StreamInterruptedException(StatusRuntimeException cause) {
      super(cause);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized StatusRuntimeException getCause() {
      return (StatusRuntimeException) super.getCause();
    }
  }

  /**
   * Wrapper for a {@code StatusRuntimeException} that was thrown in response to a stream reset
   * caused by the server dropping its HTTP connection unexpectedly.
   */
  public static class DroppedConnectionException extends Exception {
    /**
     * Instantiates a new dropped connection exception.
     *
     * @param cause the cause for the exception
     */
    public DroppedConnectionException(StatusRuntimeException cause) {
      super(cause);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized StatusRuntimeException getCause() {
      return (StatusRuntimeException) super.getCause();
    }
  }
}
