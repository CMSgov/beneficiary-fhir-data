package gov.cms.bfd.server.ng;

import jakarta.persistence.Query;
import java.util.List;

/**
 * A key/value pair for a database filter.
 *
 * @param name parameter name
 * @param value parameter value
 */
public record DbFilterParam(String name, Object value) {
  /** Applies the params to the query. */
  public static <T extends Query> T withParams(T query, List<DbFilterParam> params) {
    for (var param : params) {
      query.setParameter(param.name(), param.value());
    }
    return query;
  }
}
