package gov.cms.bfd.pipeline.rda.grpc.source;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import lombok.AccessLevel;
import lombok.Getter;

public abstract class AbstractClaimTransformer {

  protected static class Metrics {

    @Getter(AccessLevel.PROTECTED)
    private final MetricRegistry metricRegistry;

    private final Histogram insertCount;

    protected Metrics(MetricRegistry appMetrics, Class<?> jobClass) {
      final String base = jobClass.getSimpleName();
      metricRegistry = appMetrics;
      insertCount = appMetrics.histogram(MetricRegistry.name(base, "insertCount"));
    }

    public void insertCount(int count) {
      insertCount.update(count);
    }
  }
}
