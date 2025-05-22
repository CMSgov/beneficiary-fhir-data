package gov.cms.bfd.server.ng.coverage; // New package for V3 coverage

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.springframework.stereotype.Component;

/** FHIR endpoints for the Coverage resource. */
@RequiredArgsConstructor
@Component
public class CoverageResourceProvider implements IResourceProvider {

  private final CoverageHandler coverageHandler;

  @Override
  public Class<Coverage> getResourceType() {
    return Coverage.class;
  }

  /**
   * Handles the FHIR read operation for a Coverage resource by its ID. The ID format is expected to
   * be {part}-{bene_sk}.
   *
   * @param coverageId The FHIR ID, which includes the composite ID {part}-{bene_sk}.
   * @return The requested {@link Coverage} resource.
   * @throws ResourceNotFoundException if the coverage resource is not found.
   * @throws InvalidRequestException if the ID format is invalid.
   */
  @Read
  public Coverage read(@IdParam final IdType coverageId) {
    CoverageCompositeId parsedCoverageId = FhirInputConverter.toCoverageCompositeId(coverageId);

    // Call the handler with the parsed and validated ID components.
    return coverageHandler
        .readCoverage(parsedCoverageId, coverageId.getIdPart())
        .orElseThrow(() -> new ResourceNotFoundException(coverageId));
  }
}
