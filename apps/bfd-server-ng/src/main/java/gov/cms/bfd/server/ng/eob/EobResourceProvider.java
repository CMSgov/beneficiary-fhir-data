package gov.cms.bfd.server.ng.eob;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Offset;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.ClaimFilterOptions;
import gov.cms.bfd.server.ng.Configuration;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.claim.model.SamhsaSearchIntent;
import gov.cms.bfd.server.ng.input.ClaimIdSearchCriteria;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import gov.cms.bfd.server.ng.model.QueryProfile;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

/** FHIR endpoints for the ExplanationOfBenefit resource. */
@RequiredArgsConstructor
@Component
@SuppressWarnings({"java:S107", "java:S3252"})
public class EobResourceProvider implements IResourceProvider {
  private final EobHandler eobHandler;
  private final CertificateUtil certificateUtil;
  private final Configuration configuration;
  private static final String SERVICE_DATE = "service-date";
  private static final String START_INDEX = "startIndex";
  private static final String TYPE = "type";
  private static final String INCLUDE_TAX_NUMBERS_HEADER = "IncludeTaxNumbers";
  private static final String OUTCOME = "outcome";
  private static final String SOURCE_QUERY_PARAM = "_source";

  @Override
  public Class<ExplanationOfBenefit> getResourceType() {
    return ExplanationOfBenefit.class;
  }

  /**
   * Returns a {@link ExplanationOfBenefit} by its ID.
   *
   * @param fhirId FHIR ID
   * @param request HTTP request details
   * @param requestDetails different HTTP request details
   * @return patient
   */
  @Read
  public ExplanationOfBenefit find(
      @IdParam final IdType fhirId,
      final HttpServletRequest request,
      final RequestDetails requestDetails) {

    var includeTaxNumbers =
        FhirInputConverter.parseBooleanHeader(requestDetails, INCLUDE_TAX_NUMBERS_HEADER);
    var samhsaFilterMode = getFilterModeForRequest(request, SamhsaSearchIntent.UNSPECIFIED);
    var queryProfile = getQueryProfile(requestDetails);
    var options =
        ClaimFilterOptions.builder()
            .samhsaFilterMode(samhsaFilterMode)
            .includeTaxNumber(includeTaxNumbers.orElse(false))
            .queryProfile(queryProfile)
            .build();

    var eob = eobHandler.find(FhirInputConverter.toLong(fhirId), options);
    return eob.orElseThrow(() -> new ResourceNotFoundException(fhirId));
  }

