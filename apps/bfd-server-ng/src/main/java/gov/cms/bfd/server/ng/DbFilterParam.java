package gov.cms.bfd.server.ng;

/**
 * A key/value pair for a database filter.
 *
 * @param name parameter name
 * @param value parameter value
 */
public record DbFilterParam(String name, Object value) {}
