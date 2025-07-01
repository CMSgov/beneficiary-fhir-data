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
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
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
    CoverageCompositeId compositeId = FhirInputConverter.toCoverageCompositeId(coverageId);
    var coverage = coverageHandler.readCoverage(compositeId, coverageId.getIdPart());
    return coverage.orElseThrow(() -> new ResourceNotFoundException(coverageId));
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

    CoverageCompositeId compositeId = FhirInputConverter.toCoverageCompositeId(coverageId);

    return coverageHandler.searchByCoverageId(
        compositeId, coverageId.getIdPart(), FhirInputConverter.toDateTimeRange(lastUpdated));
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

    if (beneficiaryParam.getResourceType() != null
        && !"Patient".equals(beneficiaryParam.getResourceType())) {
      throw new InvalidRequestException(
          "Beneficiary search parameter must be a Patient reference.");
    }

    Long beneSk = FhirInputConverter.toLong(new IdType(beneficiaryParam.getValue()));

    return coverageHandler.searchByBeneficiary(
        beneSk, FhirInputConverter.toDateTimeRange(lastUpdated));
  }
}
