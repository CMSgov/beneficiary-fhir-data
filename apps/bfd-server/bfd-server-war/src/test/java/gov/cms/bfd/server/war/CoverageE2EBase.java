package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import ca.uhn.fhir.model.primitive.IdDt;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import io.restassured.response.Response;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;

/** Abstract base class for RestAssured Coverage E2E tests shared between BFD versions. */
public abstract class CoverageE2EBase extends ServerRequiredTest {

  /** The base coverage endpoint. */
  protected String coverageEndpoint;

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 200 and response for {@link
   * Beneficiary}-derived part A Medicare segment {@link Coverage}s that exists in the DB.
   */
  @Test
  public void testReadWhenExistingPartACoverageIdExpect200() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary);

    verifyReadIs200ForId(coverageId.getIdPart());
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 200 and response for {@link
   * Beneficiary}-derived part B Medicare segment {@link Coverage}s that exists in the DB.
   */
  @Test
  public void testReadWhenExistingPartBCoverageIdExpect200() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary);

    verifyReadIs200ForId(coverageId.getIdPart());
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 200 and response for {@link
   * Beneficiary}-derived part C Medicare segment {@link Coverage}s that exists in the DB.
   */
  @Test
  public void testReadWhenExistingPartCCoverageIdExpect200() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_C, beneficiary);

    verifyReadIs200ForId(coverageId.getIdPart());
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 200 and response for {@link
   * Beneficiary}-derived part D Medicare segment {@link Coverage}s that exists in the DB.
   */
  @Test
  public void testReadWhenExistingPartDCoverageIdExpect200() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary);

    verifyReadIs200ForId(coverageId.getIdPart());
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 404 when requesting a contract
   * that does not exist.
   */
  @Test
  public void testReadWhenNonExistingCoverageIdExpect404() {
    // Use a coverage id that does not exist
    String requestString = coverageEndpoint + "part-d-9999999";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("HAPI-0971: Resource Beneficiary/9999999 is not known"))
        .statusCode(404)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 404 when requesting a contract
   * that does not exist and is negative (to test the id validation regex).
   */
  @Test
  public void testReadWhenNonExistingNegativeCoverageIdExpect404() {
    // Use a coverage id that does not exist
    String requestString = coverageEndpoint + "part-d--9999999";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("HAPI-0971: Resource Beneficiary/-9999999 is not known"))
        .statusCode(404)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage returns a 200 when searching by an existing bene id, and the bene id in
   * each of the resources matches the searched-by id.
   */
  @Test
  public void testSearchByBeneWithExistingBeneExpect200() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString = coverageEndpoint + "?beneficiary=" + beneficiaryId;
    String patientId = "Patient/" + beneficiaryId;
    String partAId = "part-a-" + beneficiaryId;
    String partBId = "part-b-" + beneficiaryId;
    String partCId = "part-c-" + beneficiaryId;
    String partDId = "part-d-" + beneficiaryId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // returns a record for parts a,b,c,d for this bene
        .body("entry.size()", equalTo(4))
        .body("entry.resource.id.flatten()", hasItems(partAId, partBId, partCId, partDId))
        // Make sure the bene id is right for each returned record
        .body("entry.beneficiary.reference.flatten()", everyItem(equalTo(patientId)))
        // No paging was requested, so check no paging was returned (other than a self link)
        .body("link.size()", equalTo(1))
        .body("link.relation", hasItem("self"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage returns a 404 when searching by a bene id that does not exist in the data.
   */
  @Test
  public void testSearchByBeneWithMissingBeneExpectNoResults() {
    String requestString = coverageEndpoint + "?beneficiary=1234";
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage with paging requested returns the paging links and navigating to those
   * links returns valid paged results.
   */
  @Test
  public void testSearchByBeneWithPagingExpectAllPagesWork() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString = coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2";

    Response response =
        given()
            .spec(requestAuth)
            .expect()
            .body("resourceType", equalTo("Bundle"))
            // count set to 2
            .body("entry.size()", equalTo(2))
            // Check paging links are there
            .body("link.size()", equalTo(4))
            .body("link.relation", hasItems("self", "next", "first", "last"))
            .statusCode(200)
            .when()
            .get(requestString);

    // Get the next link, and make the call
    String nextLink = testUtils.getPaginationLink(response, "next");

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // count set to 2 (4 results total, so last page)
        .body("entry.size()", equalTo(2))
        // Check paging links are there
        .body("link.size()", equalTo(4))
        .body("link.relation", hasItems("self", "previous", "first", "last"))
        .statusCode(200)
        .when()
        .get(nextLink);
  }

  /**
   * Verify that Coverage search by id returns a 200 and no results filtered out when searching by
   * an existing bene id using lastUpdated dates that are inclusive of the record's lastUpdated
   * date.
   */
  @Test
  public void testSearchByBeneWithInBoundsLastUpdatedExpectNoFiltering() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString = coverageEndpoint + "?beneficiary=" + beneficiaryId;

    // Set the lower bound such that all records which were loaded for this test will be returned
    String lowerBound =
        new DateTimeDt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))).getValueAsString();
    String upperBound = new DateTimeDt(new Date()).getValueAsString();
    String inBoundsLastUpdatedParam =
        "&_lastUpdated=ge" + lowerBound + "&_lastUpdated=le" + upperBound;

    given()
        .spec(requestAuth)
        .expect()
        // should get back all records
        .body("entry.size()", equalTo(4))
        .statusCode(200)
        .when()
        .get(requestString + inBoundsLastUpdatedParam);
  }

  /**
   * Verify that Coverage search by id returns a 200 and all results filtered out when searching by
   * an existing bene id using lastUpdated dates that are exclusive of the record's lastUpdated
   * date.
   */
  @Test
  public void testSearchByBeneWithOutOfBoundsLastUpdatedExpectFiltering() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString = coverageEndpoint + "?beneficiary=" + beneficiaryId;

    // Adjusting the bounding such that nothing will return

    // This lower bound doesnt matter much
    String lowerBound =
        new DateTimeDt(Date.from(Instant.now().minus(2, ChronoUnit.DAYS))).getValueAsString();
    // This upper bound prevents us from getting records
    String upperBound =
        new DateTimeDt(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))).getValueAsString();
    String outOfBoundsLastUpdatedParam =
        "&_lastUpdated=ge" + lowerBound + "&_lastUpdated=le" + upperBound;

    given()
        .spec(requestAuth)
        .expect()
        // should filter all records
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString + outOfBoundsLastUpdatedParam);
  }

  /**
   * Verify that Coverage returns a 400 when the paging start (startIndex) is set higher than the
   * maximum number of results.
   */
  @Test
  public void testSearchByBeneWithPagingStartBeyondMaxExpect400() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=12";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (12) must be less than than result size (4)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage returns a 400 when the paging start (startIndex) is set to the maximum
   * number of results, since the highest index must be less than the number of results as a 0-based
   * index.
   */
  @Test
  public void testSearchByBeneWithPagingStartSetToMaxResultsExpect400() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=4";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (4) must be less than than result size (4)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage does not error when the paging start (startIndex) is set to be equal to
   * one less than the maximum number of results.
   */
  @Test
  public void testSearchByBeneWithPagingStartOneLessThanMaxExpect200() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=3";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that an empty bundle is returned when pagination is requested but no results are
   * returned. Normally this would return a 400 since the default startIndex is equal to the number
   * of results, but we make a special exception for empty returns since there's nothing to paginate
   * anyway.
   */
  @Test
  public void testSearchByBeneWithNoResultsAndPaginationRequestedExpect200() {
    String beneId = "0";
    String requestString = coverageEndpoint + "?beneficiary=" + beneId + "&_count=50";

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
    requestString = coverageEndpoint + "?beneficiary=" + beneId + "&startIndex=2";

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
   * expected for Coverage read.
   */
  @Test
  public void testCoverageReadIdHasAccessJsonWithExpectedMdcKeys() throws IOException {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary);
    String requestString = coverageEndpoint + coverageId.getIdPart();

    List<String> additionalExpectedMdcKeys =
        List.of(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_CONTENT_LOCATION);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys);
  }

  /**
   * Verifies that access.json is written to within BFD-server-war via API call and has the MDC keys
   * expected for Coverage search by bene id.
   */
  @Test
  public void testCoverageSearchByBeneHasAccessJsonWithExpectedMdcKeys() throws IOException {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString = coverageEndpoint + "?beneficiary=" + beneficiaryId;

    List<String> additionalExpectedMdcKeys = List.of(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys);
  }

  /**
   * Verifies that coverage read returns a 200 status and the specified medicare segment id in the
   * response.
   *
   * @param coverageId the coverage id
   */
  private void verifyReadIs200ForId(String coverageId) {
    String requestString = coverageEndpoint + coverageId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Coverage"))
        .body("id", equalTo(coverageId))
        .statusCode(200)
        .when()
        .get(requestString);
  }
}
