package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.*;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.base.Strings;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatch;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchEntry;
import gov.cms.bfd.server.ng.claim.model.ClaimFinalAction;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.MetaSourceSk;
import gov.cms.bfd.server.ng.claim.model.SamhsaSearchIntent;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.jetbrains.annotations.Nullable;

/**
 * Provides utility methods for converting FHIR input types to another type more suited for use in
 * the API.
 */
public class FhirInputConverter {
  private static final Set<String> SUPPORTED_SYSTEM_TYPES =
      Set.of(
          IdrConstants.SYSTEM_TYPE_NCH.toUpperCase(),
          IdrConstants.SYSTEM_TYPE_SHARED.toUpperCase(),
          IdrConstants.SYSTEM_TYPE_DDPS.toUpperCase());

  private static final Map<String, List<MetaSourceSk>> SOURCE_ID_MAP =
      Stream.of(MetaSourceSk.values())
          .collect(Collectors.groupingBy(s -> s.toFhirSystemType().getCode().toUpperCase()));

  private static final Set<String> SUPPORTED_FINAL_ACTION_STATUSES =
      Stream.of(ClaimFinalAction.values())
          .map(ClaimFinalAction::getFinalAction)
          .collect(Collectors.toSet());

  private static final Map<String, ClaimFinalAction> FINAL_ACTION_MAP =
      Stream.of(ClaimFinalAction.values())
          .collect(Collectors.toMap(a -> a.getFinalAction().toUpperCase(), a -> a));

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
    } catch (NumberFormatException _) {
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
   * part is supported (currently Part A or Part B for detailed FFS mapping and Part C and D).
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
   * Parses the tag query parameter into a nested list of criteria.
   *
   * <p>Outer list is AND conditions, inner list is OR conditions.
   *
   * @param tagParam _tag param from request
   * @return list of tag criteria
   */
  public static List<List<TagCriterion>> parseTagParameter(@Nullable TokenAndListParam tagParam) {
    return FhirTokenParameterParser.parse(tagParam, FhirInputConverter::parseTagToken);
  }

  private static List<TagCriterion> parseTagToken(TokenParam token) {
    if (token.getSystem() == null) {
      throw new InvalidRequestException(
          "Searching by tag requires a token (system|code) to be specified.");
    }

    var system = token.getSystem();
    var code = token.getValue().trim();

    if (SystemUrls.BLUE_BUTTON_SYSTEM_TYPE.equals(system)) {
      if (!SUPPORTED_SYSTEM_TYPES.contains(code.toUpperCase())) {
        throw new InvalidRequestException(
            String.format(
                "Unsupported _tag value for system type. Supported values are '%s', '%s', '%s'.",
                IdrConstants.SYSTEM_TYPE_NCH,
                IdrConstants.SYSTEM_TYPE_SHARED,
                IdrConstants.SYSTEM_TYPE_DDPS));
      }
      var sourceIds = SOURCE_ID_MAP.get(code.toUpperCase());
      if (sourceIds == null || sourceIds.isEmpty()) {
        throw new InvalidRequestException("Unknown claim source id: " + code);
      }
      return sourceIds.stream()
          .map(id -> (TagCriterion) new TagCriterion.MetaSourceSkCriterion(id))
          .toList();

    } else if (SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS.equals(system)) {
      if (!FINAL_ACTION_MAP.containsKey(code.toUpperCase())) {
        throw new InvalidRequestException(
            String.format(
                "Unsupported _tag value for Final Action Status. Supported values are %s.",
                SUPPORTED_FINAL_ACTION_STATUSES.stream()
                    .map(s -> String.format("'%s'", s))
                    .collect(Collectors.joining(", "))));
      }
      return List.of(
          new TagCriterion.FinalActionCriterion(FINAL_ACTION_MAP.get(code.toUpperCase())));

    } else {
      throw new InvalidRequestException(
          String.format(
              "Invalid tag system specified. Supported systems are '%s', %s'.",
              SystemUrls.BLUE_BUTTON_SYSTEM_TYPE, SystemUrls.BLUE_BUTTON_FINAL_ACTION_STATUS));
    }
  }

  /**
   * Gets claim type codes mapped to type params.
   *
   * @param typeParam The type from the type parameter.
   * @return A list of matching ClaimTypeCode.
   */
  public static List<ClaimTypeCode> getClaimTypeCodesForType(
      @Nullable TokenAndListParam typeParam) {

    return FhirTokenParameterParser.flatten(typeParam)
        .map(token -> token.getValue().trim().toLowerCase())
        .flatMap(normalizedType -> ClaimTypeCode.getClaimTypeCodesByType(normalizedType).stream())
        .toList();
  }

  public static Optional<PatientMatch> getPatientMatch(@Nullable Patient patient) {
    if (patient == null) {
      return Optional.empty();
    }
    var name = patient.getName().stream().findFirst();
    Optional<Object> birthDate =
        Optional.ofNullable(patient.getBirthDate()).map(DateUtil::toLocalDate);
    Optional<Object> firstName =
        name.flatMap(n -> n.getGiven().stream().findFirst())
            .map(s -> normalizeString(s.toString()));
    Optional<Object> lastName = name.map(n -> normalizeString(n.getFamily()));
    var addresses =
        patient.getAddress().stream()
            .map(FhirInputConverter::formatAddress)
            .flatMap(Optional::stream)
            .toList();
    Optional<Object> mbi = findIdentifier(patient, SystemUrls.CMS_MBI).map(i -> i);

    var ssnLastFourLength = 4;
    Optional<Object> ssn =
        findIdentifier(patient, SystemUrls.US_SSN)
            .filter(s -> s.length() >= ssnLastFourLength)
            .map(s -> s.substring(s.length() - ssnLastFourLength));

    return Optional.of(
        PatientMatch.builder()
            .firstName(
                new PatientMatchEntry(
                    firstName.stream().toList(), "beneficiaryName.normalizedFirstName"))
            .lastName(
                new PatientMatchEntry(
                    lastName.stream().toList(), "beneficiaryName.normalizedLastName"))
            .mbi(new PatientMatchEntry(mbi.stream().toList(), "identifier.mbi"))
            .birthDate(new PatientMatchEntry(birthDate.stream().toList(), "birthDate"))
            .addresses(new PatientMatchEntry(addresses, "address.normalizedAddress"))
            .ssnLastFourDigits(new PatientMatchEntry(ssn.stream().toList(), "ssn"))
            .build());
  }

  private static Optional<Object> formatAddress(Address address) {
    var line = address.getLine().stream().findFirst().map(StringType::toString);
    return line.map(
        s ->
            String.format(
                "%s %s %s %s", s, address.getCity(), address.getState(), address.getPostalCode()));
  }

  private static Optional<String> findIdentifier(Patient patient, String system) {
    return patient.getIdentifier().stream()
        .filter(i -> i.getSystem().equals(system))
        .map(Identifier::getValue)
        .findFirst();
  }

  private static String normalizeString(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{Punct}\\s", "")
        .toUpperCase();
  }

  /**
   * Parses the _security search parameter into a {@link SamhsaSearchIntent}.
   *
   * <p>Supports granular inclusion and exclusion of the SAMHSA claims.
   *
   * @param securityParam _source param from request
   * @return {@link SamhsaSearchIntent}
   */
  public static SamhsaSearchIntent parseSecurityParameter(
      @Nullable TokenAndListParam securityParam) {

    var tokens = FhirTokenParameterParser.flatten(securityParam).toList();

    if (tokens.isEmpty()) {
      return SamhsaSearchIntent.UNSPECIFIED;
    }
    var requested = false;
    var excluded = false;

    for (var token : tokens) {
      validateSamhsaToken(token);
      var hasNotModifier = TokenParamModifier.NOT == token.getModifier();
      if (hasNotModifier) {
        excluded = true;
      } else {
        requested = true;
      }
    }

    return resolveSamhsaSearchIntent(requested, excluded);
  }

  private static void validateSamhsaToken(TokenParam token) {
    if (isSamhsaActCode(token)) {
      return;
    }
    throw new InvalidRequestException(
        String.format(
            "Invalid security code: Use '%s' or '%s|%s'.",
            IdrConstants.SAMHSA_SECURITY_CODE,
            SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL,
            IdrConstants.SAMHSA_SECURITY_CODE));
  }

  private static boolean isSamhsaActCode(TokenParam token) {
    var code = token.getValue();
    var system = token.getSystem();

    return (IdrConstants.SAMHSA_SECURITY_CODE.equalsIgnoreCase(code)
            && (Strings.isNullOrEmpty(system)))
        || (SystemUrls.SAMHSA_ACT_CODE_SYSTEM_URL.equalsIgnoreCase(system)
            && IdrConstants.SAMHSA_SECURITY_CODE.equalsIgnoreCase(code));
  }

  private static SamhsaSearchIntent resolveSamhsaSearchIntent(
      boolean requestedSamhsa, boolean excludedSamhsa) {
    if (requestedSamhsa && excludedSamhsa) {
      throw new InvalidRequestException(
          "Conflict in search filters: You have requested both to include and exclude the '42CFRPart2' (SAMHSA) security label. Please choose one to see results.");
    }
    if (requestedSamhsa) {
      return SamhsaSearchIntent.ONLY_SAMHSA;
    }
    if (excludedSamhsa) {
      return SamhsaSearchIntent.EXCLUDE_SAMHSA;
    }

    return SamhsaSearchIntent.UNSPECIFIED;
  }

  /**
   * Parses the source query parameter into a nested list of sources.
   *
   * <p>Outer list is AND conditions, inner list is OR conditions.
   *
   * @param sourceParam _source param from request
   * @return list of sources
   */
  public static List<List<MetaSourceSk>> parseSourceParameter(
      @Nullable TokenAndListParam sourceParam) {
    return FhirTokenParameterParser.parse(sourceParam, token -> List.of(parseSourceToken(token)));
  }

  private static MetaSourceSk parseSourceToken(TokenParam token) {
    var source = token.getValue();

    if (source == null || source.trim().isEmpty()) {
      throw new InvalidRequestException("Source cannot be empty.");
    }

    return MetaSourceSk.tryFromDisplay(source.trim())
        .orElseThrow(
            () ->
                new InvalidRequestException(
                    "Unknown source. Supported sources are: "
                        + Arrays.toString(MetaSourceSk.values())));
  }
}
