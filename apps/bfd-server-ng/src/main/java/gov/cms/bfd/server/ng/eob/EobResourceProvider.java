package gov.cms.bfd.server.ng.eob;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.Configuration;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
import gov.cms.bfd.server.ng.claim.model.SamhsaSearchIntent;
import gov.cms.bfd.server.ng.input.ClaimSearchCriteria;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
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
  private final EobNewHandler eobHandler;
  private final CertificateUtil certificateUtil;
  private final Configuration configuration;
  private static final String SERVICE_DATE = "service-date";
  private static final String START_INDEX = "startIndex";
  private static final String TYPE = "type";

  @Override
  public Class<ExplanationOfBenefit> getResourceType() {
    return ExplanationOfBenefit.class;
  }

  /**
   * Returns a {@link ExplanationOfBenefit} by its ID.
   *
   * @param fhirId FHIR ID
   * @param request HTTP request details
   * @return patient
   */
  @Read
  public ExplanationOfBenefit find(@IdParam final IdType fhirId, final HttpServletRequest request) {
    var eob =
        eobHandler.find(
            FhirInputConverter.toLong(fhirId),
            getFilterModeForRequest(request, SamhsaSearchIntent.UNSPECIFIED));
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
   * @param tag tags to filter by
   * @param type claim type to filter by
   * @param source claim source to filter by
   * @param security security to filter SAMHSA by
   * @param request HTTP request details
   * @return bundle
   */
  @Search
  public Bundle searchByPatientNew(
      @RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) final ReferenceParam patient,
      @Count final Integer count,
      @OptionalParam(name = SERVICE_DATE) final DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated,
      @OptionalParam(name = START_INDEX) final NumberParam startIndex,
      @OptionalParam(name = Constants.PARAM_TAG) final TokenAndListParam tag,
      @OptionalParam(name = TYPE) final TokenAndListParam type,
      @OptionalParam(name = Constants.PARAM_SOURCE) final TokenAndListParam source,
      @OptionalParam(name = Constants.PARAM_SECURITY) final TokenAndListParam security,
      final HttpServletRequest request) {

    var tagCriteria = FhirInputConverter.parseTagParameter(tag);
    var claimTypeCodes = FhirInputConverter.getClaimTypeCodesForType(type);
    var samhsaSearchIntent = FhirInputConverter.parseSecurityParameter(security);

    var criteria =
        new ClaimSearchCriteria(
            FhirInputConverter.toLong(patient, "Patient"),
            FhirInputConverter.toDateTimeRange(serviceDate),
            FhirInputConverter.toDateTimeRange(lastUpdated),
            Optional.ofNullable(count),
            FhirInputConverter.toIntOptional(startIndex),
            tagCriteria,
            claimTypeCodes,
            FhirInputConverter.parseSourceParameter(source));

    return eobHandler.searchByBene(criteria, getFilterModeForRequest(request, samhsaSearchIntent));
  }

  /**
   * Search for claims data by FHIR ID.
   *
   * @param fhirId FHIR ID
   * @param serviceDate service date
   * @param lastUpdated last updated
   * @param request HTTP request details
   * @return bundle
   */
  @Search
  public Bundle searchById(
      @RequiredParam(name = ExplanationOfBenefit.SP_RES_ID) final IdType fhirId,
      @OptionalParam(name = SERVICE_DATE) final DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated,
      final HttpServletRequest request) {
    return eobHandler.searchById(
        FhirInputConverter.toLong(fhirId),
        FhirInputConverter.toDateTimeRange(serviceDate),
        FhirInputConverter.toDateTimeRange(lastUpdated),
        getFilterModeForRequest(request, SamhsaSearchIntent.UNSPECIFIED));
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
}
