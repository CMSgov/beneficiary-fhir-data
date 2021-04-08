package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RDASink;
import gov.cms.bfd.pipeline.rda.grpc.RDASource;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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

  private final GrpcStreamCaller.Factory<T> callerFactory;
  private final Clock clock;
  private ManagedChannel channel;

  public GrpcRDASource(Config config, GrpcStreamCaller.Factory<T> callerFactory) {
    this.callerFactory = callerFactory;
    clock = Clock.systemDefaultZone();
    channel =
        ManagedChannelBuilder.forAddress(config.host, config.port)
            .usePlaintext()
            .idleTimeout(config.maxIdle.toMillis(), TimeUnit.MILLISECONDS)
            .build();
  }

  @VisibleForTesting
  GrpcRDASource(ManagedChannel channel, GrpcStreamCaller.Factory<T> callerFactory, Clock clock) {
    this.callerFactory = callerFactory;
    this.channel = channel;
    this.clock = clock;
  }

  @Override
  public int retrieveAndProcessObjects(
      int maxToProcess, int maxPerBatch, Duration maxRunTime, RDASink<PreAdjudicatedClaim> sink)
      throws ProcessingException {
    int processed = 0;
    try {
      final GrpcStreamCaller<T> caller = callerFactory.createCaller(channel);
      final List<PreAdjudicatedClaim> batch = new ArrayList<>();
      Instant now = clock.instant();
      final Instant stopTime = now.plus(maxRunTime);
      while (shouldContinue(processed, maxToProcess, now, stopTime)) {
        final Iterator<T> resultIterator = caller.callService(Duration.between(now, stopTime));
        now = clock.instant();
        while (resultIterator.hasNext() && shouldContinue(processed, maxToProcess, now, stopTime)) {
          final T result = resultIterator.next();
          final PreAdjudicatedClaim claim = caller.convertResultToClaim(result);
          batch.add(claim);
          if (batch.size() >= maxPerBatch) {
            processed += submitBatchToSink(sink, batch);
          }
          now = clock.instant();
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

  private boolean shouldContinue(int processed, int maxToProcess, Instant now, Instant stopTime) {
    if (processed >= maxToProcess) {
      LOGGER.info("exiting loop after processing max number of records: processed={}", processed);
      return false;
    }
    if (now.compareTo(stopTime) >= 0) {
      LOGGER.info("exiting loop after reaching max runtime");
      return false;
    }
    return true;
  }

  private int submitBatchToSink(RDASink<PreAdjudicatedClaim> sink, List<PreAdjudicatedClaim> batch)
      throws ProcessingException {
    LOGGER.info("submitting batch to sink: size={}", batch.size());
    int processed = sink.writeBatch(batch);
    LOGGER.info("submitted batch to sink: size={} processed={}", batch.size(), processed);
    batch.clear();
    return processed;
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
