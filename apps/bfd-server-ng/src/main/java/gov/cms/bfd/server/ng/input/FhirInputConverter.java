package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.ArrayList;
import java.util.Collections;
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
  private FhirInputConverter() {}

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
      var longId = id.getIdPartAsLong();
      if (longId == null) {
        throw new InvalidRequestException("ID is not a valid number");
      }
      return longId;
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
    if (numberParam == null || numberParam.getValue() == null) {
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
    if (reference == null || reference.getIdPartAsLong() == null) {
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

    var rawCompositeIdStr = coverageId.getIdPart();
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
    if (tag == null || tag.getValue() == null) {
      return Collections.emptyList();
    }

    var supportedAdjudicationStatuses =
        Set.of(
            IdrConstants.ADJUDICATION_STATUS_PARTIAL.toUpperCase(),
            IdrConstants.ADJUDICATION_STATUS_FINAL.toUpperCase());

    var systemFromTag = tag.getSystem();

    if (systemFromTag != null && !systemFromTag.equals(SystemUrls.SYS_ADJUDICATION_STATUS)) {
      throw new InvalidRequestException(
          String.format(
              "Unsupported system for _tag adjudication status. Expected system '%s'.",
              SystemUrls.SYS_ADJUDICATION_STATUS));
    }

    var statusValue = tag.getValue();

    if (!supportedAdjudicationStatuses.contains(statusValue.toUpperCase())) {
      throw new InvalidRequestException(
          String.format(
              "Unsupported _tag value for adjudication status. Supported values are '%s' and '%s'.",
              IdrConstants.ADJUDICATION_STATUS_PARTIAL, IdrConstants.ADJUDICATION_STATUS_FINAL));
    }
    return Stream.of(ClaimSourceId.values())
        .filter(
            sourceId -> {
              var adjudicationStatus = sourceId.getAdjudicationStatus();
              return adjudicationStatus.isPresent()
                  && adjudicationStatus.get().equalsIgnoreCase(statusValue);
            })
        .toList();
  }

  /**
   * Gets claim type codes mapped to type params.
   *
   * @param typeParam The type from the type parameter.
   * @return A list of matching ClaimTypeCode.
   */
  public static List<ClaimTypeCode> getClaimTypeCodesForType(
      @Nullable TokenAndListParam typeParam) {

    List<ClaimTypeCode> claimTypeCodes = new ArrayList<>();

    if (typeParam == null || typeParam.getValuesAsQueryTokens().isEmpty()) {
      return Collections.emptyList();
    }
    var typeParams = typeParam.getValuesAsQueryTokens();

    for (TokenOrListParam type : typeParams) {
      claimTypeCodes.addAll(ClaimTypeCode.getClaimTypeCodesByType(type.getValuesAsQueryTokens()));
    }

    return claimTypeCodes;
  }
}
