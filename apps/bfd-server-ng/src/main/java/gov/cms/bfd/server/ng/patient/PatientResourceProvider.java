package gov.cms.bfd.server.ng.patient;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.NumberParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.types.FhirInputConverter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** FHIR endpoints for the Patient resource. */
@Validated
@RequiredArgsConstructor
@Component
public class PatientResourceProvider implements IResourceProvider {

  @Override
  public Class<Patient> getResourceType() {
    return Patient.class;
  }

  private final PatientHandler patientHandler;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  @Read
  public Patient find(@IdParam final IdType fhirId) {
    var patient = patientHandler.find(FhirInputConverter.toLong(fhirId));
    if (patient.isEmpty()) {
      throw new ResourceNotFoundException(fhirId);
    }
    return patient.get();
  }

  /**
   * Searches for a Patient by ID.
   *
   * @param fhirId FHIR ID
   * @param lastUpdated last updated datetime
   * @return bundle
   */
  @Search
  public Bundle searchByLogicalId(
      @RequiredParam(name = Patient.SP_RES_ID) final IdType fhirId,
      @OptionalParam(name = Patient.SP_RES_LAST_UPDATED) final DateRangeParam lastUpdated) {

    var patient =
        patientHandler.searchByLogicalId(
            FhirInputConverter.toLong(fhirId), FhirInputConverter.toDateTimeRange(lastUpdated));
    var bundle = new Bundle();
    patient.ifPresent(p -> bundle.addEntry(new Bundle.BundleEntryComponent().setResource(p)));
    return bundle;
  }

  /**
   * Searches for a Patient by identifier.
   *
   * @param identifier identifier
   * @param lastUpdated last updated datetime
   * @param requestDetails request details
   * @return bundle
   */
  @Search
  public Bundle searchByIdentifier(
      @RequiredParam(name = Patient.SP_IDENTIFIER) final TokenParam identifier,
      @OptionalParam(name = Constants.PARAM_LASTUPDATED) final DateRangeParam lastUpdated,
      final RequestDetails requestDetails) {
    return new Bundle();
  }

  /**
   * Searches for a Patient by contract info.
   *
   * @param coverageId coverage ID
   * @param referenceYear reference year
   * @param cursor pagination cursor
   * @param count pagination count
   * @param requestDetails request details
   * @return bundle
   */
  @Search
  public Bundle searchByCoverageContract(
      @RequiredParam(name = "_has:Coverage.extension") final TokenParam coverageId,
      @OptionalParam(name = "_has:Coverage.rfrncyr") final TokenParam referenceYear,
      @OptionalParam(name = "cursor") final NumberParam cursor,
      @OptionalParam(name = Constants.PARAM_COUNT) final NumberParam count,
      final RequestDetails requestDetails) {
    return new Bundle();
  }
}
