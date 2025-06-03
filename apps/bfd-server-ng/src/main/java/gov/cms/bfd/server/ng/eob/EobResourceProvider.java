package gov.cms.bfd.server.ng.eob;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EobResourceProvider implements IResourceProvider {
  private EobHandler eobHandler;

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
}
