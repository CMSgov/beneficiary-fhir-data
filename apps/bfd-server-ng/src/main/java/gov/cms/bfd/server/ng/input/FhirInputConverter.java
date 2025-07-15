package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.IdrConstants;
import gov.cms.bfd.server.ng.SystemUrls;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
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
   * Converts an {@link NumberParam} to an optional int.
   *
   * @param numberParam number param
   * @return int value
   */
  public static Optional<Integer> toIntOptional(@Nullable NumberParam numberParam) {
    if (numberParam == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(numberParam.getValue().intValueExact());
    } catch (ArithmeticException ex) {
      throw new InvalidRequestException("Numeric input was not in a valid format");
    }
  }

  /**
   * Converts a {@link ReferenceParam} to a numeric ID type.
   *
   * @param reference reference
   * @param validResourceType name of the resource that belongs to the ID
   * @return ID
   */
  public static Long toLong(@Nullable ReferenceParam reference, String validResourceType) {
    if (reference == null) {
      throw new InvalidRequestException("Reference is missing");
    }
    var resourceType = reference.getResourceType();
    if (!StringUtils.isBlank(resourceType) && !resourceType.equals(validResourceType)) {
      throw new InvalidRequestException("Invalid resource type");
    }

    return reference.getIdPartAsLong();
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

  /**
   * Converts a FHIR {@link IdType} for a Coverage resource into a validated {@link
   * CoverageCompositeId} object. It parses the ID string, validates its format, and ensures the
   * part is supported (currently Part A or Part B for detailed FFS mapping).
   *
   * @param coverageId The FHIR ID for the Coverage resource.
   * @return A {@link CoverageCompositeId} containing the parsed part and beneSk.
   * @throws InvalidRequestException if the ID is null, empty, malformed, or represents an
   *     unsupported part.
   */
  public static CoverageCompositeId toCoverageCompositeId(@Nullable IdType coverageId) {
    if (coverageId == null
        || coverageId.getIdPart() == null
        || coverageId.getIdPart().trim().isEmpty()) {
      throw new InvalidRequestException("Coverage ID must not be null or empty");
    }

    String rawCompositeIdStr = coverageId.getIdPart();

    return CoverageCompositeId.parse(rawCompositeIdStr);
  }

  /**
   * Maps a tag code ("Adjudicated" or "PartiallyAdjudicated") to the corresponding ClaimSourceId
   * enum values.
   *
   * @param tag The code from the _tag parameter.
   * @return A list of matching ClaimSourceId enums.
   */
  public static List<ClaimSourceId> getSourceIdsForTagCode(@Nullable TokenParam tag) {

    Set<String> SUPPORTED_ADJUDICATION_STATUSES =
        Set.of(
            IdrConstants.ADJUDICATION_STATUS_PARTIAL.toUpperCase(),
            IdrConstants.ADJUDICATION_STATUS_FINAL.toUpperCase());

    if (tag == null) {
      return new ArrayList<>();
    }

    var systemFromTag = tag.getSystem();

    if (systemFromTag != null && !systemFromTag.equals(SystemUrls.SYS_ADJUDICATION_STATUS)) {
      throw new InvalidRequestException(
          "Unsupported system for _tag adjudication status. Expected system '"
              + SystemUrls.SYS_ADJUDICATION_STATUS
              + "'.");
    }

    var statusValue = tag.getValue();

    if (!SUPPORTED_ADJUDICATION_STATUSES.contains(statusValue.toUpperCase())) {
      throw new InvalidRequestException(
          "Unsupported _tag value for adjudication status. Supported values are '"
              + IdrConstants.ADJUDICATION_STATUS_PARTIAL
              + "' and '"
              + IdrConstants.ADJUDICATION_STATUS_FINAL
              + "'.");
    }
    var matchingSources =
        Stream.of(ClaimSourceId.values())
            .filter(
                sourceId ->
                    sourceId.getAdjudicationStatus().isPresent()
                        && sourceId.getAdjudicationStatus().get().equalsIgnoreCase(statusValue))
            .toList();

    return matchingSources;
  }
}
