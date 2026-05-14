package gov.cms.bfd.server.ng.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores request metrics. This context gets propagated to asynchronous database query threads. This
 * is needed so database query timings can be accumulated amd emitted in the final request telemetry
 * log.
 */
public class RequestTelemetryContext {

  private static final ThreadLocal<RequestTelemetryContext> CONTEXT = new ThreadLocal<>();
  private final List<DbQueryTelemetry> dbQueries = new ArrayList<>();

  /** Creates a context for the current thread. */
  public static void createContext() {
    var context = new RequestTelemetryContext();
    CONTEXT.set(context);
  }

  /**
   * Returns the context associated with the current thread.
   *
   * @return the current context
   */
  public static RequestTelemetryContext current() {
    return CONTEXT.get();
  }

  /** Removes the context associated the current thread. */
  public static void clear() {
    CONTEXT.remove();
  }

  /**
   * Sets the context for the current thread so database query metrics can be shared with
   * asynchronous tasks.
   *
   * @param context the context for the current thread
   */
  public static void set(RequestTelemetryContext context) {
    CONTEXT.set(context);
  }

  /**
   * Records the database query telemetry.
   *
   * @param query telemetry details for an executed query
   */
  public static void addDbQuery(DbQueryTelemetry query) {
    var context = current();

    if (context != null) {
      context.dbQueries.add(query);
    }
  }

  /**
   * Returns all database query telemetry collected for a request.
   *
   * @return list of database query telemetry entries
   */
  public List<DbQueryTelemetry> getDbQueries() {
    return dbQueries;
  }
}
