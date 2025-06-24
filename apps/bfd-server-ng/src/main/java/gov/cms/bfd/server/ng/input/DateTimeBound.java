package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import gov.cms.bfd.server.ng.DateUtil;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Represents a boundary condition for a DateTime filter.
 *
 * @param bound boundary DateTime
 * @param boundType Denotes if the bound is inclusive or exclusive
 */
public record DateTimeBound(ZonedDateTime bound, DateTimeBoundType boundType) {
  /**
   * Creates a new {@link DateTimeBound} from a {@link DateParam}.
   *
   * @param dateParam FHIR date param
   */
  public DateTimeBound(Date date, ParamPrefixEnum prefix) {
    this(DateUtil.toZonedDateTime(date), DateTimeBoundType.fromPrefix(prefix));
  }
}
