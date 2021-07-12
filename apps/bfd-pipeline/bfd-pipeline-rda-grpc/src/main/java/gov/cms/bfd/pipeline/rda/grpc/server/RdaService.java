package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of RDAServiceImplBase that services FissClaims using a FissClaimSource object. A
 * Supplier is used to create a new FissClaimSource object for each client call to getFissClaims.
 */
public class RdaService extends RDAServiceGrpc.RDAServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaService.class);
  private final Supplier<ClaimSource<FissClaim>> fissSourceFactory;
  private final Supplier<ClaimSource<McsClaim>> mcsSourceFactory;

  public RdaService(
      Supplier<ClaimSource<FissClaim>> fissSourceFactory,
      Supplier<ClaimSource<McsClaim>> mcsSourceFactory) {
    this.fissSourceFactory = fissSourceFactory;
    this.mcsSourceFactory = mcsSourceFactory;
  }

  @Override
  public void getFissClaims(Empty request, StreamObserver<ClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call");
    ClaimSource<FissClaim> generator = fissSourceFactory.get();
    Responder<FissClaim> responder =
        new Responder<>(responseObserver, generator, ClaimChange.Builder::setFissClaim);
    responder.sendResponses();
    LOGGER.info("end getFissClaims call");
  }

  @Override
  public void getMcsClaims(Empty request, StreamObserver<ClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call");
    ClaimSource<McsClaim> generator = mcsSourceFactory.get();
    Responder<McsClaim> responder =
        new Responder<>(responseObserver, generator, ClaimChange.Builder::setMcsClaim);
    responder.sendResponses();
    LOGGER.info("end getMcsClaims call");
  }

  private static class Responder<T> {
    private final ServerCallStreamObserver<ClaimChange> responseObserver;
    private final ClaimSource<T> generator;
    private final BiConsumer<ClaimChange.Builder, T> setter;
    private final AtomicBoolean running;

    private Responder(
        StreamObserver<ClaimChange> responseObserver,
        ClaimSource<T> generator,
        BiConsumer<ClaimChange.Builder, T> setter) {
      this.generator = generator;
      this.setter = setter;
      this.running = new AtomicBoolean(true);
      this.responseObserver = (ServerCallStreamObserver<ClaimChange>) responseObserver;
      this.responseObserver.setOnReadyHandler(this::sendResponses);
    }

    private void sendResponses() {
      if (running.get()) {
        try {
          while (responseObserver.isReady()
              && !responseObserver.isCancelled()
              && generator.hasNext()) {
            ClaimChange.Builder builder = ClaimChange.newBuilder();
            builder.setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE);
            setter.accept(builder, generator.next());
            responseObserver.onNext(builder.build());
          }
          if (responseObserver.isCancelled()) {
            running.set(false);
            responseObserver.onCompleted();
            generator.close();
            LOGGER.info("getFissClaims call cancelled by client");
          } else if (!generator.hasNext()) {
            running.set(false);
            responseObserver.onCompleted();
            generator.close();
            LOGGER.info("getFissClaims call complete");
          }
        } catch (Exception ex) {
          running.set(false);
          LOGGER.error("getFissClaims caught exception: {}", ex.getMessage(), ex);
          responseObserver.onError(ex);
        }
      }
    }
  }
}
