package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.primitive.DateTimeDt;
import com.google.common.collect.ImmutableList;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import io.restassured.response.Response;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** Abstract base class for RestAssured E2E tests shared between BFD versions. */
public abstract class ExplanationOfBenefitE2EBase extends ServerRequiredTest {

  /** The base eob endpoint. */
  protected String eobEndpoint;

  /**
   * Verifies that an EOB Carrier claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadEobCarrierIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.CARRIER);
  }

  /**
   * Verifies that an EOB DME claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadDmeEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.DME);
  }

  /**
   * Verifies that an EOB HHA claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadHhaEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.HHA);
  }

  /**
   * Verifies that an EOB Hospice claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadHospiceEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.HOSPICE);
  }

  /**
   * Verifies that an EOB Inpatient claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadInpatientEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.INPATIENT);
  }

  /**
   * Verifies that an EOB Outpatient claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadOutpatientEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.OUTPATIENT);
  }

  /**
   * Verifies that an EOB PDE claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadPdeEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.PDE);
  }

  /**
   * Verifies that an EOB SNF claim can be requested using the read endpoint, successfully returns a
   * 200 response, and has the claim id and a couple other EOB details present in the body.
   */
  @Test
  public void testReadSnfEobIdExpectClmIdInResponse() {
    verifySuccessfulResponseAndClaimIdFor(ClaimType.SNF);
  }

  /** Test that an EOB read request returns a 404 if the eob Id does not match any claims. */
  @Test
  public void testReadForMissingEobExpect404() {
    // Set eobId to something that will return no results
    String eobId = "carrier-9999999999";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue[0].severity", equalTo("error"))
        .body(
            "issue[0].diagnostics",
            equalTo("HAPI-0971: Resource ExplanationOfBenefit/carrier-9999999999 is not known"))
        .statusCode(404)
        .when()
        .get(eobEndpoint + eobId);
  }

  /**
   * Test that an EOB read request returns a 404 if the eob Id does not match any claims and is
   * negative. Tests negative ID will pass regex pattern.
   */
  @Test
  public void testReadForMissingNegativeEobExpect404() {
    // Set eobId to something that will return no results, and is negative
    String eobId = "carrier--9999999999";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue[0].severity", equalTo("error"))
        .body(
            "issue[0].diagnostics",
            equalTo("HAPI-0971: Resource ExplanationOfBenefit/carrier--9999999999 is not known"))
        .statusCode(404)
        .when()
        .get(eobEndpoint + eobId);
  }

