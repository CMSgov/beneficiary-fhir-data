package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Stopwatch;
import com.nava.health.v1.HealthCheckRequest;
import com.nava.health.v1.HealthCheckResponse;
import com.nava.health.v1.HealthGrpc;
import gov.cms.bfd.pipeline.rda.grpc.PreAdjudicatedClaim;
import gov.cms.bfd.pipeline.rda.grpc.ProcessingException;
import gov.cms.bfd.pipeline.rda.grpc.RDASink;
import gov.cms.bfd.pipeline.rda.grpc.RDASource;
import io.grpc.Deadline;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckSource implements RDASource<PreAdjudicatedClaim> {
  private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckSource.class);

  private final Config config;
  private ManagedChannel channel;

  public HealthCheckSource(Config config) {
    this.config = config;
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
    final HealthGrpc.HealthBlockingStub stub = HealthGrpc.newBlockingStub(channel);
    final HealthCheckRequest request = HealthCheckRequest.newBuilder().setService("").build();
    final List<PreAdjudicatedClaim> batch = new ArrayList<>();
    final Stopwatch timer = Stopwatch.createStarted();
    int processed = 0;
    try {
      while (true) {
        if (processed >= maxToProcess) {
          LOGGER.info(
              "exiting loop after processing max number of records: processed={}", processed);
          break;
        }
        if (runtimeRemains(timer, maxRunTime)) {
          LOGGER.info(
              "exiting loop after reaching max runtime: runtime={}ms", timer.elapsed().toMillis());
          break;
        }
        final Iterator<HealthCheckResponse> responses =
            stub.withDeadline(Deadline.after(maxRunTime.toMillis(), TimeUnit.MILLISECONDS))
                .watch(request);
        while (responses.hasNext()) {
          responses.next(); // we don't use the actual object
          batch.add(new PreAdjudicatedClaim());
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

  private boolean runtimeRemains(Stopwatch timer, Duration maxRunTime) {
    return timer.elapsed().compareTo(maxRunTime) < 0;
  }

  public static class Config {
    private final String host;
    private final int port;
    private final Duration maxIdle;

    public Config() {
      this("localhost", 5001, Duration.ofSeconds(30));
    }

    public Config(String host, int port, Duration maxIdle) {
      this.host = host;
      this.port = port;
      this.maxIdle = maxIdle;
    }
  }
}
