package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.protobuf.Empty;
import gov.cms.mpsm.rda.v1.RDAServiceGrpc;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class RdaService extends RDAServiceGrpc.RDAServiceImplBase {
  private final Supplier<FissClaimSource> sourceFactory;

  public RdaService(Supplier<FissClaimSource> sourceFactory) {
    this.sourceFactory = sourceFactory;
  }

  @Override
  public void getFissClaims(Empty request, StreamObserver<FissClaim> responseObserver) {
    FissClaimSource generator = sourceFactory.get();
    FissClaimResponder responder = new FissClaimResponder(responseObserver, generator);
    responder.sendResponses();
  }

  private static class FissClaimResponder {
    private final ServerCallStreamObserver<FissClaim> responseObserver;
    private final FissClaimSource generator;
    private final AtomicBoolean running;

    private FissClaimResponder(
        StreamObserver<FissClaim> responseObserver, FissClaimSource generator) {
      this.generator = generator;
      this.running = new AtomicBoolean(true);
      this.responseObserver = (ServerCallStreamObserver<FissClaim>) responseObserver;
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
          } else if (!generator.hasNext()) {
            running.set(false);
            responseObserver.onCompleted();
            generator.close();
          }
        } catch (Exception ex) {
          running.set(false);
          ex.printStackTrace(System.out);
          responseObserver.onError(ex);
        }
      }
    }
  }
}
