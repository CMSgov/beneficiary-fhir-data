package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.DateParam;
import gov.cms.bfd.server.ng.DateUtil;
import java.time.LocalDateTime;

/**
 * Represents a boundary condition for a DateTime filter.
 *
 * @param bound boundary DateTime
 * @param boundType Denotes if the bound is inclusive or exclusive
 */
public record DateTimeBound(LocalDateTime bound, DateTimeBoundType boundType) {
  /**
   * Creates a new {@link DateTimeBound} from a {@link DateParam}.
   *
   * @param dateParam FHIR date param
   */
  public DateTimeBound(DateParam dateParam) {
    this(
        DateUtil.toLocalDateTime(dateParam.getValue()),
        DateTimeBoundType.fromPrefix(dateParam.getPrefix()));
  }
}