  /** Verifies that EOB read returns a 400 when a non-numeric id is passed in. */
  @Test
  public void testReadEobForNonNumericClmIdExpect400() {
    String requestString = eobEndpoint + "junk";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .body()
        .body("issue[0].severity", equalTo("error"))
        .body(
            "issue[0].diagnostics",
            equalTo(
                "ExplanationOfBenefit ID pattern: 'junk' does not match expected pattern: {alphaString}-{idNumber}"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /** Verifies that the EOB returns a 400 when called with a negative count parameter. */
  @Test
  public void testEobByPatientIdWithNegativeCountExpect400() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // Its possible to add these via the API, but this is easier to liken to actual request strings
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=-10";

    given()
        .spec(requestAuth)
        .expect()
        // returns a list of issues, so we need to use hasItem here to check each one (even though
        // we only really get one item back)
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("Value for pageSize cannot be negative: -10"))
        .statusCode(400)
        .when()
        .get(requestString);

    /* This would be one of the major types of advantageous tests to use RestAssured for. We can hit the endpoint as a
     * user would, and ensure they get back the http codes/error messages we expect. */
  }

  /**
   * Verifies that the EOB returns a 200 with the expected DD paths for each claim type returned.
   */
  @Test
  public void testEobByPatientIdWithValidPatientIdExpect200() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    String requestString = eobEndpoint + "?patient=" + patientId;
    OutpatientClaim expectedOutpatientClaim = testUtils.getClaim(loadedData, OutpatientClaim.class);

    given()
        .spec(requestAuth)
        .expect()
        // our top level is a bundle
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        // the claim types of these entries should all be ExplanationOfBenefit
        .body("entry.resource.resourceType", everyItem(equalTo("ExplanationOfBenefit")))
        // Check our response has the various claim types by checking their metadata ids for
        // each claim type
        .body(
            "entry.resource.id",
            hasItems(
                containsString("hha"),
                containsString("inpatient"),
                containsString("carrier"),
                containsString("dme"),
                containsString("snf"),
                containsString("pde"),
                containsString("hospice"),
                containsString("hha")))
        // Find the outpatient claim, then find the extension for fi_doc_clm_cntl_num, then get the
        // value, compare to the loaded data value. This validates we added our extension with the
        // right path, with the right value. This is an example of a "data correctness" check.
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num' }.valueIdentifier.value",
            equalTo(expectedOutpatientClaim.getFiDocumentClaimControlNumber().orElseThrow()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Test that when we get a valid EOB by patient ID response, if we did not initially request
   * paging we can still request paging into the results by using startIndex.
   */
  @Test
  public void testEobByPatientIdWithValidNonPagingResponseExpectCanAddPagingToResults() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId;

    Response response =
        given()
            .spec(requestAuth)
            .expect()
            // we should have 8 claim type entries
            .body("entry.size()", equalTo(8))
            // Check that no paging info exists, since we didn't request it (only self should exist
            // for link)
            .body("link.size()", equalTo(1))
            .body("link.relation", hasItem("self"))
            .statusCode(200)
            .when()
            .get(requestString);

    // Get the 'self' link
    String selfLink = testUtils.getPaginationLink(response, "self");

    // Add a start index to the self link to get a subset of the results
    String indexedSelfLink = selfLink + "&startIndex=4";

    // Do a call with the new indexed link, starting at entry 4
    given()
        .spec(requestAuth)
        .expect()
        // we should have 4 claim type entries since we started at item 4/8
        .body("entry.size()", equalTo(4))
        // By adding start index we've enabled paging, so check paging was added
        // there should be 4 entries for the paging section; first, previous, last, self (we're on
        // the last page, so no next)
        .body("link.size()", equalTo(4))
        .body("link.relation", hasItems("first", "previous", "last", "self"))
        .statusCode(200)
        .when()
        .get(indexedSelfLink);
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

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=0";

    // This test is interesting. In the E2E, we have a test that checks that count=0 does
    // a specific thing (returns the result as normal with no paging). However, if we run this test,
    // we actually get
    // a 400 with an error 'Invalid pageSize 0', revealing a deficiency in our current E2E test
    // strategy (Setting the
    // count in the test client directly, as in the tests, does not seem to 1:1 map with calling
    // hapi-FHIR via REST call)
    // As an aside, pageSize _should_ be able to be 0, so the error is incorrect and should be fixed
    // in our code.

    // This is what we'd expect, testing the same thing as the E2E
    // 'searchForEobsByExistingPatientWithPageSizeZero'
    /* given()
       .spec(requestAuth)
       .expect()
       .statusCode(200)
       .log()
       .body()
       .body("entry.size()", equalTo(8))
       // Check that no paging info exists (other than self, which is always added)
       .body("link.size()", equalTo(1))
       .body("link.relation", hasItem("self"))
       .when()
       .get(requestString);
    */

    // But this is what passes
    // FUTURE: Look into supporting pageSize 0
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(400)
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("Invalid pageSize '0'"))
        .when()
        .get(requestString);
  }

  /** Verify that EOB returns a 400 when searching with an invalid bene ID. */
  @Test
  public void testEobByPatientIdWithInvalidIdReturns400() {

    String requestString = eobEndpoint + "?patient=abc";

    given()
        .spec(requestAuth)
        .expect()
        .statusCode(400)
        .body("issue.severity", hasItem("error"))
        .when()
        .get(requestString);
  }

  /**
   * Tests that when searching for EOB by patient id and an unrecognized request param results in a
   * 400 http code.
   */
  @Test
  public void testEobByPatientIdWithBadRequestParamExpect400() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&unknownParam=124";

