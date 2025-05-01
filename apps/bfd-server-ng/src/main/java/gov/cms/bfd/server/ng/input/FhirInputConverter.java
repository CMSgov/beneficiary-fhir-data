package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.IdType;
import org.jetbrains.annotations.Nullable;

/**
 * Provides utility methods for converting FHIR input types to another type more suited for use in
 * the API.
 */
public class FhirInputConverter {
  /**
   * Converts a {@link DateRangeParam} to a {@link DateTimeRange}.
   *
   * @param dateRangeParam FHIR date range param
   * @return datetime range
   */
  public static DateTimeRange toDateTimeRange(@Nullable DateRangeParam dateRangeParam) {
    if (dateRangeParam == null) {
      return new DateTimeRange();
    }

    return new DateTimeRange(dateRangeParam);
  }

  /**
   * Converts an {@link IdType} to a {@link Long}.
   *
   * @param id FHIR ID
   * @return long value
   */
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

  /**
   * Converts a {@link TokenParam} with a required system to its {@link String} value.
   *
   * @param tokenParam FHIR token
   * @param expectedSystem expected System URL
   * @return string value
   */
  public static String toString(@Nullable TokenParam tokenParam, String expectedSystem) {
    if (tokenParam == null || tokenParam.getValueNotNull().isBlank()) {
      throw new InvalidRequestException("Value is missing");
    }
    if (!expectedSystem.equals(tokenParam.getSystem())) {
      throw new InvalidRequestException("Invalid or missing system");
    }
    return tokenParam.getValue();
  }
}
