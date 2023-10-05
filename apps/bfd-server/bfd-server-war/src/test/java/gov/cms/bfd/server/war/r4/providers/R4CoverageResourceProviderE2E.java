package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for the {@link R4CoverageResourceProvider}. */
public final class R4CoverageResourceProviderE2E extends ServerRequiredTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(R4CoverageResourceProviderE2E.class);

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} works as expected for {@link
   * Beneficiary}-derived {@link Coverage}s that do exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readCoveragesForExistingBeneficiary() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Coverage partACoverage =
        fhirClient
            .read()
            .resource(Coverage.class)
            .withId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_A, beneficiary))
            .execute();
    CoverageTransformerV2Test.assertPartAMatches(beneficiary, partACoverage);

    Coverage partBCoverage =
        fhirClient
            .read()
            .resource(Coverage.class)
            .withId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_B, beneficiary))
            .execute();
    CoverageTransformerV2Test.assertPartBMatches(beneficiary, partBCoverage);

    Coverage partDCoverage =
        fhirClient
            .read()
            .resource(Coverage.class)
            .withId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_D, beneficiary))
            .execute();
    CoverageTransformerV2Test.assertPartDMatches(beneficiary, partDCoverage);
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#searchByBeneficiary} works as expected for a
   * {@link Beneficiary} that does exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchByExistingBeneficiary() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);
    assertEquals(MedicareSegment.values().length, searchResults.getTotal());

    /*
     * Verify that no paging links exist within the bundle.
     */
    assertNull(searchResults.getLink(Constants.LINK_FIRST));
    assertNull(searchResults.getLink(Constants.LINK_NEXT));
    assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected Coverages (one for every
     * MedicareSegment) is present and looks correct.
     */

    Coverage partACoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c ->
                    c.getClass_().stream()
                        .filter(
                            cl -> TransformerConstants.COVERAGE_PLAN_PART_A.equals(cl.getValue()))
                        .findAny()
                        .isPresent())
            .findFirst()
            .get();
    CoverageTransformerV2Test.assertPartAMatches(beneficiary, partACoverageFromSearchResult);

    Coverage partBCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c ->
                    c.getClass_().stream()
                        .filter(
                            cl -> TransformerConstants.COVERAGE_PLAN_PART_B.equals(cl.getValue()))
                        .findAny()
                        .isPresent())
            .findFirst()
            .get();
    CoverageTransformerV2Test.assertPartBMatches(beneficiary, partBCoverageFromSearchResult);

    Coverage partDCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c ->
                    c.getClass_().stream()
                        .filter(
                            cl -> TransformerConstants.COVERAGE_PLAN_PART_D.equals(cl.getValue()))
                        .findAny()
                        .isPresent())
            .findFirst()
            .get();
    CoverageTransformerV2Test.assertPartDMatches(beneficiary, partDCoverageFromSearchResult);
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#searchByBeneficiary} works as expected for a
   * search with a lastUpdated value.
   */
  @Test
  public void searchWithLastUpdated() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    LOGGER.info(
        "Bundle information: database {}, first {}",
        searchResults.getMeta().getLastUpdated(),
        searchResults.getEntry().get(0).getResource().getMeta().getLastUpdated());

    Date nowDate = new Date();
    Date secondsAgoDate = Date.from(Instant.now().minusSeconds(100));

    DateRangeParam inBoundsRange =
        new DateRangeParam().setLowerBoundInclusive(secondsAgoDate).setUpperBoundExclusive(nowDate);
    LOGGER.info("Query Date Range {}", inBoundsRange);
    Bundle searchInBoundsResults =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .lastUpdated(inBoundsRange)
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchInBoundsResults);
    assertEquals(MedicareSegment.values().length, searchInBoundsResults.getTotal());

    Date hourAgoDate = Date.from(Instant.now().minusSeconds(3600));
    DateRangeParam outOfBoundsRange = new DateRangeParam(hourAgoDate, secondsAgoDate);
    Bundle searchOutOfBoundsResult =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
            .lastUpdated(outOfBoundsRange)
            .returnBundle(Bundle.class)
            .execute();
    assertNotNull(searchOutOfBoundsResult);
    assertEquals(0, searchOutOfBoundsResult.getTotal());
  }
}