    given().spec(requestAuth).expect().statusCode(400).when().get(requestString);
  }

  /**
   * Tests that when searching for EOB by patient id and a number of pages, we can cycle through all
   * pages and all expected pages exist. Also makes sure when we have a page count of higher than
   * the remaining entries at the end, we do not encounter an out-of-bounds exception.
   */
  @Test
  public void testEobByPatientIdWithPagingExpectAllPages() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // Make a request with paging by adding startIndex or pageSize (_count in FHIR)
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=3";

    // Page 1/3

    Response response =
        given()
            .spec(requestAuth)
            .expect()
            .log()
            .ifError()
            .body("resourceType", equalTo("Bundle"))
            // we should have 3 entries, since we set page size to 3
            .body("entry.size()", equalTo(3))
            // Our entries should contain the first three claim types (we return claim types in
            // order)
            .body(
                "entry.resource.id",
                hasItems(
                    containsString("outpatient"), containsString("dme"), containsString("hha")))
            // there should be 4 entries for the paging section; first, next, last, self
            .body("link.size()", equalTo(4))
            .body("link.relation", hasItems("first", "next", "last", "self"))
            .statusCode(200)
            .when()
            .get(requestString);

    // Get the next link, and make the call; ensure we have the right number and type of claims on
    // the next page

    // The first line gets the array of json objects inside the "link" path, then we find the one
    // that matches 'next' and get its url
    String nextLink = testUtils.getPaginationLink(response, "next");

    // Page 2/3

    response =
        given()
            .spec(requestAuth)
            .expect()
            .log()
            .ifError()
            .body("resourceType", equalTo("Bundle"))
            // we should have 3 entries, since we set page size to 3
            .body("entry.size()", equalTo(3))
            // Our entries should contain the first two claim types (we return claim types in order)
            .body(
                "entry.resource.id",
                hasItems(containsString("snf"), containsString("pde"), containsString("inpatient")))
            // there should still be the 4 entries for the paging section; first, next, last, self,
            // plus a new "previous" entry
            .body("link.size()", equalTo(5))
            .body("link.relation", hasItems("first", "next", "last", "self", "previous"))
            .statusCode(200)
            .when()
            .get(nextLink);

    // page 3/3

    nextLink = testUtils.getPaginationLink(response, "next");

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("resourceType", equalTo("Bundle"))
        // we should have 2 entries, since we only have 2 elements left in the list
        .body("entry.size()", equalTo(2))
        .body("entry.resource.id", hasItems(containsString("carrier"), containsString("hospice")))
        // expect 4 entries for the paging section; first, last, self, previous; should be no "next"
        .body("link.size()", equalTo(4))
        .body("link.relation", hasItems("first", "last", "self", "previous"))
        .statusCode(200)
        .when()
        .get(nextLink);
  }

  /**
   * Tests that when searching for EOB by patient id and using startIndex, returns paging results,
   * as startIndex (alongside _count) is one of the indicators to return paged results.
   */
  @Test
  public void testEobByPatientIdWithOnlyStartIndexExpectingPaging() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // Make a request with paging by adding startIndex or pageSize (_count in FHIR)
    String requestString = eobEndpoint + "?patient=" + patientId + "&startIndex=3";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 5 entries, since we entered on index 3 with 8 results
        .body("entry.size()", equalTo(5))
        // there should be 4 entries for the paging section; first, previous, last, self
        .body("link.size()", equalTo(4))
        .body("link.relation", hasItems("first", "previous", "last", "self"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that findByPatient works as expected for a {@link Patient} that does exist in the DB,
   * with a page size of 50 with fewer (8) results.
   */
  @Test
  public void searchForEobsWithLargePageSizesOnFewerResults() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=50";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have all 8 claims, since count > claims returned
        .body("entry.size()", equalTo(8))
        // there should be 3 entries for the paging section; first, last, self (since all the items
        // were returned on one page)
        .body("link.size()", equalTo(3))
        .body("link.relation", hasItems("first", "last", "self"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that EOB by patient id throws a 400 error when the paging start (startIndex) is set
   * higher than the maximum number of results.
   */
  @Test
  public void testEobByPatientIdWithPagingStartBeyondMaxExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=2&startIndex=12";

    // This request should only have 8 entries, so startIndex=12 is an error scenario
    // Keep in mind startIndex is which _entry_ to start on, not which _page_ of entries
    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (12) must be less than than result size (8)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that EOB by patient id throws a 400 error when the paging start (startIndex) is set to
   * the maximum number of results, since the highest index must be less than the number of results
   * as a 0-based index.
   */
  @Test
  public void testEobByPatientIdWithPagingStartSetToMaxResultsExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=2&startIndex=8";

    // This request should only have 8 entries, so startIndex=8 is an error scenario
    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (8) must be less than than result size (8)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that EOB by patient id does not error when the paging start (startIndex) is set to be
   * equal to one less than the maximum number of results.
   */
  @Test
  public void testEobByPatientIdWithPagingStartOneLessThanMaxExpect200() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=2&startIndex=7";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("resourceType", equalTo("Bundle"))
        // we should have 1 claim, since the startIndex was equal to the max number of claims
        .body("entry.size()", equalTo(1))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Test that searching by patient id with samhsa filtering = true does filter the samhsa data from
   * the response.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering() {

    // Load the SAMHSA sample data, which contains a claim entry for EVERY filterable code
    String patientId = testUtils.getPatientId(testUtils.loadSampleASamhsaData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&excludeSAMHSA=true";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // Make sure we found only the PDE claim that was not modified
        // (claims with samhsa data are entirely filtered out of the result set)
        .body("entry.size()", equalTo(1))
        .body("entry.resource.id", hasItem(containsString("pde")))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Ensure nothing unusual happens when we excludeSAMHSA = false and the result has non-samhsa
   * data.
   */
  @Test
  public void testEobByPatientIdForNonSamhsaEobsWithExcludeSamhsaTrueExpectNoError() {

    // dont load samhsa data
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // call samhsa filter, but it shouldn't do anything since there is nothing to filter
    String requestString = eobEndpoint + "?patient=" + patientId + "&excludeSAMHSA=true";

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /** Expect SAMHSA to be filtered when SAMHSA is allowed but excludeSAMHSA is true. */
  @Test
  public void
      testEobByPatientIdForNonSamhsaEobsWithExcludeSamhsaTrueWithSamhsaAllowedExpectNoError() {

    // don't load samhsa data
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // call samhsa filter, but it shouldn't do anything since there is nothing to filter
    String requestString = eobEndpoint + "?patient=" + patientId + "&excludeSAMHSA=true";

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is not
   * set, as we default to false. The server call takes a long time with the amount of exhaustive
   * SAMHSA data, so the explicit tests to test the setting set to false and the default were
   * combined for the sake of saving 25 seconds of test execution time.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaFalseByDefaultAndExpectNoFiltering() {
    // Adjust the sampleA data that was loaded to include some samhsa data by passing true
    List<Object> samhsaFiles = testUtils.loadSampleASamhsaData();
    String patientId = testUtils.getPatientId(samhsaFiles);
    String requestString = eobEndpoint + "?patient=" + patientId;
    // This number will change if we update any the SAMHSA filter lists
    // Num claims = loaded -6 (1 for bene, 5 for bene history) meaning the rest are claims to return
    int numSamhsaClaims = samhsaFiles.size() - 6;

    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // Check nothing is filtered; we should see tons of claims as we load 1 claim per SAMHSA
        // code for each type
        .body("entry.size()", equalTo(numSamhsaClaims))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /** Test eob by patient id does not return tax numbers for applicable claim types */
  @Test
  public void testEobByPatientIdWithExpectNoTaxNumbers() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId;

    // make sure all 8 entries come back as expected and no 400/500/other errors
    Response response =
        given()
            .spec(requestAuth)
            .given()
            .expect()
            .body("resourceType", equalTo("Bundle"))
            // we should have 8 claim type entries
            .body("entry.size()", equalTo(8))
            .statusCode(200)
            .when()
            .get(requestString);

    // Check no tax numbers on any of the claims
    // RestAssured also supports taking the response and doing gpath filters on it, instead of in
    // the chain
    // Helpful if you're doing something slightly more complex
    // We flatten here because there are multiple lists in the chain, so we end up with arrays of
    // arrays otherwise
    List<String> extensionUrls = response.path("entry.resource.item.extension.url.flatten()");
    for (String url : extensionUrls) {
      assertNotEquals("https://bluebutton.cms.gov/resources/variables/tax_num", url);
    }
  }

  /**
   * Validates that passing patient id and claim type returns only the requested claim type in the
   * response.
   */
  @Test
  public void testEobByPatientIdAndClaimTypeExpectOneResult() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&type=PDE";

    given()
        .spec(requestAuth)
        .expect()
        // our top level is still a bundle
        .body("resourceType", equalTo("Bundle"))
        // we should have 1 claim type entry
        .body("entry.size()", equalTo(1))
        // the claim type of the entry should be ExplanationOfBenefit
        .body("entry.resource.resourceType", everyItem(equalTo("ExplanationOfBenefit")))
        // Check our response has the single claim type (PDE as requested)
        .body("entry.resource.id", hasItem(containsString("pde")))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Tests EOB search by patient id with various lastUpdated values returns the expected number of
   * results for that query.
   */
  @Test
  public void testEobByPatientIdWithLastUpdated() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String baseRequestString = eobEndpoint + "?patient=" + patientId;

    // Build up a list of lastUpdatedURLs that return all claim types
    String nowDateTime = new DateTimeDt(Date.from(Instant.now().plusSeconds(1))).getValueAsString();
    String earlyDateTime = "2019-10-01T00:00:00+00:00";
    List<String> allUrls =
        Arrays.asList(
            "&_lastUpdated=gt" + earlyDateTime,
            "&_lastUpdated=ge" + earlyDateTime,
            "&_lastUpdated=le" + nowDateTime,
            "&_lastUpdated=ge" + earlyDateTime + "&_lastUpdated=le" + nowDateTime,
            "&_lastUpdated=gt" + earlyDateTime + "&_lastUpdated=lt" + nowDateTime);

    // Test that for each of the above lastUpdated values, we get 8 results back
    for (String lastUpdatedValue : allUrls) {

      given()
          .spec(requestAuth)
          .expect()
          .body("entry.size()", equalTo(8))
          .statusCode(200)
          .when()
          .get(baseRequestString + lastUpdatedValue);
    }

    // Should return 0 results when adjusting lastUpdated for a date that excludes all entries
    List<String> emptyUrls =
        Arrays.asList("&_lastUpdated=lt" + earlyDateTime, "&_lastUpdated=le" + earlyDateTime);

    for (String lastUpdatedValue : emptyUrls) {
      given()
          .spec(requestAuth)
          .expect()
          // Should have no entries in the path
          .body("$", not(hasKey("entry")))
          // Total in response should be set to 0
          .statusCode(200)
          .when()
          .get(baseRequestString + lastUpdatedValue);
    }
  }

  /**
   * Verifies that findByPatient works as with a lastUpdated parameter after yesterday and
   * pagination links work and contain lastUpdated.
   */
  @Test
  public void searchEobByPatientIdWithLastUpdatedAndPagination() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    int expectedCount = 5;
    int expectedTotal = 8;
    String yesterday =
        new DateTimeDt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))).getValueAsString();
    String now = new DateTimeDt(new Date()).getValueAsString();
    String requestString =
        String.format(
            eobEndpoint + "?patient=%s&_lastUpdated=ge%s&_lastUpdated=le%s&_count=%s",
            patientId,
            yesterday,
            now,
            expectedCount);

    // Search with lastUpdated range between yesterday and now, expect 5 results (due to count)
    Response response =
        given()
            .spec(requestAuth)
            .expect()
            .body("entry.size()", equalTo(expectedCount))
            .statusCode(200)
            .when()
            .get(requestString);

    // Check links have lastUpdated
    String selfLink = testUtils.getPaginationLink(response, "self");
    assertTrue(selfLink.contains("_lastUpdated"));

    String nextLink = testUtils.getPaginationLink(response, "next");
    assertTrue(nextLink.contains("_lastUpdated"));

    String lastLink = testUtils.getPaginationLink(response, "last");
    assertTrue(lastLink.contains("_lastUpdated"));

    String firstLink = testUtils.getPaginationLink(response, "first");
    assertTrue(firstLink.contains("_lastUpdated"));

    // Ensure using the next link works appropriately and returns the last 3 results
    given()
        .spec(requestAuth)
        /* RestAssured will url encode urls by default, and we use pre-encoded urls during pagination, so turn it off
         * since it will double-encode the lastUpdated field otherwise. */
        .urlEncodingEnabled(false)
        .expect()
        .body("entry.size()", equalTo(3))
        .statusCode(200)
        .when()
        .get(nextLink);
  }

  /**
   * Verifies that the EOB findByPatient logic falls back to using the correct dates for querying
   * the database when lastUpdated is not set / null.
   */
  @Test
  public void searchEobByPatientIdWhenNullLastUpdatedExpectFallback() {
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedRecords);
    Long claimId = Long.valueOf(testUtils.getClaimIdFor(loadedRecords, ClaimType.CARRIER));
    String requestString = eobEndpoint + "?patient=" + patientId;
    // Do some annoying date formatting since the json response and constant have different
    // precisions/formats
    String expectedFallbackDate =
        new DateTimeDt(Date.from(TransformerConstants.FALLBACK_LAST_UPDATED))
            .setPrecision(TemporalPrecisionEnum.MILLI)
            .getValueAsString();

    // Clear lastupdated in the database
    ServerTestUtils.get()
        .doTransaction(
            (em) -> {
              em.createQuery("update CarrierClaim set lastUpdated=null where claimId=:claimId")
                  .setParameter("claimId", claimId)
                  .executeUpdate();
            });

    // Make a call to get the data and ensure lastUpdated matches the fallback value
    given()
        .spec(requestAuth)
        .expect()
        // Expect all the claims to return
        .body("entry.size()", equalTo(8))
        .rootPath("entry.find { it.resource.id.contains('carrier') }")
        .body("resource.meta.lastUpdated", equalTo(expectedFallbackDate))
        .statusCode(200)
        .when()
        .get(requestString);

    // Make a call with lastUpdated to make sure the fallback is used for lastUpdated queries
    // Set the lastUpdated to pull anything before the current date
    String now = new DateTimeDt(new Date()).getValueAsString();

    given()
        .spec(requestAuth)
        .expect()
        // Expect all the claims to return
        .body("entry.size()", equalTo(8))
        // Check the lastUpdated is set to the fallback
        .rootPath("entry.find { it.resource.id.contains('carrier') }")
        .body("resource.meta.lastUpdated", equalTo(expectedFallbackDate))
        .statusCode(200)
        .when()
        .get(requestString + "&_lastUpdated=le" + now);

    // Set the lastUpdated to pull anything after the test started
    // This should avoid the null lastUpdated item we modified, so 7/8 should return

    // Set the time to be slightly before now, so we can pull the stuff we loaded in this test
    String slightyBeforeNow =
        new DateTimeDt(new Date(System.currentTimeMillis() - 100000)).getValueAsString();

    given()
        .spec(requestAuth)
        .expect()
        // Expect all the claims to return
        .body("entry.size()", equalTo(7))
        .statusCode(200)
        .when()
        .get(requestString + "&_lastUpdated=gt" + slightyBeforeNow);
  }

  /**
   * Verifies that search by patient id returns the expected data when filtering by service date.
   */
  @Test
  public void searchEobByPatientIdWithServiceDate() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = eobEndpoint + "?patient=" + patientId;

    // For SampleA data, we have the following service dates
    // HHA 23-JUN-2015
    // Hospice 30-JAN-2014
    // Inpatient 27-JAN-2016
    // Outpatient 24-JAN-2011
    // SNF 18-DEC-2013
    // Carrier 27-OCT-1999
    // DME 03-FEB-2014
    // PDE 12-MAY-2015

    // TestName:serviceDate:ExpectedCount
    List<Triple<String, String, Integer>> testCases =
        ImmutableList.of(
            ImmutableTriple.of("No service date filter", "", 8),
            ImmutableTriple.of(
                "Contains all", "service-date=ge1999-10-27&service-date=le2016-01-27", 8),
            ImmutableTriple.of("Contains none - upper bound", "service-date=gt2016-01-27", 0),
            ImmutableTriple.of("Contains none - lower bound", "service-date=lt1999-10-27", 0),
            ImmutableTriple.of(
                "Exclusive check - no earliest/latest",
                "service-date=gt1999-10-27&service-date=lt2016-01-27",
                6),
            ImmutableTriple.of(
                "Year end 2015 inclusive check (using last day of 2015)",
                "service-date=le2015-12-31",
                7),
            ImmutableTriple.of(
                "Year end 2014 exclusive check (using first day of 2015)",
                "service-date=lt2015-01-01",
                5));

    // Test each case
    for (Triple testData : testCases) {

      String url = requestString + "&" + testData.getMiddle();
      Response response =
          given()
              .spec(requestAuth)
              .expect()
              .statusCode(200)
              .body("resourceType", equalTo("Bundle"))
              .when()
              .get(url);

      // To preserve the test case message, we'll set this up using assertEquals
      Integer entry = 0;
      if (response.path("entry") != null) {
        entry = response.path("entry.size()");
      }
      assertEquals(
          String.valueOf(entry), testData.getRight().toString(), testData.getLeft().toString());
    }
  }

  /**
   * Verify that an empty bundle is returned when pagination is requested but no results are
   * returned. Normally this would return a 400 since the default startIndex is equal to the number
   * of results, but we make a special exception for empty returns since there's nothing to paginate
   * anyway.
   */
  @Test
  public void searchEobByPatientIdWithNoResultsAndPaginationRequestedExpect200() {
    String patientId = "0";
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=50";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString);

    // check with startIndex as well
    requestString = eobEndpoint + "?patient=" + patientId + "&startIndex=2";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that access.json is written to within BFD-server-war via API call and has the MDC keys
   * expected for EOB read.
   */
  @Test
  public void testEobReadIdHasAccessJsonWithExpectedMdcKeys() throws IOException {
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String claimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.CARRIER);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.CARRIER, claimId);
    String requestString = eobEndpoint + eobId;

    Map<String, String> headers = new HashMap<>();
    headers.put(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");
    headers.put(R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true");
    // Add extra check for accept-charset to make sure it passes through
    headers.put("Accept-Charset", "utf-8");
    List<String> additionalExpectedMdcKeys =
        List.of(
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_CONTENT_LOCATION,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_TAX_NUMBERS,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ADDRESS_FIELDS,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_CHARSET);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys, headers);
  }

  /**
   * Verifies that access.json is written to within BFD-server-war via API call and has the MDC keys
   * expected for EOB by patient id.
   */
  @Test
  public void testEobByPatientIdHasAccessJsonWithExpectedMdcKeys() throws IOException {
    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    String requestString = eobEndpoint + "?patient=" + patientId;

    Map<String, String> headers = new HashMap<>();
    headers.put(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");
    headers.put(R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true");
    List<String> additionalExpectedMdcKeys =
        List.of(
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_TAX_NUMBERS,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ADDRESS_FIELDS);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys, headers);
  }

  /**
   * Verify a successful EOB response returns when searching for the specified claim type's id (as
   * taken from the sample data).
   *
   * @param claimType the claim type to search for
   */
  private void verifySuccessfulResponseAndClaimIdFor(ClaimType claimType) {
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String claimId = ServerTestUtils.getClaimIdFor(loadedRecords, claimType);
    // Code is the same between utils, so can use v2 call for both
    String eobId = CommonTransformerUtils.buildEobId(claimType, claimId);
    String systemId = "https://bluebutton.cms.gov/resources/variables/clm_id";
    if (claimType == ClaimType.PDE) {
      // PDE uses a diff identifier for its ID
      systemId = "https://bluebutton.cms.gov/resources/variables/pde_id";
    }

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        // For non-array items, we can use equalTo (or stringContains for partial matches)
        .body("resourceType", equalTo("ExplanationOfBenefit"))
        .body("id", equalTo(eobId))
        /* RestAssured uses Groovy under the hood, so our search query closures can use Groovy's collection API.
         * See https://www.baeldung.com/rest-assured-groovy for some examples.
         */
        .body(
            String.format("identifier.find {it.system == '%s'}.value", systemId), equalTo(claimId))
        .statusCode(200)
        .when()
        .get(eobEndpoint + eobId);
  }
}
