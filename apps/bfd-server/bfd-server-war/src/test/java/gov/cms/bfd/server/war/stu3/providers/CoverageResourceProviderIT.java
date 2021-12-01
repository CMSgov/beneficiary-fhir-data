package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider}. */
public final class CoverageResourceProviderIT {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageResourceProviderIT.class);

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called once to make sure
   * that any existing data is deleted from the tables before running the test suite.
   */
  @BeforeClass
  public static void cleanupDatabaseBeforeTestSuite() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for {@link Beneficiary}-derived {@link Coverage}s that do exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void readCoveragesForExistingBeneficiary() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
            .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary))
            .execute();
    CoverageTransformerTest.assertPartAMatches(beneficiary, partACoverage);

    Coverage partBCoverage =
        fhirClient
            .read()
            .resource(Coverage.class)
            .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary))
            .execute();
    CoverageTransformerTest.assertPartBMatches(beneficiary, partBCoverage);

    Coverage partDCoverage =
        fhirClient
            .read()
            .resource(Coverage.class)
            .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary))
            .execute();
    CoverageTransformerTest.assertPartDMatches(beneficiary, partDCoverage);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for {@link Beneficiary}-derived {@link Coverage}s that do not exist in the DB
   * (with both positive and negative IDs).
   */
  @Test
  public void readCoveragesForMissingBeneficiary() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    // No data is loaded, so these should return nothing.
    ResourceNotFoundException exception;

    exception = null;
    try {
      fhirClient
          .read()
          .resource(Coverage.class)
          .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, "1234"))
          .execute();
    } catch (ResourceNotFoundException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);

    exception = null;
    try {
      fhirClient
          .read()
          .resource(Coverage.class)
          .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_B, "1234"))
          .execute();
    } catch (ResourceNotFoundException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);

    // Tests negative ID will pass regex pattern for valid coverageId.
    exception = null;
    try {
      fhirClient
          .read()
          .resource(Coverage.class)
          .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_D, "-1234"))
          .execute();
    } catch (ResourceNotFoundException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)}
   * works as expected for {@link Beneficiary}-derived {@link Coverage}s that has an invalid {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#IdParam} parameter.
   */
  @Test
  public void readCoveragesForInvalidIdParam() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    // Parameter is invalid, should throw exception
    InvalidRequestException exception;

    exception = null;
    try {
      fhirClient
          .read()
          .resource(Coverage.class)
          .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, "1?234"))
          .execute();
    } catch (InvalidRequestException e) {
      exception = e;
    }
    Assert.assertNotNull(exception);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#searchByBeneficiary(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Beneficiary} that does exist in the DB.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchByExistingBeneficiary() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(MedicareSegment.values().length, searchResults.getTotal());

    /*
     * Verify that no paging links exist within the bundle.
     */
    Assert.assertNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected Coverages (one for every
     * MedicareSegment) is present and looks correct.
     */

    Coverage partACoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_A.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartAMatches(beneficiary, partACoverageFromSearchResult);

    Coverage partBCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_B.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartBMatches(beneficiary, partBCoverageFromSearchResult);

    Coverage partDCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_D.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartDMatches(beneficiary, partDCoverageFromSearchResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#searchByBeneficiary(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Beneficiary} that does exist in the DB, with paging.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchByExistingBeneficiaryWithPaging() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .count(4)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(MedicareSegment.values().length, searchResults.getTotal());

    /*
     * Verify that only the first and last paging links exist, since there should
     * only be one page.
     */
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_FIRST));
    Assert.assertNull(searchResults.getLink(Constants.LINK_NEXT));
    Assert.assertNull(searchResults.getLink(Constants.LINK_PREVIOUS));
    Assert.assertNotNull(searchResults.getLink(Constants.LINK_LAST));

    /*
     * Verify that each of the expected Coverages (one for every MedicareSegment) is
     * present and looks correct.
     */

    Coverage partACoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_A.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartAMatches(beneficiary, partACoverageFromSearchResult);

    Coverage partBCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_B.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartBMatches(beneficiary, partBCoverageFromSearchResult);

    Coverage partDCoverageFromSearchResult =
        searchResults.getEntry().stream()
            .filter(e -> e.getResource() instanceof Coverage)
            .map(e -> (Coverage) e.getResource())
            .filter(
                c -> TransformerConstants.COVERAGE_PLAN_PART_D.equals(c.getGrouping().getSubPlan()))
            .findFirst()
            .get();
    CoverageTransformerTest.assertPartDMatches(beneficiary, partDCoverageFromSearchResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#searchByBeneficiary(ca.uhn.fhir.rest.param.ReferenceParam)}
   * works as expected for a {@link Beneficiary} that does not exist in the DB.
   */
  @Test
  public void searchByMissingBeneficiary() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

    // No data is loaded, so this should return 0 matches.
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId("1234")))
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchResults);
    Assert.assertEquals(0, searchResults.getTotal());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider#searchByBeneficiary} works as
   * expected for a search with a lastUpdated value.
   */
  @Test
  public void searchWithLastUpdated() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();

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
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
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
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .lastUpdated(inBoundsRange)
            .returnBundle(Bundle.class)
            .execute();

    Assert.assertNotNull(searchInBoundsResults);
    Assert.assertEquals(MedicareSegment.values().length, searchInBoundsResults.getTotal());

    Date hourAgoDate = Date.from(Instant.now().minusSeconds(3600));
    DateRangeParam outOfBoundsRange = new DateRangeParam(hourAgoDate, secondsAgoDate);
    Bundle searchOutOfBoundsResult =
        fhirClient
            .search()
            .forResource(Coverage.class)
            .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
            .lastUpdated(outOfBoundsRange)
            .returnBundle(Bundle.class)
            .execute();
    Assert.assertNotNull(searchOutOfBoundsResult);
    Assert.assertEquals(0, searchOutOfBoundsResult.getTotal());
  }
}