  /**
   * Search for claims data by bene.
   *
   * @param patient patient
   * @param count record count
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param startIndex start index
   * @param offset offset
   * @param tag tags to filter by
   * @param type claim type to filter by
   * @param outcome outcome to filter by
   * @param source claim source to filter by
   * @param security security to filter SAMHSA by
   * @param profile profile
   * @param request HTTP request details
   * @param requestDetails HAPI FHIR request details
   * @return bundle
   */
  @Search
  public Bundle searchByPatient(
      @RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) final ReferenceParam patient,
      @Count final Integer count,
      @OptionalParam(name = SERVICE_DATE) final DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated,
      @OptionalParam(name = START_INDEX) final NumberParam startIndex,
      @Offset final Integer offset,
      @OptionalParam(name = Constants.PARAM_TAG) final TokenAndListParam tag,
      @OptionalParam(name = TYPE) final TokenAndListParam type,
      @OptionalParam(name = OUTCOME) final TokenAndListParam outcome,
      @OptionalParam(name = Constants.PARAM_SOURCE) final TokenAndListParam source,
      @OptionalParam(name = Constants.PARAM_SECURITY) final TokenAndListParam security,
      @OptionalParam(name = Constants.PARAM_PROFILE) final TokenAndListParam profile,
      final HttpServletRequest request,
      final RequestDetails requestDetails) {

    var includeTaxNumbers =
        FhirInputConverter.parseBooleanHeader(requestDetails, INCLUDE_TAX_NUMBERS_HEADER);
    var tagCriteria = FhirInputConverter.parseTagParameter(tag);
    var claimTypeCodes = FhirInputConverter.getClaimTypeCodesForType(type);
    var outcomeCriteria = FhirInputConverter.parseOutcomeParameter(outcome);
    var samhsaSearchIntent = FhirInputConverter.parseSecurityParameter(security);
    var queryProfile = getQueryProfile(requestDetails);

    var options =
        ClaimFilterOptions.builder()
            .samhsaFilterMode(getFilterModeForRequest(request, samhsaSearchIntent))
            .includeTaxNumber(includeTaxNumbers.orElse(false))
            .queryProfile(queryProfile)
            .build();

    var criteria =
        new ClaimSearchCriteria(
            FhirInputConverter.toLong(patient, "Patient"),
            FhirInputConverter.toDateTimeRange(serviceDate),
            FhirInputConverter.toDateTimeRange(lastUpdated),
            Optional.ofNullable(count),
            // we will support both offset and startIndex for now, but they can't be used together.
            // If both are provided, offset will take precedence
            Optional.ofNullable(offset).or(() -> FhirInputConverter.toIntOptional(startIndex)),
            tagCriteria,
            claimTypeCodes,
            outcomeCriteria,
            FhirInputConverter.parseSourceParameter(source),
            queryProfile);

    return eobHandler.searchByBene(criteria, options, Optional.of(requestDetails));
  }

  /**
   * Search for claims data by FHIR ID.
   *
   * @param fhirIds FHIR IDs
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param requestDetails request Details object
   * @param request HTTP request details
   * @param outcome outcome to filter by
   * @param source claim source to filter by
   * @param profile profile
   * @return bundle
   */
  @Search
  public Bundle searchById(
      @RequiredParam(name = ExplanationOfBenefit.SP_RES_ID) final TokenAndListParam fhirIds,
      @OptionalParam(name = SERVICE_DATE) final DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated,
      final HttpServletRequest request,
      final RequestDetails requestDetails,
      @OptionalParam(name = OUTCOME) final TokenAndListParam outcome,
      @OptionalParam(name = Constants.PARAM_SOURCE) final TokenAndListParam source,
      @OptionalParam(name = Constants.PARAM_PROFILE) final TokenAndListParam profile) {

    var includeTaxNumbers =
        FhirInputConverter.parseBooleanHeader(requestDetails, INCLUDE_TAX_NUMBERS_HEADER);
    var samhsaFilterMode = getFilterModeForRequest(request, SamhsaSearchIntent.UNSPECIFIED);
    var queryProfile = getQueryProfile(requestDetails);
    var options =
        ClaimFilterOptions.builder()
            .samhsaFilterMode(samhsaFilterMode)
            .includeTaxNumber(includeTaxNumbers.orElse(false))
            .queryProfile(queryProfile)
            .build();

    var searchCriteria =
        new ClaimIdSearchCriteria(
            FhirInputConverter.toLongList(fhirIds),
            FhirInputConverter.toDateTimeRange(serviceDate),
            FhirInputConverter.toDateTimeRange(lastUpdated),
            FhirInputConverter.parseOutcomeParameter(outcome),
            FhirInputConverter.parseSourceParameter(source),
            queryProfile);

    return eobHandler.searchById(searchCriteria, options);
  }

  private SamhsaFilterMode getFilterModeForRequest(
      HttpServletRequest request, SamhsaSearchIntent samhsaSearchIntent) {
    final var certAlias = certificateUtil.getAliasAttribute(request);
    final var samhsaAllowedCertificateAliases = configuration.getSamhsaAllowedCertificateAliases();
    final var authorized =
        certAlias.isPresent() && samhsaAllowedCertificateAliases.contains(certAlias.get());

    if (!authorized) {
      return SamhsaFilterMode.EXCLUDE;
    }
    // authorized, defer to security param
    return switch (samhsaSearchIntent) {
      case ONLY_SAMHSA -> SamhsaFilterMode.ONLY_SAMHSA;
      case EXCLUDE_SAMHSA -> SamhsaFilterMode.EXCLUDE;
      case UNSPECIFIED -> SamhsaFilterMode.INCLUDE;
    };
  }

  private QueryProfile getQueryProfile(RequestDetails requestDetails) {
    if (requestDetails == null || requestDetails.getParameters() == null) {
      return QueryProfile.CMS;
    }
    String[] profiles = requestDetails.getParameters().get("_profile");
    if (profiles != null && profiles.length > 0) {
      for (String p : profiles) {
        // Strip FHIR version suffix (|x.x.x) if present so bare and versioned URLs both match.
        // Use contains() for URL patterns in case the pipe is encoded differently by the client.
        String normalized = p.contains("|") ? p.substring(0, p.lastIndexOf('|')) : p;
        if ("Basis".equalsIgnoreCase(normalized)
            || normalized.contains("C4BB-ExplanationOfBenefit-Pharmacy-Basis")) {
          return QueryProfile.BASIS;
        } else if ("Regular".equalsIgnoreCase(normalized)
            || normalized.contains("C4BB-ExplanationOfBenefit-Pharmacy-Regular")) {
          return QueryProfile.REGULAR;
        } else if ("CMS".equalsIgnoreCase(normalized)
            || normalized.contains("C4BB-ExplanationOfBenefit-Pharmacy-CMS")
            || normalized.contains("CMS-ExplanationOfBenefit-Pharmacy")) {
          return QueryProfile.CMS;
        }
      }
    }
    return QueryProfile.CMS;
  }
}
