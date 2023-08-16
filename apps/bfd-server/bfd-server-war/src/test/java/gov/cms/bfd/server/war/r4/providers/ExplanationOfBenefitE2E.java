package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcherTest;
import io.restassured.response.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V2 explanation of benefits endpoint. */
public class ExplanationOfBenefitE2E extends ServerRequiredTest {

  /** The base eob endpoint. */
  public static String EOB_ENDPOINT;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (EOB_ENDPOINT == null) {
      EOB_ENDPOINT = baseServerUrl + "/v2/fhir/ExplanationOfBenefit/";
    }
  }

  /**
   * Verifies that an EOB carrier claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadEobIdExpectClmIdInResponse() {

    List<Object> loadedRecords = loadData();
    CarrierClaim claim = getCarrierClaim(loadedRecords);
    String claimId = String.valueOf(claim.getClaimId());
    String eobId = TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, claim.getClaimId());

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
        .get(EOB_ENDPOINT + eobId);
  }

  /** Verifies that the EOB returns a 400 when called with a negative count parameter. */
  @Test
  public void testEobByPatientIdWithNegativeCountExpect400() {

    String patientId = getPatientId(loadData());
    // It's also possible to add these to the call below, but I liked the request string as
    // one would write it when calling the API
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&_count=-10";

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

    List<Object> loadedData = loadData();
    String patientId = getPatientId(loadedData);
    String requestString = EOB_ENDPOINT + "?patient=" + patientId;
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
        .body("entry.resource.id", hasItem(containsString("outpatient")))
        .body("entry.resource.id", hasItem(containsString("inpatient")))
        .body("entry.resource.id", hasItem(containsString("carrier")))
        .body("entry.resource.id", hasItem(containsString("dme")))
        .body("entry.resource.id", hasItem(containsString("snf")))
        .body("entry.resource.id", hasItem(containsString("pde")))
        .body("entry.resource.id", hasItem(containsString("hospice")))
        .body("entry.resource.id", hasItem(containsString("hha")))
        // Can also just make one big nice list like this
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
        // right path, with the right value. This is a "data correctness" check.
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num' }.valueIdentifier.value",
            equalTo(expectedOutpatientClaim.getFiDocumentClaimControlNumber().orElseThrow()))
        .statusCode(200)
        .when()
        .get(requestString);

    /* The above correctness check with the fi_doc_clm_cntl_num is probably more appropriate and easier to test in a transformer unit test.
     * RestAssured tests are more useful for checking http status codes on errors and successes, metadata items and things added as part
     * of hapiFhir or other interceptors that are hard to test otherwise, particularly things that HAPI-FHIR catches and handles, like 400s/500s.
     *
     * As an aside, our resource provider E2Es are not terribly useful currently. They essentially just call the resource provider and ensure
     * the output matches what happens when we call the transformer directly; this is essentially just checking the transformer was called, which we can
     * do with mocking in a unit test. This says nothing about if the results from the transformer are _correct_ so the E2E is not terrible valuable.
     *
     * We could write more correctness tests in the transformer unit tests, or in these tests if desired. */
  }

  /**
   * Test that when we get a valid EOB by patient ID response, if we did not initially request
   * paging we can still request paging into the results by using startIndex.
   */
  @Test
  public void testEobByPatientIdWithValidNonPagingResponseExpectCanAddPagingToResults() {

    String patientId = getPatientId(loadData());
    String requestString = EOB_ENDPOINT + "?patient=" + patientId;

    Response response =
        given()
            .spec(requestAuth)
            .expect()
            // we should have 8 claim type entries
            .body("entry.size()", equalTo(8))
            // Check that no paging info exists, since we didn't request it (only self should exist
            // for
            // link)
            .body("link.size()", equalTo(1))
            .body("link.relation", hasItem("self"))
            .statusCode(200)
            .when()
            .get(requestString);

    // Get the 'self' link
    List<Map<String, ?>> links = response.jsonPath().getList("link");
    String selfLink =
        links.stream()
            .filter(m -> m.get("relation").equals("self"))
            .findFirst()
            .orElseThrow()
            .get("url")
            .toString();

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

    String patientId = getPatientId(loadData());
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&_count=0";

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

    String patientId = getPatientId(loadData());
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&unknownParam=124";

    given().spec(requestAuth).expect().statusCode(400).when().get(requestString);
  }

  /**
   * Tests that when searching for EOB by patient id and a number of pages, we can cycle through all
   * pages and all expected pages exist.
   */
  @Test
  public void testEobByPatientIdWithPagingExpectAllPages() {

    String patientId = getPatientId(loadData());
    // Make a request with paging by adding startIndex or pageSize (_count in FHIR)
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&_count=2";

    // Page 1/4

    Response response =
        given()
            .spec(requestAuth)
            .expect()
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
    List<Map<String, ?>> links = response.jsonPath().getList("link");
    String nextLink =
        links.stream()
            .filter(m -> m.get("relation").equals("next"))
            .findFirst()
            .orElseThrow()
            .get("url")
            .toString();

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

    links = response.jsonPath().getList("link");
    nextLink =
        links.stream()
            .filter(m -> m.get("relation").equals("next"))
            .findFirst()
            .orElseThrow()
            .get("url")
            .toString();

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

    links = response.jsonPath().getList("link");
    nextLink =
        links.stream()
            .filter(m -> m.get("relation").equals("next"))
            .findFirst()
            .orElseThrow()
            .get("url")
            .toString();

    given()
        .spec(requestAuth)
        .expect()
        // Can use this to log the response; requires an empty body call after the log
        .log()
        .body()
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
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void searchForEobsWithLargePageSizesOnFewerResults() {

    String patientId = getPatientId(loadData());
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&_count=50";

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

  /** Test eob by patient id with paging start beyond max expect 400. */
  @Test
  public void testEobByPatientIdWithPagingStartBeyondMaxExpect400() {
    String patientId = getPatientId(loadData());
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&_count=2&startIndex=12";

    // This request should only have 8 entries, so startIndex=12 is an error scenario
    // Keep in mind startIndex is which _entry_ to start on, not which _page_ of entries
    // This should be a 400 since its a user input error, however it returns a 500; we can improve
    // this by adjusting
    // paths in the code which are user errors to return a 400 (already partly done, but not in this
    // case).
    // This type of testing could help us discover and fix issues like this.
    given().spec(requestAuth).expect().log().body().statusCode(400).when().get(requestString);
  }

  /** Test eob by patient id with exclude samhsa true expect filtering. */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data
    // We could also just keep a samhsa set, or have the default set have samhsa
    // so that we don't need a separate set or modification
    String patientId = getPatientId(loadData(true));
    String requestString =
        EOB_ENDPOINT + "?patient=" + patientId + "&type=CARRIER&excludeSAMHSA=true";

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

  /** Test eob by patient id with exclude samhsa false expect no filtering. */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaFalseExpectNoFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data by passing true
    String patientId = getPatientId(loadData(true));
    String requestString =
        EOB_ENDPOINT + "?patient=" + patientId + "&type=CARRIER&excludeSAMHSA=false";

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

  /** Ensure nothing unusual happens when we exclude samhsa on non-samhsa data. */
  @Test
  public void testEobByPatientIdForNonSamhsaEobsWithExcludeSamhsaTrueExpectNoError() {

    // dont load samhsa data
    String patientId = getPatientId(loadData(false));
    // call samhsa filter, but it shouldn't do anything since there is nothing to filter
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&excludeSAMHSA=true";

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

    List<Object> loadedData = loadData();
    String patientId = getPatientId(loadedData);
    // IncludeTaxNumbers is a header, so added below in restAssured API
    String requestString = EOB_ENDPOINT + "?patient=" + patientId;
    CarrierClaim carrierClaim = getCarrierClaim(loadedData);
    DMEClaim dmeClaim = getDmeClaim(loadedData);

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .given()
        .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true")
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .body("total", equalTo(8))
        // Check there are tax numbers on applicable claims; carrier and DME
        // Tax num value is found in the carrier.resource.item.extension.valueCoding.code (for the
        // appropriate url)
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.item[0].extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/tax_num' }.valueCoding.code",
            equalTo(dmeClaim.getLines().get(0).getProviderTaxNumber()))
        // Couldnt get the above find on tax num working for this; some quirk of Gpath i think?
        // Needs some research, but this works too.
        // Manually looked at the raw response and dont see any reason why the above find shouldnt
        // work,
        // the path and structure is the same between carrier and dme for the tax num extension in
        // item
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.item[0].extension.valueCoding.code.flatten()",
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

    String patientId = getPatientId(loadData());
    // IncludeTaxNumbers is a header, so added below in restAssured API
    String requestString = EOB_ENDPOINT + "?patient=" + patientId;

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
    // RestAssured also supports taking the response and doing gpath filters on it, instead of in the chain
    // Helpful if you're doing something slightly more complex
    // We flatten here because there are multiple lists in the chain, so we end up with arrays of arrays otherwise
    List<String> extensionUrls = response.path("entry.resource.item.extension.url.flatten()");
    for (String url : extensionUrls) {
      assertNotEquals("https://bluebutton.cms.gov/resources/variables/tax_num", url);
    }
  }

  // TODO: patientId+claim type, lastUpdated, lastUpdated+pagination,
  // TODO: lastUpdated+pagination+type, null lastUpdated, serviceDate

  


  // Helper methods; could easily be in a utils class, if desired

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
   * Gets a carrier claim for basic test information.
   *
   * @param loadedRecords the loaded records
   * @return the carrier claim
   */
  public OutpatientClaim getOutpatientClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof OutpatientClaim)
        .map(OutpatientClaim.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets a carrier claim for basic test information.
   *
   * @param loadedRecords the loaded records
   * @return the carrier claim
   */
  public DMEClaim getDmeClaim(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof DMEClaim)
        .map(DMEClaim.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Load data and get patient id string.
   *
   * @return the string
   */
  public List<Object> loadData() {
    return loadData(false);
  }

  /**
   * Loads the sample A data to use in tests.
   *
   * @param addSamhsa if samhsa data should be added to the test data
   * @return the loaded records
   */
  public List<Object> loadData(boolean addSamhsa) {

    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    if (addSamhsa) {
      addSamhsaToLoadedRecords(loadedRecords);
    }

    return loadedRecords;
  }

  /**
   * Gets the first beneficiary.
   *
   * @param loadedRecords the loaded records
   * @return the first beneficiary
   */
  public Beneficiary getFirstBeneficiary(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(Beneficiary.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets the patient id.
   *
   * @param loadedRecords the loaded records
   * @return the patient id
   */
  public String getPatientId(List<Object> loadedRecords) {
    return TransformerUtilsV2.buildPatientId(getFirstBeneficiary(loadedRecords)).getIdPart();
  }

  /**
   * Load the SAMPLE_A resources and then tweak carrier claim types to have a SAMHSA diagnosis code.
   *
   * <p>Ideally get rid of this and add samhsa to the default set.
   *
   * @param loadedRecords the loaded records
   */
  private void addSamhsaToLoadedRecords(List<Object> loadedRecords) {
    // Load the SAMPLE_A resources normally.
    EntityManager entityManager = null;

    try {
      EntityManagerFactory entityManagerFactory =
          PipelineTestUtils.get().getPipelineApplicationState().getEntityManagerFactory();
      entityManager = entityManagerFactory.createEntityManager();

      // Tweak the SAMPLE_A claims such that they are SAMHSA-related.
      adjustCarrierClaimForSamhsaDiagnosis(loadedRecords, entityManager);

    } finally {
      if (entityManager != null && entityManager.getTransaction().isActive())
        entityManager.getTransaction().rollback();
      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Adjusts the carrier claim to support samhsa.
   *
   * @param loadedRecords the loaded records
   * @param entityManager the entity manager
   */
  private void adjustCarrierClaimForSamhsaDiagnosis(
      List<Object> loadedRecords, EntityManager entityManager) {

    CarrierClaim carrierRifRecord =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();

    entityManager.getTransaction().begin();
    carrierRifRecord = entityManager.find(CarrierClaim.class, carrierRifRecord.getClaimId());
    carrierRifRecord.setDiagnosis2Code(
        Optional.of(Stu3EobSamhsaMatcherTest.SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE));
    carrierRifRecord.setDiagnosis2CodeVersion(Optional.of('9'));
    entityManager.merge(carrierRifRecord);
    entityManager.getTransaction().commit();
  }
}
