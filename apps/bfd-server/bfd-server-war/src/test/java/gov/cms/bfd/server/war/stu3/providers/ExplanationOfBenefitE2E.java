package gov.cms.bfd.server.war.stu3.providers;

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
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import io.restassured.response.Response;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V1 explanation of benefits endpoint. */
public class ExplanationOfBenefitE2E extends ServerRequiredTest {

  /** The base eob endpoint. */
  private static String eobEndpoint;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (eobEndpoint == null) {
      eobEndpoint = baseServerUrl + "/v1/fhir/ExplanationOfBenefit/";
    }
  }

  /**
   * Verifies that an EOB carrier claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadEobIdExpectClmIdInResponse() {

    List<Object> loadedRecords = testUtils.loadSampleAData();
    CarrierClaim claim = getCarrierClaim(loadedRecords);
    String claimId = String.valueOf(claim.getClaimId());
    String eobId = TransformerUtils.buildEobId(ClaimType.CARRIER, claim.getClaimId());

    given()
        .spec(requestAuth)
        .expect()
        // For non-array items, we can use equalTo (or stringContains for partial matches)
        .body("resourceType", equalTo("ExplanationOfBenefit"))
        .body("id", equalTo(eobId))
        /* RestAssured uses Groovy under the hood, so our search query closures can use Groovy's collection API.
         * See https://www.baeldung.com/rest-assured-groovy for some examples.
         */
        .body(
            "identifier.find {it.system == 'https://bluebutton.cms.gov/resources/variables/clm_id'}.value",
            equalTo(claimId))
        .statusCode(200)
        .when()
        .get(eobEndpoint + eobId);
  }

  /** Verifies that the EOB returns a 400 when called with a negative count parameter. */
  @Test
  public void testEobByPatientIdWithNegativeCountExpect400() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // It's also possible to add these to the call below, but I liked the request string as
    // one would write it when calling the API
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
  public void testEobByPatientIdWithValidPatientIdExpectValidEob() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    String requestString = eobEndpoint + "?patient=" + patientId;
    OutpatientClaim expectedOutpatientClaim = getOutpatientClaim(loadedData);

    given()
        .spec(requestAuth)
        .expect()
        // our top level is a bundle
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        // we should also have a tptal field that describes how many entries too
        .body("total", equalTo(8))
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} returns no paging for
   * a patient that exists in the DB, with paging on a page size of 0.
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
   * pages and all expected pages exist.
   */
  @Test
  public void testEobByPatientIdWithPagingExpectAllPages() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // Make a request with paging by adding startIndex or pageSize (_count in FHIR)
    String requestString = eobEndpoint + "?patient=" + patientId + "&_count=2";

    // Page 1/4

    Response response =
        given()
            .spec(requestAuth)
            .expect()
            .log()
            .body()
            .body("resourceType", equalTo("Bundle"))
            // we should have 2 entries, since we set page size to 2
            .body("entry.size()", equalTo(2))
            // Our entries should contain the first two claim types (we return claim types in order)
            .body(
                "entry.resource.id", hasItems(containsString("outpatient"), containsString("dme")))
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

    // Page 2/4

    response =
        given()
            .spec(requestAuth)
            .expect()
            .body("resourceType", equalTo("Bundle"))
            // we should have 2 entries, since we set page size to 2
            .body("entry.size()", equalTo(2))
            // Our entries should contain the first two claim types (we return claim types in order)
            .body("entry.resource.id", hasItems(containsString("hha"), containsString("inpatient")))
            // there should still be the 4 entries for the paging section; first, next, last, self,
            // plus a new "previous" entry
            .body("link.size()", equalTo(5))
            .body("link.relation", hasItems("first", "next", "last", "self", "previous"))
            .statusCode(200)
            .when()
            .get(nextLink);

    // page 3/4

    nextLink = testUtils.getPaginationLink(response, "next");

    response =
        given()
            .spec(requestAuth)
            .expect()
            .body("resourceType", equalTo("Bundle"))
            // we should have 2 entries, since we set page size to 2
            .body("entry.size()", equalTo(2))
            .body("entry.resource.id", hasItems(containsString("pde"), containsString("snf")))
            .body("link.size()", equalTo(5))
            .body("link.relation", hasItems("first", "next", "last", "self", "previous"))
            .statusCode(200)
            .when()
            .get(nextLink);

    // page 4/4

    nextLink = testUtils.getPaginationLink(response, "next");

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 2 entries, since we set page size to 2
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} works as expected for
   * a {@link Patient} that does exist in the DB, with a page size of 50 with fewer (8) results.
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
        .body("total", equalTo(8))
        .when()
        .get(requestString);
  }

  /**
   * Test that searching by patient id with samhsa filtering = true does filter the samhsa data from
   * the response.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data
    // We could also just keep a samhsa set, or have the default set have samhsa
    // so that we don't need a separate set or modification
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData(true));
    String requestString =
        eobEndpoint + "?patient=" + patientId + "&type=CARRIER&excludeSAMHSA=true";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // check the samhsa data is not present in the claim (carrier, in this example)
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            not(hasItem(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE)))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is set
   * to false.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaFalseExpectNoFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data by passing true
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData(true));
    String requestString =
        eobEndpoint + "?patient=" + patientId + "&type=CARRIER&excludeSAMHSA=false";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // check the samhsa data is not present in the claim (carrier, in this example)
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
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
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData(false));
    // call samhsa filter, but it shouldn't do anything since there is nothing to filter
    String requestString = eobEndpoint + "?patient=" + patientId + "&excludeSAMHSA=true";

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .body("total", equalTo(8))
        .statusCode(200)
        .when()
        .get(requestString);
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
    CarrierClaim carrierClaim = getCarrierClaim(loadedData);
    DMEClaim dmeClaim = getDmeClaim(loadedData);

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
   * Test eob by patient id does not return tax numbers for applicable claim types when
   * IncludeTaxNumbers = false.
   */
  @Test
  public void testEobByPatientIdWithIncludeTaxNumbersFalseExpectNoTaxNumbers() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    // IncludeTaxNumbers is a header, so added below in restAssured API
    String requestString = eobEndpoint + "?patient=" + patientId;

    // make sure all 8 entries come back as expected and no 400/500/other errors
    Response response =
        given()
            .spec(requestAuth)
            .given()
            .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "false")
            .expect()
            .body("resourceType", equalTo("Bundle"))
            // we should have 8 claim type entries
            .body("entry.size()", equalTo(8))
            .body("total", equalTo(8))
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
        .body("total", equalTo(1))
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
          .body("total", equalTo(8))
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
          .body("total", equalTo(0))
          .statusCode(200)
          .when()
          .get(baseRequestString + lastUpdatedValue);
    }
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} works as with a
   * lastUpdated parameter after yesterday and pagination links work and contain lastUpdated.
   *
   * <p>FIXME: Count doesnt seem to work with the query string as-is; may be a bug?
   */
  @Test
  @Disabled("Broken, needs investigation/fixing")
  public void searchEobByPatientIdWithLastUpdatedAndPagination() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    int expectedCount = 5;
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
            .body("total", equalTo(expectedCount))
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
        .expect()
        .body("entry.size()", equalTo(3))
        .body("total", equalTo(3))
        .statusCode(200)
        .when()
        .get(URLDecoder.decode(nextLink));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider} falls back to using the correct
   * dates for querying the database when lastUpdated is not set / null.
   */
  @Test
  public void searchEobByPatientIdWhenNullLastUpdatedExpectFallback() {

    List<Object> loadedRecords = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedRecords);
    Long claimId = getCarrierClaim(loadedRecords).getClaimId();
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
        .body("total", equalTo(8))
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
        .body("total", equalTo(8))
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
        .body("total", equalTo(7))
        .statusCode(200)
        .when()
        .get(requestString + "&_lastUpdated=gt" + slightyBeforeNow);
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider} search by patient id returns the
   * expected data when filtering by service date.
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
      Integer total = response.path("total");
      assertEquals(
          String.valueOf(total.intValue()),
          testData.getRight().toString(),
          testData.getLeft().toString());
    }
  }

  /**
   * Gets a carrier claim for basic test information.
   *
   * @param loadedRecords the loaded records
   * @return the carrier claim
   */
  public CarrierClaim getCarrierClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof CarrierClaim)
        .map(CarrierClaim.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets an outpatient claim for basic test information.
   *
   * @param loadedRecords the loaded records
   * @return the outpatient claim
   */
  public OutpatientClaim getOutpatientClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof OutpatientClaim)
        .map(OutpatientClaim.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets a dme claim for basic test information.
   *
   * @param loadedRecords the loaded records
   * @return the dme claim
   */
  public DMEClaim getDmeClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof DMEClaim)
        .map(DMEClaim.class::cast)
        .findFirst()
        .get();
  }
}
