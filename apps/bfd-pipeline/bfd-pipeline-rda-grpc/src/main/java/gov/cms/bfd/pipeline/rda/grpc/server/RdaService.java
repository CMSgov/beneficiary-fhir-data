package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ApiVersion;
import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
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
  public static final String RDA_PROTO_VERSION = "0.4";

  private final Config config;

  public RdaService(Config config) {
    this.config = config;
  }

  @Override
  public void getVersion(Empty request, StreamObserver<ApiVersion> responseObserver) {
    try {
      LOGGER.info("getVersion called: response={}", config.getVersion());
      responseObserver.onNext(config.getVersion().toApiVersion());
      responseObserver.onCompleted();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
  }

  @Override
  public void getFissClaims(
      ClaimRequest request, StreamObserver<FissClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call with since={}", request.getSince());
    try {
      MessageSource<FissClaimChange> generator =
          config.getFissSourceFactory().apply(request.getSince());
      Responder<FissClaimChange> responder = new Responder<>(responseObserver, generator);
      responder.sendResponses();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
    LOGGER.info("end getFissClaims call");
  }

  @Override
  public void getMcsClaims(ClaimRequest request, StreamObserver<McsClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call with since={}", request.getSince());
    try {
      MessageSource<McsClaimChange> generator =
          config.getMcsSourceFactory().apply(request.getSince());
      Responder<McsClaimChange> responder = new Responder<>(responseObserver, generator);
      responder.sendResponses();
    } catch (Exception ex) {
      responseObserver.onError(ex);
    }
    LOGGER.info("end getMcsClaims call");
  }

  private static class Responder<TChange> {
    private final ServerCallStreamObserver<TChange> responseObserver;
    private final MessageSource<TChange> generator;
    private final AtomicBoolean cancelled;
    private final AtomicBoolean running;

    private Responder(StreamObserver<TChange> responseObserver, MessageSource<TChange> generator) {
      this.generator = generator;
      this.cancelled = new AtomicBoolean(false);
      this.running = new AtomicBoolean(true);
      this.responseObserver = (ServerCallStreamObserver<TChange>) responseObserver;
      this.responseObserver.setOnReadyHandler(this::sendResponses);
      this.responseObserver.setOnCancelHandler(() -> cancelled.set(true));
    }

    private void sendResponses() {
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
          responseObserver.onError(ex);
        }
      }
    }
  }

  @Value
  @Builder
  public static class Version {
    @Builder.Default String version = RDA_PROTO_VERSION;
    @Builder.Default String commitId = "";
    @Builder.Default String buildTime = "";

    public ApiVersion toApiVersion() {
      return ApiVersion.newBuilder()
          .setVersion(version)
          .setCommitId(commitId)
          .setBuildTime(buildTime)
          .build();
    }
  }

  @Value
  @Builder
  public static class Config {
    @Builder.Default
    MessageSource.Factory<FissClaimChange> fissSourceFactory = EmptyMessageSource.factory();

    @Builder.Default
    MessageSource.Factory<McsClaimChange> mcsSourceFactory = EmptyMessageSource.factory();

    @Builder.Default Version version = Version.builder().build();

    public RdaService createService() {
      return new RdaService(this);
    }
  }
}
