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
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.Configuration;
import gov.cms.bfd.server.ng.SamhsaFilterMode;
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
public class EobResourceProvider implements IResourceProvider {
  private final EobHandler eobHandler;
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
    var eob = eobHandler.find(FhirInputConverter.toLong(fhirId), getFilterModeForRequest(request));
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
   * @param tag tag to filter by (e.g., Adjudicated status)
   * @param type claim type to filter by
   * @param request HTTP request details
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
      @OptionalParam(name = Constants.PARAM_TAG) final TokenParam tag,
      @OptionalParam(name = TYPE) final TokenAndListParam type,
      final HttpServletRequest request) {

    var sourceIds = FhirInputConverter.getSourceIdsForTagCode(tag);
    var claimTypeCodes = FhirInputConverter.getClaimTypeCodes(type);

    return eobHandler.searchByBene(
        FhirInputConverter.toLong(patient, "Patient"),
        Optional.ofNullable(count),
        FhirInputConverter.toDateTimeRange(serviceDate),
        FhirInputConverter.toDateTimeRange(lastUpdated),
        FhirInputConverter.toIntOptional(startIndex),
        sourceIds,
        claimTypeCodes,
        getFilterModeForRequest(request));
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
        getFilterModeForRequest(request));
  }

  private SamhsaFilterMode getFilterModeForRequest(HttpServletRequest request) {
    final var certAlias = certificateUtil.getAliasAttribute(request);
    final var samhsaAllowedCertificateAliases = configuration.getSamhsaAllowedCertificateAliases();
    return certAlias.isEmpty() || !samhsaAllowedCertificateAliases.contains(certAlias.get())
        ? SamhsaFilterMode.EXCLUDE
        : SamhsaFilterMode.INCLUDE;
  }
}
