package gov.cms.bfd.server.ng.types;

import ca.uhn.fhir.rest.param.DateParam;
import gov.cms.bfd.server.ng.DateUtil;
import java.time.LocalDateTime;

public record DateTimeBound(LocalDateTime bound, DateTimeBoundType boundType) {
  public DateTimeBound(DateParam dateParam) {
    this(
        DateUtil.toLocalDateTime(dateParam.getValue()),
        DateTimeBoundType.fromPrefix(dateParam.getPrefix()));
  }
}
