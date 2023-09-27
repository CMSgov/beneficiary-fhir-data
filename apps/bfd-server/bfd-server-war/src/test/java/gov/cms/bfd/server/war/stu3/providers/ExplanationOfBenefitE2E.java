package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.server.war.ExplanationOfBenefitE2EBase;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Endpoint end-to-end test for the V1 explanation of benefits endpoint. Most test logic should be
 * placed in {@link ExplanationOfBenefitE2EBase} to be shared, unless there are version-specific
 * paths or functionality to test.
 *
 * <p>Test methods are placed here as a development convenience; while the suites can inherit their
 * tests just fine from the parent if annotated there, they are hard to run in an IDE. Test methods
 * are placed here and call to the super logic to aid in local development and allow for easy
 * running of individual tests.
 */
public class ExplanationOfBenefitE2E extends ExplanationOfBenefitE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (eobEndpoint == null) {
      eobEndpoint = baseServerUrl + "/v1/fhir/ExplanationOfBenefit/";
    }
  }

  /**
   * Test eob by patient id returns tax numbers for applicable claim types when IncludeTaxNumbers =
   * true.
   */
  @Test
  public void testEobByPatientIdWithIncludeTaxNumbersTrueExpectTaxNumbers() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    // IncludeTaxNumbers is a header, so added below in restAssured API
    String requestString = eobEndpoint + "?patient=" + patientId;
    CarrierClaim carrierClaim = getClaim(loadedData, CarrierClaim.class);
    DMEClaim dmeClaim = getClaim(loadedData, DMEClaim.class);

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .given()
        .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true")
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .body("total", equalTo(8))
        /* Check there are tax numbers on applicable claims; carrier and DME
        Tax num value (for v1) is found in the eob.careTeam[N].provider.identifier[N].value (for
        the appropriate url) */
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.careTeam.find { it.provider.identifier.system == 'http://terminology.hl7.org/CodeSystem/v2-0203' }.provider.identifier.value",
            equalTo(dmeClaim.getLines().get(0).getProviderTaxNumber()))
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.careTeam.find { it.provider.identifier.system == 'http://terminology.hl7.org/CodeSystem/v2-0203' }.provider.identifier.value",
            equalTo(carrierClaim.getLines().get(0).getProviderTaxNumber()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that an EOB Carrier claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadEobCarrierIdExpectClmIdInResponse() {
    super.testReadEobCarrierIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB DME claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadDmeEobIdExpectClmIdInResponse() {
    super.testReadDmeEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB HHA claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadHhaEobIdExpectClmIdInResponse() {
    super.testReadHhaEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB Hospice claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadHospiceEobIdExpectClmIdInResponse() {
    super.testReadHospiceEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB Inpatient claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadInpatientEobIdExpectClmIdInResponse() {
    super.testReadInpatientEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB Outpatient claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadOutpatientEobIdExpectClmIdInResponse() {
    super.testReadOutpatientEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB PDE claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadPdeEobIdExpectClmIdInResponse() {
    super.testReadPdeEobIdExpectClmIdInResponse();
  }

  /**
   * Verifies that an EOB SNF claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  public void testReadSnfEobIdExpectClmIdInResponse() {
    super.testReadSnfEobIdExpectClmIdInResponse();
  }

  /** Test that an EOB read request returns a 404 if the eob Id does not match any claims. */
  @Test
  public void testReadForMissingEobExpect404() {
    super.testReadForMissingEobExpect404();
  }

  /**
   * Test that an EOB read request returns a 404 if the eob Id does not match any claims and is
   * negative. Tests negative ID will pass regex pattern.
   */
  @Test
  public void testReadForMissingNegativeEobExpect404() {
    super.testReadForMissingNegativeEobExpect404();
  }

  /** Verifies that EOB read returns a 400 when a non-numeric id is passed in. */
  @Test
  public void testReadEobForNonNumericClmIdExpect400() {
    super.testReadEobForNonNumericClmIdExpect400();
  }

  /** Verifies that the EOB returns a 400 when called with a negative count parameter. */
  @Test
  public void testEobByPatientIdWithNegativeCountExpect400() {
    super.testEobByPatientIdWithNegativeCountExpect400();
  }

  /**
   * Verifies that the EOB returns a 200 with the expected DD paths for each claim type returned.
   */
  @Test
  public void testEobByPatientIdWithValidPatientIdExpectValidEob() {
    super.testEobByPatientIdWithValidPatientIdExpectValidEob();
  }

  /**
   * Test that when we get a valid EOB by patient ID response, if we did not initially request
   * paging we can still request paging into the results by using startIndex.
   */
  @Test
  public void testEobByPatientIdWithValidNonPagingResponseExpectCanAddPagingToResults() {
    super.testEobByPatientIdWithValidNonPagingResponseExpectCanAddPagingToResults();
  }

  /**
   * Verifies that findByPatient returns no paging for a patient that exists in the DB, with paging
   * on a page size of 0.
   *
   * <p>This tests a HAPI-FHIR bug and is therefore an E2E test; if/when this bug is fixed, this
   * could be replaced by a unit test.
   *
   * <p>According to the FHIR spec, paging for _count=0 should not return any claim entries in the
   * bundle, but instead just a total for the number of entries that match the search criteria. This
   * functionality does no work currently (see https://github.com/jamesagnew/hapi-fhir/issues/1074)
   * and so for now paging with _count=0 should behave as though paging was not requested.
   */
  @Test
  public void testEobByPatientIdWithPageSizeZeroReturnsNoPaging() {
    super.testEobByPatientIdWithPageSizeZeroReturnsNoPaging();
  }

  /**
   * Tests that when searching for EOB by patient id and an unrecognized request param results in a
   * 400 http code.
   */
  @Test
  public void testEobByPatientIdWithBadRequestParamExpect400() {
    super.testEobByPatientIdWithBadRequestParamExpect400();
  }

  /**
   * Tests that when searching for EOB by patient id and a number of pages, we can cycle through all
   * pages and all expected pages exist. Also makes sure when we have a page count of higher than
   * the remaining entries at the end, we do not encounter an out-of-bounds exception.
   */
  @Test
  public void testEobByPatientIdWithPagingExpectAllPages() {
    super.testEobByPatientIdWithPagingExpectAllPages();
  }

  /**
   * Tests that when searching for EOB by patient id and using startIndex, returns paging results,
   * as startIndex (alongside _count) is one of the indicators to return paged results.
   */
  @Test
  public void testEobByPatientIdWithOnlyStartIndexExpectingPaging() {
    super.testEobByPatientIdWithOnlyStartIndexExpectingPaging();
  }

  /**
   * Verifies that findByPatient works as expected for a {@link Patient} that does exist in the DB,
   * with a page size of 50 with fewer (8) results.
   */
  @Test
  public void searchForEobsWithLargePageSizesOnFewerResults() {
    super.searchForEobsWithLargePageSizesOnFewerResults();
  }

  /**
   * Verify that EOB by patient id throws a 400 error when the paging start (startIndex) is set
   * higher than the maximum number of results.
   */
  @Test
  public void testEobByPatientIdWithPagingStartBeyondMaxExpect400() {
    super.testEobByPatientIdWithPagingStartBeyondMaxExpect400();
  }

  /**
   * Verify that EOB by patient id throws a 400 error when the paging start (startIndex) is set to
   * the maximum number of results, since the highest index must be less than the number of results
   * as a 0-based index.
   */
  @Test
  public void testEobByPatientIdWithPagingStartSetToMaxResultsExpect400() {
    super.testEobByPatientIdWithPagingStartSetToMaxResultsExpect400();
  }

  /**
   * Verify that EOB by patient id does not error when the paging start (startIndex) is set to be
   * equal to one less than the maximum number of results.
   */
  @Test
  public void testEobByPatientIdWithPagingStartOneLessThanMaxExpect200() {
    super.testEobByPatientIdWithPagingStartOneLessThanMaxExpect200();
  }

  /**
   * Test that searching by patient id with samhsa filtering = true does filter the samhsa data from
   * the response.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering() {
    super.testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering();
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is set
   * to false.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaFalseExpectNoFiltering() {
    super.testEobByPatientIdWithExcludeSamhsaFalseExpectNoFiltering();
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is not
   * explicitly set (should default to false internally).
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaDefaultExpectNoFiltering() {
    super.testEobByPatientIdWithExcludeSamhsaDefaultExpectNoFiltering();
  }

  /**
   * Ensure nothing unusual happens when we excludeSAMHSA = false and the result has non-samhsa
   * data.
   */
  @Test
  public void testEobByPatientIdForNonSamhsaEobsWithExcludeSamhsaTrueExpectNoError() {
    super.testEobByPatientIdForNonSamhsaEobsWithExcludeSamhsaTrueExpectNoError();
  }

  /**
   * Test eob by patient id does not return tax numbers for applicable claim types when
   * IncludeTaxNumbers = false.
   */
  @Test
  public void testEobByPatientIdWithIncludeTaxNumbersFalseExpectNoTaxNumbers() {
    super.testEobByPatientIdWithIncludeTaxNumbersFalseExpectNoTaxNumbers();
  }

  /**
   * Validates that passing patient id and claim type returns only the requested claim type in the
   * response.
   */
  @Test
  public void testEobByPatientIdAndClaimTypeExpectOneResult() {
    super.testEobByPatientIdAndClaimTypeExpectOneResult();
  }

  /**
   * Tests EOB search by patient id with various lastUpdated values returns the expected number of
   * results for that query.
   */
  @Test
  public void testEobByPatientIdWithLastUpdated() {
    super.testEobByPatientIdWithLastUpdated();
  }

  /**
   * Verifies that findByPatient works as with a lastUpdated parameter after yesterday and
   * pagination links work and contain lastUpdated.
   */
  @Test
  public void searchEobByPatientIdWithLastUpdatedAndPagination() {
    super.searchEobByPatientIdWithLastUpdatedAndPagination();
  }

  /**
   * Verifies that the EOB findByPatient logic falls back to using the correct dates for querying
   * the database when lastUpdated is not set / null.
   */
  @Test
  public void searchEobByPatientIdWhenNullLastUpdatedExpectFallback() {
    super.searchEobByPatientIdWhenNullLastUpdatedExpectFallback();
  }

  /**
   * Verifies that search by patient id returns the expected data when filtering by service date.
   */
  @Test
  public void searchEobByPatientIdWithServiceDate() {
    super.searchEobByPatientIdWithServiceDate();
  }

  /**
   * Verify that an empty bundle is returned when pagination is requested but no results are
   * returned. Normally this would return a 400 since the default startIndex is equal to the number
   * of results, but we make a special exception for empty returns since there's nothing to paginate
   * anyway.
   */
  @Test
  public void searchEobByPatientIdWithNoResultsAndPaginationRequestedExpect200() {
    super.searchEobByPatientIdWithNoResultsAndPaginationRequestedExpect200();
  }
}
