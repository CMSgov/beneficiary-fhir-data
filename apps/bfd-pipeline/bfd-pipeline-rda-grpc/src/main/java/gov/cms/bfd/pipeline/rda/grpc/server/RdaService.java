package gov.cms.bfd.pipeline.rda.grpc.server;

import gov.cms.mpsm.rda.v1.ClaimRequest;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of RDAServiceImplBase that services FissClaims using a FissClaimSource object. A
 * Supplier is used to create a new FissClaimSource object for each client call to getFissClaims.
 */
public class RdaService extends RDAServiceGrpc.RDAServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(RdaService.class);
  private final Supplier<MessageSource<FissClaimChange>> fissSourceFactory;
  private final Supplier<MessageSource<McsClaimChange>> mcsSourceFactory;

  public RdaService(
      Supplier<MessageSource<FissClaimChange>> fissSourceFactory,
      Supplier<MessageSource<McsClaimChange>> mcsSourceFactory) {
    this.fissSourceFactory = fissSourceFactory;
    this.mcsSourceFactory = mcsSourceFactory;
  }

  @Override
  public void getFissClaims(
      ClaimRequest request, StreamObserver<FissClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call");
    MessageSource<FissClaimChange> generator = fissSourceFactory.get();
    Responder responder = new Responder(responseObserver, generator);
    responder.sendResponses();
    LOGGER.info("end getFissClaims call");
  }

  @Override
  public void getMcsClaims(ClaimRequest request, StreamObserver<McsClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call");
    MessageSource<McsClaimChange> generator = mcsSourceFactory.get();
    Responder responder = new Responder(responseObserver, generator);
    responder.sendResponses();
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
          while (responseObserver.isReady()
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
}
