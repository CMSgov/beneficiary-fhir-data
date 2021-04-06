package gov.cms.bfd.pipeline.dc.geo;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;

import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DcGeoRDALoadJob<T> implements PipelineJob {
  private static final Logger LOGGER = LoggerFactory.getLogger(DcGeoRDALoadJob.class);
  public static final String SCAN_INTERVAL_PROPERTY = "DCGeoRDALoadIntervalSeconds";
  public static final String RUN_TIME_PROPERTY = "DCGeoRDALoadRunSeconds";
  public static final String MAX_RECORDS_PROPERTY = "DCGeoRDALoadMaxRecords";
  public static final String SCAN_INTERVAL_DEFAULT = "300";
  public static final String RUN_TIME_DEFAULT = "300";
  public static final String MAX_RECORDS_DEFAULT = String.valueOf(Integer.MAX_VALUE);
  public static final String BATCH_SIZE_PROPERTY = "DCGeoBatchSize";
  public static final String BATCH_SIZE_DEFAULT = "1";

  private final Config config;
  private final Callable<RDASource<T>> sourceFactory;
  private final Callable<RDASink<T>> sinkFactory;

  public DcGeoRDALoadJob(
      Config config, Callable<RDASource<T>> sourceFactory, Callable<RDASink<T>> sinkFactory) {
    this.config = config;
    this.sourceFactory = sourceFactory;
    this.sinkFactory = sinkFactory;
  }

  public Duration getScanInterval() {
    return config.getScanInterval();
  }

  @Override
  public PipelineJobOutcome call() throws Exception {
    final long startMillis = System.currentTimeMillis();
    int processedCount = 0;
    Exception error = null;
    try {
      try (RDASource<T> source = sourceFactory.call();
          RDASink<T> sink = sinkFactory.call()) {
        processedCount =
            source.retrieveAndProcessObjects(
                config.getMaxObjectsPerCall(), config.getBatchSize(), config.getMaxRunTime(), sink);
      }
    } catch (ProcessingException ex) {
      processedCount += ex.getProcessedCount();
      error = ex.getCause();
    } catch (Exception ex) {
      error = ex;
    }
    final long stopMillis = System.currentTimeMillis();
    LOGGER.info("processed {} objects in {} ms", processedCount, stopMillis - startMillis);
    if (error != null) {
      LOGGER.error("processing aborted by an exception: message={}", error.getMessage(), error);
      throw error;
    }
    return processedCount == 0 ? NOTHING_TO_DO : PipelineJobOutcome.WORK_DONE;
  }

  public static final class Config {
    private final Duration scanInterval;
    private final Duration maxRunTime;
    private final int maxObjectsPerCall;
    private final int batchSize;

    public Config(
        Duration scanInterval, Duration maxRunTime, int maxObjectsPerCall, int batchSize) {
      this.scanInterval = scanInterval;
      this.maxRunTime = maxRunTime;
      this.maxObjectsPerCall = maxObjectsPerCall;
      this.batchSize = batchSize;
    }

    public Config(Properties properties) {
      this(
          Duration.ofSeconds(
              Long.parseLong(
                  properties.getProperty(SCAN_INTERVAL_PROPERTY, SCAN_INTERVAL_DEFAULT))),
          Duration.ofSeconds(
              Long.parseLong(properties.getProperty(RUN_TIME_PROPERTY, RUN_TIME_DEFAULT))),
          Integer.parseInt(properties.getProperty(MAX_RECORDS_PROPERTY, MAX_RECORDS_DEFAULT)),
          Integer.parseInt(properties.getProperty(BATCH_SIZE_PROPERTY, BATCH_SIZE_DEFAULT)));
    }

    public Duration getScanInterval() {
      return scanInterval;
    }

    public Duration getMaxRunTime() {
      return maxRunTime;
    }

    public int getMaxObjectsPerCall() {
      return maxObjectsPerCall;
    }

    public int getBatchSize() {
      return batchSize;
    }
  }
}
