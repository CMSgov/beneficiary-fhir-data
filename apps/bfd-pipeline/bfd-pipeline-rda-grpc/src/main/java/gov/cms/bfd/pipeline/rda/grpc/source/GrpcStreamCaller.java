package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.protobuf.Empty;
import gov.cms.bfd.pipeline.rda.grpc.RdaServerJob;
import gov.cms.mpsm.rda.v1.ApiVersion;
import gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCalls;
import java.io.IOException;
import java.util.Iterator;
import org.slf4j.Logger;

/**
 * Abstract base class for objects that call specific RPCs to open a stream of incoming data.
 * Particular implementations deal with issues such as: which RPC to call, how to construct proper
 * request parameters, and (potentially) how to map the incoming response objects into our own
 * internal objects. The latter could be done here or on the RdaSink that ultimately processes the
 * data. No requirement is made on where that mapping is performed.
 *
 * @param <TResponse> Type of objects returned by to the client by this caller. Generally either
 *     FissClaimChange or McsClaimChange in real code
 */
public abstract class GrpcStreamCaller<TResponse> {
  /** Maximum number of call attempts in {@link #callVersionService}. */
  private static final int MAX_CALL_ATTEMPTS = 3;

  /** Delay between retries in {@link #callVersionService}. */
  private static final long CALL_RETRY_DELAY_MILLIS = 15_000;

  /** The passed-in logger. */
  protected final Logger logger;

  /**
   * Instantiates a new grpc stream caller.
   *
   * @param logger the logger to use for this caller
   */
  protected GrpcStreamCaller(Logger logger) {
    this.logger = logger;
  }

  /**
   * Make the call to the service and return a blocking GrpcResponseStream that allows results to be
   * traversed like an Iterator but also allows the stream to be cancelled at any time. The starting
   * sequence number allows the caller to resume the stream at an arbitrary location. The sequence
   * numbers come back from the API server in change objects.
   *
   * @param channel an already open channel to the service being called
   * @param callOptions the CallOptions object to use for the API call
   * @param startingSequenceNumber specifies the sequence number to send to the RDA API server
   * @return a blocking GrpcResponseStream allowing iteration over stream results
   * @throws Exception any exception thrown calling the RPC or setting up the stream
   */
  public abstract GrpcResponseStream<TResponse> callService(
      ManagedChannel channel, CallOptions callOptions, long startingSequenceNumber)
      throws Exception;

  /**
   * Calls the service to get the sequence number range.
   *
   * @param channel an already open channel to the service being called
   * @param callOptions the CallOptions object to use for the API call
   * @return sequence number range
   */
  public abstract ClaimSequenceNumberRange callSequenceNumberRangeService(
      ManagedChannel channel, CallOptions callOptions);

  /**
   * Make a call to the server's {@code getVersion()} service and return the version component. Will
   * retry several times if the call fails. Retries allow the job to handle with a race condition
   * with the start of {@link RdaServerJob} and/or temporary downtime in the real API server.
   *
   * @param channel an already open channel to the server
   * @param callOptions the call options
   * @return version string from the server
   * @throws Exception if an IO issue occurs
   */
  public String callVersionService(ManagedChannel channel, CallOptions callOptions)
      throws Exception {
    Preconditions.checkNotNull(channel);
    RuntimeException error = null;
    for (int tryNumber = 1; tryNumber <= MAX_CALL_ATTEMPTS; ++tryNumber) {
      try {
        var version = callVersionServiceImpl(channel, callOptions);
        if (tryNumber > 1) {
          logger.info("callVersionService successful on attempt {}", tryNumber);
        }
        return version;
      } catch (StatusRuntimeException ex) {
        // if we're not authenticated now we never will be...
        if (ex.getStatus() == Status.UNAUTHENTICATED) {
          throw ex;
        }
        error = ex;
      } catch (RuntimeException ex) {
        error = ex;
      }
      logger.info(
          "callVersionService attempt {} failed with {} message {}.",
          tryNumber,
          error.getClass().getSimpleName(),
          error.getMessage());
      if (tryNumber < MAX_CALL_ATTEMPTS) {
        // wait before next connection attempt
        try {
          logger.info("callVersionService waiting before next attempt");
          Thread.sleep(CALL_RETRY_DELAY_MILLIS);
        } catch (InterruptedException ex) {
          // If we are interrupted while sleeping just exit loop to let original
          // exception be thrown immediately.
          break;
        }
      }
    }
    // all retries failed so throw the last exception
    throw error;
  }

  /**
   * Make a call to the server's {@code getVersion()} service and return the version component.
   *
   * @param channel an already open channel to the server
   * @param callOptions the call options
   * @return version string from the server
   * @throws Exception if an IO issue occurs
   */
  private String callVersionServiceImpl(ManagedChannel channel, CallOptions callOptions)
      throws Exception {
    Preconditions.checkNotNull(channel);
    logger.info("calling getVersion service");
    final MethodDescriptor<Empty, ApiVersion> method = RDAServiceGrpc.getGetVersionMethod();
    final ClientCall<Empty, ApiVersion> call = channel.newCall(method, callOptions);
    final Iterator<ApiVersion> apiResults =
        ClientCalls.blockingServerStreamingCall(call, Empty.getDefaultInstance());
    // Oddly enough the server returns a stream.  Rather than hard code an assumption of one
    // result object this loop ensures we fully consume the stream if multiple objects are
    // received for some reason.
    String answer = "";
    while (apiResults.hasNext()) {
      ApiVersion response = apiResults.next();
      logger.info(
          "getVersion service response: version='{}' commitId='{}' buildTime='{}'",
          response.getVersion(),
          response.getCommitId(),
          response.getBuildTime());
      if (!Strings.isNullOrEmpty(response.getVersion())) {
        if (!answer.isEmpty()) {
          throw new IOException("RDA API Server returned multiple non-empty version strings");
        }
        answer = response.getVersion();
      }
    }
    if (answer.isEmpty()) {
      throw new IOException("RDA API Server did not return a non-empty version string");
    }
    return answer;
  }
}
