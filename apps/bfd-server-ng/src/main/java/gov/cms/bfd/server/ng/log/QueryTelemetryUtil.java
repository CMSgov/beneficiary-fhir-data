package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.LOG_TYPE;

import jakarta.persistence.TypedQuery;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Utility for executing for capturing database query telemetry metrics. */
@Component
public class QueryTelemetryUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryTelemetryUtil.class);

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
    var duration = Duration.ofNanos(System.nanoTime() - start).toMillis();

    LOGGER
        .atInfo()
        .setMessage("Database query Completed")
        .addKeyValue(LOG_TYPE, "databaseQueryTelemetry")
        .addKeyValue("query_name", queryName)
        .addKeyValue("duration_in_milliseconds", duration)
        .addKeyValue("row_count", results.size())
        .log();

    return results;
  }
}
