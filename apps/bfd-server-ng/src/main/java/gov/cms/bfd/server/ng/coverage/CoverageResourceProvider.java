package gov.cms.bfd.server.ng.coverage; // New package for V3 coverage

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.server.ng.input.FhirInputConverter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
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
   * Reads Coverage resource by its ID. The ID format is expected to be {part}-{bene_sk}.
   *
   * @param coverageId The FHIR ID, which includes the composite ID {part}-{bene_sk}.
   * @return The requested {@link Coverage} resource.
   * @throws ResourceNotFoundException if the coverage resource is not found.
   * @throws InvalidRequestException if the ID format is invalid.
   */
  @Read
  public Coverage read(@IdParam final IdType coverageId) {
    // todo: this would need to be a bundle, check if fine
    var compositeId = FhirInputConverter.toCoverageCompositeId(coverageId);
    var coverages = coverageHandler.readCoverage(compositeId);
    return coverages.stream()
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException(coverageId));
  }

  /**
   * Searches for Coverage resources by their logical ID (_id). The _id for Coverage is a composite
   * ID like "part-a-beneSk".
   *
   * @param coverageId The _id search parameter (composite ID string).
   * @param lastUpdated The _lastUpdated search parameter.
   * @return A Bundle of Coverage resources.
   */
  @Search
  public Bundle searchByLogicalId(
      @RequiredParam(name = Coverage.SP_RES_ID) final IdType coverageId,
      @OptionalParam(name = Coverage.SP_RES_LAST_UPDATED) final DateRangeParam lastUpdated) {

    var compositeId = FhirInputConverter.toCoverageCompositeId(coverageId);

    return coverageHandler.searchByCoverageId(
        compositeId, FhirInputConverter.toDateTimeRange(lastUpdated));
  }

  /**
   * Searches for Coverage resources by beneficiary reference.
   *
   * @param beneficiaryParam The beneficiary search parameter (bene_sk)
   * @param lastUpdated The _lastUpdated search parameter.
   * @return A Bundle of Coverage resources.
   */
  @Search
  public Bundle searchByBeneficiary(
      @RequiredParam(name = Coverage.SP_BENEFICIARY) final ReferenceParam beneficiaryParam,
      @OptionalParam(name = Coverage.SP_RES_LAST_UPDATED) final DateRangeParam lastUpdated) {

    var beneSk = FhirInputConverter.toLong(new IdType(beneficiaryParam.getValue()));

    return coverageHandler.searchByBeneficiary(
        beneSk, FhirInputConverter.toDateTimeRange(lastUpdated));
  }
}
