package gov.cms.bfd.server.ng.eob;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/** FHIR endpoints for the ExplanationOfBenefit resource. */
@RequiredArgsConstructor
@Component
public class EobResourceProvider implements IResourceProvider {
  private final EobHandler eobHandler;
  private static final String SERVICE_DATE = "service-date";
  private static final String START_INDEX = "startIndex";

  @Override
  public Class<ExplanationOfBenefit> getResourceType() {
    return ExplanationOfBenefit.class;
  }

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  @Read
  public ExplanationOfBenefit find(@IdParam final IdType fhirId) {
    var eob = eobHandler.find(FhirInputConverter.toLong(fhirId));
    return eob.orElseThrow(() -> new ResourceNotFoundException(fhirId));
  }

  @Search
  public Bundle searchByPatient(
      @RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) final ReferenceParam patient,
      @Count final Integer count,
      @OptionalParam(name = SERVICE_DATE) DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated,
      @OptionalParam(name = START_INDEX) NumberParam startIndex) {

    return eobHandler.searchByBene(
        FhirInputConverter.toLong(patient, "Patient"),
        Optional.ofNullable(count),
        FhirInputConverter.toDateTimeRange(serviceDate),
        FhirInputConverter.toDateTimeRange(lastUpdated),
        FhirInputConverter.toIntOptional(startIndex));
  }

  @Search
  public Bundle searchById(
      @RequiredParam(name = ExplanationOfBenefit.SP_RES_ID) final IdType fhirId,
      @OptionalParam(name = SERVICE_DATE) DateRangeParam serviceDate,
      @OptionalParam(name = ExplanationOfBenefit.SP_RES_LAST_UPDATED)
          final DateRangeParam lastUpdated) {
    return eobHandler.searchById(
        FhirInputConverter.toLong(fhirId),
        FhirInputConverter.toDateTimeRange(serviceDate),
        FhirInputConverter.toDateTimeRange(lastUpdated));
  }
}
