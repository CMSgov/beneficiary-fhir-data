package gov.cms.bfd.server.ng.log;

/**
 * Represents telemetry captures for a single database query execution.
 *
 * @param name the name of the query
 * @param durationMs the duration of the query execution in milliseconds
 * @param rowCount the number of rows returned
 */
public record DbQueryTelemetry(String name, long durationMs, int rowCount) {}
