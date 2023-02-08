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
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
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
  /** The RDA server version. */
  public static final String RDA_PROTO_VERSION = "0.10";

  /** The configuration for the server. */
  private final Config config;

  /**
   * Instantiates a new Rda service.
   *
   * @param config the config
   */
  public RdaService(Config config) {
    this.config = config;
  }

  /** {@inheritDoc} */
  @Override
  public void getVersion(Empty request, StreamObserver<ApiVersion> responseObserver) {
    try {
      LOGGER.info("getVersion called: response={}", config.getVersion());
      responseObserver.onNext(config.getVersion().toApiVersion());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void getFissClaims(
      ClaimRequest request, StreamObserver<FissClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call with since={}", request.getSince());
    try {
      MessageSource<FissClaimChange> generator =
          config.getFissSourceFactory().apply(request.getSince() + 1);
      Responder<FissClaimChange> responder = createFissResponder(responseObserver, generator);
      responder.sendResponses();
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
    }
    LOGGER.info("end getFissClaims call");
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

  /** {@inheritDoc} */
  @Override
  public void getMcsClaims(ClaimRequest request, StreamObserver<McsClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call with since={}", request.getSince());
    try {
      MessageSource<McsClaimChange> generator =
          config.getMcsSourceFactory().apply(request.getSince() + 1);
      Responder<McsClaimChange> responder = createMcsResponder(responseObserver, generator);
      responder.sendResponses();
    } catch (Exception ex) {
      responseObserver.onError(Status.fromThrowable(ex).asException());
    }
    LOGGER.info("end getMcsClaims call");
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
    /** If the responder is cancelled. */
    private final AtomicBoolean cancelled;
    /** If the responder if running. */
    private final AtomicBoolean running;

    /**
     * Instantiates a new Responder.
     *
     * @param responseObserver the response observer
     * @param generator the message generator
     */
    private Responder(StreamObserver<TChange> responseObserver, MessageSource<TChange> generator) {
      this.generator = generator;
      this.cancelled = new AtomicBoolean(false);
      this.running = new AtomicBoolean(true);
      this.responseObserver = (ServerCallStreamObserver<TChange>) responseObserver;
      this.responseObserver.setOnReadyHandler(this::sendResponses);
      this.responseObserver.setOnCancelHandler(() -> cancelled.set(true));
    }

    /** Sends responses from the {@link #generator}. */
    @VisibleForTesting
    void sendResponses() {
      final var random = new Random();
      if (running.get()) {
        try {
          while (running.get()
              && responseObserver.isReady()
              && !responseObserver.isCancelled()
              && !cancelled.get()
              && generator.hasNext()) {
            responseObserver.onNext(generator.next());
          }
          if (responseObserver.isCancelled() || cancelled.get()) {
            running.set(false);
            responseObserver.onCompleted();
            generator.close();
            LOGGER.info("call cancelled by client");
          } else if (!generator.hasNext()) {
            running.set(false);
            responseObserver.onCompleted();
            generator.close();
            LOGGER.info("call complete");
          }
        } catch (Exception ex) {
          running.set(false);
          LOGGER.error("caught exception: {}", ex.getMessage(), ex);
          responseObserver.onError(Status.fromThrowable(ex).asException());
        }
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

  /** Configuration class for the service. */
  @Value
  @Builder
  public static class Config {
    /** The fiss stream source factory. */
    @Builder.Default
    MessageSource.Factory<FissClaimChange> fissSourceFactory = EmptyMessageSource.factory();
    /** The MCS stream source factory. */
    @Builder.Default
    MessageSource.Factory<McsClaimChange> mcsSourceFactory = EmptyMessageSource.factory();
    /** The version information. */
    @Builder.Default Version version = Version.builder().build();

    /**
     * Creates a new RDA service with this configuration.
     *
     * @return the rda service
     */
    public RdaService createService() {
      return new RdaService(this);
    }
  }
}
