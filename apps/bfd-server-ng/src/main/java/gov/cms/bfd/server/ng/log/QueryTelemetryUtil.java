package gov.cms.bfd.server.ng.log;

import jakarta.persistence.TypedQuery;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/** Utility for executing for capturing database query telemetry metrics. */
@Component
public class QueryTelemetryUtil {

  /**
   * Executes the query and records telemetry which consists of query name, execution duration and
   * number of records returned.
   *
   * @param queryName the query name
   * @param query the query to execute
   * @param <T> query result type
   * @return query results
   */
  public <T> List<T> executeAndTrack(String queryName, TypedQuery<T> query) {

    var start = System.nanoTime();
    var results = query.getResultList();
    var dbQueryTelemetry =
        new DbQueryTelemetry(
            queryName, Duration.ofNanos(System.nanoTime() - start).toMillis(), results.size());
    RequestTelemetryContext.addDbQuery(dbQueryTelemetry);
    return results;
  }
}
