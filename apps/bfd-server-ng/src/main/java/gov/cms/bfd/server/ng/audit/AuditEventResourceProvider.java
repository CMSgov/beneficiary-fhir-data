package gov.cms.bfd.server.ng.audit;

import ca.uhn.fhir.rest.annotation.Count;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

/** Resource Provider for AuditEvent Resource. */
@RequiredArgsConstructor
@Component
public class AuditEventResourceProvider implements IResourceProvider {

  private static final String LAST_INDEX = "lastIndex";

  private final AuditEventHandler auditEventHandler;

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return AuditEvent.class;
  }

  /**
   * Returns a {@link AuditEvent} by its ID.
   *
   * @param fhirId FHIR ID
   * @return AuditEvent
   */
  @Read
  public AuditEvent find(@IdParam final IdType fhirId) {
    AuditEventId id;
    try {
      id = AuditEventId.parse(fhirId.getIdPart());
    } catch (IllegalArgumentException _) {
      throw new ResourceNotFoundException(fhirId);
    }
    return auditEventHandler.getAuditEventById(id);
  }

  /**
   * Searches for AuditEvent resources by PatientId. ID like "part-a-beneSk".
   *
   * @param patient The entity search parameter (composite ID string Patient/{patientId}).
   * @param count limit for results
   * @param lastIndex lastIndex for previous result bundle
   * @param requestDetails HAPI FHIR request details
   * @return A Bundle of AuditEvent resources.
   */
  @Search
  public Bundle searchByPatient(
      @RequiredParam(name = AuditEvent.SP_ENTITY) final ReferenceParam patient,
      @Count final Integer count,
      @OptionalParam(name = LAST_INDEX) final String lastIndex,
      final RequestDetails requestDetails) {
    return auditEventHandler.getAuditEventsByBeneficiary(
        new AuditPatientSearchCriteria(
            FhirInputConverter.toLong(patient, "Patient"),
            Optional.ofNullable(count),
            Optional.ofNullable(lastIndex)),
        requestDetails);
  }

  /**
   * Search for claims data by FHIR ID.
   *
   * @param fhirIds FHIR IDs
   * @return bundle
   */
  @Search
  public Bundle searchById(
      @RequiredParam(name = IAnyResource.SP_RES_ID) final TokenAndListParam fhirIds) {
    return auditEventHandler.getAuditEventsById(
        FhirInputConverter.toStringList(fhirIds).stream().map(AuditEventId::parse).toList());
  }
}
