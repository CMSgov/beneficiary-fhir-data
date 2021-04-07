package gov.cms.bfd.pipeline.rda.grpc.source;

import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RDASink;
import gov.cms.bfd.pipeline.rda.grpc.RDASource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General RDASource implementation that delegates actual service call and result mapping to another
 * class.
 *
 * @param <T> type of objects returned by the gRPC service
 */
public class GrpcRDASource<T> implements RDASource<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRDASource.class);

  private final GrpcStreamCaller<T> caller;
  private ManagedChannel channel;

  public GrpcRDASource(Config config, GrpcStreamCaller<T> caller) {
    this.caller = caller;
    channel =
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .usePlaintext()
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build();
  }

  @Override
  public int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RDASink<PreAdjudicatedClaim> sink)
      throws ProcessingException {
    int processed = 0;
    try {
      final List<PreAdjudicatedClaim> batch = new ArrayList<>();
      final Instant stopTime = Instant.now().plus(maxRunTime);
      caller.createStub(channel);
      while (true) {
        if (processed >= maxToProcess) {
          LOGGER.info(
              "exiting loop after processing max number of records: processed={}", processed);
          break;
        }
        if (runtimeExceeded(stopTime)) {
          LOGGER.info("exiting loop after reaching max runtime");
          break;
        }
        final Iterator<T> resultIterator = caller.callService(maxRunTime);
        while (resultIterator.hasNext()) {
          final T result = resultIterator.next();
          final PreAdjudicatedClaim claim = caller.convertResultToClaim(result);
          batch.add(claim);
          if (batch.size() >= maxPerBatch) {
            processed += submitBatchToSink(sink, batch);
          }
        }
        if (batch.size() > 0) {
          processed += submitBatchToSink(sink, batch);
        }
      }
    } catch (ProcessingException ex) {
      throw new ProcessingException(ex.getCause(), processed + ex.getProcessedCount());
    } catch (Exception ex) {
      throw new ProcessingException(ex, processed);
    }
    return processed;
  }

  @Override
  public void close() throws Exception {
    if (channel != null) {
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      channel = null;
    }
  }

  private int submitBatchToSink(RDASink<PreAdjudicatedClaim> sink, List<PreAdjudicatedClaim> batch)
      throws ProcessingException {
    LOGGER.info("submitting batch to sink: size={}", batch.size());
    int processed = sink.writeBatch(Collections.unmodifiableCollection(batch));
    LOGGER.info("submitted batch to sink: size={} processed={}", batch.size(), processed);
    batch.clear();
    return processed;
  }

  private boolean runtimeExceeded(Instant stopTime) {
    return Instant.now().compareTo(stopTime) < 0;
  }

  public static class Config {
    private final String host;
    private final int port;
    private final Duration maxIdle;

    public Config(String host, int port, Duration maxIdle) {
      this.host = host;
      this.port = port;
      this.maxIdle = maxIdle;
    }
  }
}
