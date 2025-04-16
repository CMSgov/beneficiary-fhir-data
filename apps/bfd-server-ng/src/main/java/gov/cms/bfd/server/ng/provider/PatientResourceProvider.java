package gov.cms.bfd.server.ng.provider;

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
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/** FHIR endpoints for the Patient resource. */
@RequiredArgsConstructor
@Component
public class PatientResourceProvider implements IResourceProvider {

  @Override
  public Class<Patient> getResourceType() {
    return Patient.class;
  }

  private final BeneficiaryRepository beneficiaryRepository;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @param requestDetails requestDetails
   * @return patient
   */
  @Read
  public Patient find(@IdParam final IdType fhirId, final RequestDetails requestDetails) {

    var patient = beneficiaryRepository.getById(fhirId.getIdPartAsLong()).toFhir();
    var ids = beneficiaryRepository.getHistoricalIdentities(fhirId.getIdPartAsLong());
    for (var id : ids) {
      var idFhir = id.toFhir(patient);
      patient.addIdentifier(idFhir.getMbi());
      idFhir.getLink().ifPresent(patient::addLink);
    }
    return patient;
  }

  /**
   * Searches for a Patient by ID.
   *
   * @param fhirId FHIR ID
   * @param lastUpdated last updated datetime
   * @param requestDetails request details
   * @return bundle
   */
  @Search
  public Bundle searchByLogicalId(
      @RequiredParam(name = Patient.SP_RES_ID) final TokenParam fhirId,
      @OptionalParam(name = Patient.SP_RES_LAST_UPDATED) final DateRangeParam lastUpdated,
      final RequestDetails requestDetails) {
    return new Bundle();
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
