package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.ClaimChange;
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
  private final Supplier<MessageSource<ClaimChange>> fissSourceFactory;
  private final Supplier<MessageSource<ClaimChange>> mcsSourceFactory;

  public RdaService(
      Supplier<MessageSource<ClaimChange>> fissSourceFactory,
      Supplier<MessageSource<ClaimChange>> mcsSourceFactory) {
    this.fissSourceFactory = fissSourceFactory;
    this.mcsSourceFactory = mcsSourceFactory;
  }

  @Override
  public void getFissClaims(Empty request, StreamObserver<ClaimChange> responseObserver) {
    LOGGER.info("start getFissClaims call");
    MessageSource<ClaimChange> generator = fissSourceFactory.get();
    Responder responder = new Responder(responseObserver, generator);
    responder.sendResponses();
    LOGGER.info("end getFissClaims call");
  }

  @Override
  public void getMcsClaims(Empty request, StreamObserver<ClaimChange> responseObserver) {
    LOGGER.info("start getMcsClaims call");
    MessageSource<ClaimChange> generator = mcsSourceFactory.get();
    Responder responder = new Responder(responseObserver, generator);
    responder.sendResponses();
    LOGGER.info("end getMcsClaims call");
  }

  private static class Responder {
    private final ServerCallStreamObserver<ClaimChange> responseObserver;
    private final MessageSource<ClaimChange> generator;
    private final AtomicBoolean running;

    private Responder(
        StreamObserver<ClaimChange> responseObserver, MessageSource<ClaimChange> generator) {
      this.generator = generator;
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
            responseObserver.onNext(generator.next());
          }
          if (responseObserver.isCancelled()) {
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
