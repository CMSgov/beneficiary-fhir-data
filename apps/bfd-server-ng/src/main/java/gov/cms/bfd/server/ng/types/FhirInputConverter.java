package gov.cms.bfd.server.ng.types;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.DateUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hl7.fhir.r4.model.IdType;
import org.jetbrains.annotations.Nullable;

public class FhirInputConverter {
  public static DateTimeRange toDateTimeRange(@Nullable DateRangeParam dateRangeParam) {
    if (dateRangeParam == null) {
      return new DateTimeRange();
    }

    return new DateTimeRange(dateRangeParam);
  }

  public static Long toLong(@Nullable IdType id) {
    if (id == null) {
      throw new InvalidRequestException("ID is missing");
    }
    try {
      return id.getIdPartAsLong();
    } catch (NumberFormatException ex) {
      throw new InvalidRequestException("ID is not a valid number");
    }
  }

  public static String toString(@Nullable TokenParam tokenParam, String expectedSystem) {
    if (tokenParam == null || tokenParam.getValueNotNull().isBlank()) {
      throw new InvalidRequestException("Value is missing");
    }
    if (!expectedSystem.equals(tokenParam.getSystem())) {
      throw new InvalidRequestException("Invalid or missing system");
    }
    return tokenParam.getValue();
  }

  private static Optional<LocalDateTime> toDateTime(@Nullable DateParam dateParam) {
    return Optional.ofNullable(dateParam).map(d -> DateUtil.toLocalDateTime(d.getValue()));
  }
}
