package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ApiVersion;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of RDAServiceImplBase that services FissClaims using a FissClaimSource object. A
 * Supplier is used to create a new FissClaimSource object for each client call to getFissClaims.
 */
public class RdaService extends RDAServiceGrpc.RDAServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaService.class);

  /** The RDA server version to use when serving messages. */
  public static final String RDA_PROTO_VERSION = "0.14.1";

  /** The source of claims for the server. */
  private final RdaMessageSourceFactory messageSourceFactory;

  /**
   * Instantiates a new Rda service.
   *
   * @param messageSourceFactory used to create sources of claims
   */
  public RdaService(RdaMessageSourceFactory messageSourceFactory) {
    this.messageSourceFactory = messageSourceFactory;
  }

  @Override
  public void getVersion(Empty request, StreamObserver<ApiVersion> responseObserver) {
    try {
      LOGGER.info("getVersion called: response={}", messageSourceFactory.getVersion());
      responseObserver.onNext(messageSourceFactory.getVersion().toApiVersion());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
    }
  }

  @Override
  public void getFissClaimsSequenceNumberRange(
      com.google.protobuf.Empty request,
      StreamObserver<gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange> responseObserver) {
    LOGGER.info("start getFissClaimsSequenceNumberRange");
    try (MessageSource<FissClaimChange> source = messageSourceFactory.createFissMessageSource(0)) {
      responseObserver.onNext(source.getSequenceNumberRange());
      responseObserver.onCompleted();
      LOGGER.info("end getFissClaimsSequenceNumberRange");
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
      LOGGER.error(
          "end getFissClaimsSequenceNumberRange call - call failed with exception: message={}",
          ex.getMessage(),
          ex);
    }
  }

  @Override
  public void getMcsClaimsSequenceNumberRange(
      com.google.protobuf.Empty request,
      StreamObserver<gov.cms.mpsm.rda.v1.ClaimSequenceNumberRange> responseObserver) {
    LOGGER.info("start getMcsClaimsSequenceNumberRange");
    try (MessageSource<McsClaimChange> source = messageSourceFactory.createMcsMessageSource(0)) {
      responseObserver.onNext(source.getSequenceNumberRange());
      responseObserver.onCompleted();
      LOGGER.info("end getMcsClaimsSequenceNumberRange");
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
      LOGGER.error(
          "end getMcsClaimsSequenceNumberRange call - call failed with exception: message={}",
          ex.getMessage(),
          ex);
    }
  }

  @Override
  public void getFissClaims(
      ClaimRequest request, StreamObserver<FissClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call with since={}", request.getSince());
    try {
      MessageSource<FissClaimChange> generator =
          messageSourceFactory.createFissMessageSource(request.getSince() + 1);
      Responder<FissClaimChange> responder = createFissResponder(responseObserver, generator);
      responder.start();
      LOGGER.info("end getFissClaims call - stream running in background");
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
      LOGGER.error(
          "end getFissClaims call - call failed with exception: message={}", ex.getMessage(), ex);
    }
  }

  /**
   * Helper method to make mocking in tests easier.
   *
   * @param observer the observer
   * @param source the source
   * @return the responder
   */
  @VisibleForTesting
  Responder<FissClaimChange> createFissResponder(
      StreamObserver<FissClaimChange> observer, MessageSource<FissClaimChange> source) {
    return new Responder<>(observer, source);
  }

  @Override
  public void getMcsClaims(ClaimRequest request, StreamObserver<McsClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call with since={}", request.getSince());
    try {
      MessageSource<McsClaimChange> generator =
          messageSourceFactory.createMcsMessageSource(request.getSince() + 1);
      Responder<McsClaimChange> responder = createMcsResponder(responseObserver, generator);
      responder.start();
      LOGGER.info("end getMcsClaims call - stream running in background");
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
      LOGGER.error(
          "end getMcsClaims call - call failed with exception: message={}", ex.getMessage(), ex);
    }
  }

  /**
   * Helper method to make mocking in tests easier.
   *
   * @param observer the observer
   * @param source the source
   * @return the responder
   */
  @VisibleForTesting
  Responder<McsClaimChange> createMcsResponder(
      StreamObserver<McsClaimChange> observer, MessageSource<McsClaimChange> source) {
    return new Responder<>(observer, source);
  }

  /**
   * Class for returning responses to requests.
   *
   * @param <TChange> the type parameter
   */
  @VisibleForTesting
  static class Responder<TChange> {
    /** Observer for responses. */
    private final ServerCallStreamObserver<TChange> responseObserver;

    /** The message generator. */
    private final MessageSource<TChange> generator;

    /** True if the client has cancelled the connection. */
    private final AtomicBoolean cancelled;

    /**
     * True if we have sent all changes to the client and called {@link StreamObserver#onCompleted}.
     */
    private final AtomicBoolean completed;

    /** Total number of changes we have sent. */
    private final AtomicInteger totalSent;

    /**
     * Initializes object.
     *
     * @param responseObserver the response observer
     * @param generator the message generator
     */
    private Responder(StreamObserver<TChange> responseObserver, MessageSource<TChange> generator) {
      this.generator = generator;
      this.cancelled = new AtomicBoolean(false);
      this.completed = new AtomicBoolean(false);
      this.totalSent = new AtomicInteger(0);
      this.responseObserver = (ServerCallStreamObserver<TChange>) responseObserver;
    }

    /**
     * Initializes callbacks to start sending data when client is ready and receive notification if
     * client cancels.
     */
    void start() {
      responseObserver.setOnCancelHandler(this::onCancelled);
      responseObserver.setOnReadyHandler(this::onReady);
    }

    /** Callback from client indicating that is has cancelled the connection. */
    void onCancelled() {
      cancelled.set(true);
      LOGGER.info("call cancelled by client: total={}", totalSent.get());
      closeGenerator();
    }

    /**
     * Callback from client indicating that it is ready to receive some records. Send as many as
     * they are ready to receive or until generator runs out of data.
     */
    @VisibleForTesting
    void onReady() {
      // docs indicate a race condition can trigger a redundant call
      if (completed.get() || cancelled.get()) {
        return;
      }

      int sent = 0;
      int total = totalSent.get();

      try {
        while (responseObserver.isReady() && generator.hasNext()) {
          var change = generator.next();
          responseObserver.onNext(change);
          sent += 1;
          total = totalSent.incrementAndGet();
        }
        if (generator.hasNext()) {
          LOGGER.debug("pausing: sent={} total={}", sent, total);
        } else {
          completed.set(true);
          responseObserver.onCompleted();
          closeGenerator();
          LOGGER.info("stream complete: sent={} total={}", sent, total);
        }
      } catch (Exception ex) {
        LOGGER.error(
            "caught exception: sent={} total={} message={}", sent, total, ex.getMessage(), ex);
        responseObserver.onError(Status.fromThrowable(ex).asException());
        closeGenerator();
      }
    }

    /** Closes the random claim generator so it can release any resources. */
    void closeGenerator() {
      try {
        generator.close();
      } catch (Exception ex) {
        LOGGER.error("caught exception closing generator: message={}", ex.getMessage(), ex);
      }
    }
  }

  /** Class for the RDA version information. */
  @Value
  @Builder
  public static class Version {
    /** The RDA version. */
    @Builder.Default String version = RDA_PROTO_VERSION;

    /** The commit id. */
    @Builder.Default String commitId = "";

    /** The build time. */
    @Builder.Default String buildTime = "";

    /**
     * Converts this {@link Version} to an {@link ApiVersion}.
     *
     * @return the api version
     */
    public ApiVersion toApiVersion() {
      return ApiVersion.newBuilder()
          .setVersion(version)
          .setCommitId(commitId)
          .setBuildTime(buildTime)
          .build();
    }
  }
}
