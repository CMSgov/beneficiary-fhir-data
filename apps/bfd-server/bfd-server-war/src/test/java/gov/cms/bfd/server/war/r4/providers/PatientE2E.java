package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.PatientE2EBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Endpoint end-to-end test for the V2 Patient endpoint. Most test logic should be placed in {@link
 * PatientE2EBase} to be shared, unless there are version-specific paths or functionality to test.
 *
 * <p>To run individual tests in-IDE, ensure you use a view that shows inherited tests (like
 * IntelliJ's Structure panel with the "Inherited" option at the top)
 */
public class PatientE2E extends PatientE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Dont need any headers to return mbi in V2
    headers = new HashMap<>();
    patientEndpoint = baseServerUrl + "/v2/fhir/Patient/";
  }

  /**
   * Verifies that searching by a known existing part D contract number returns a 200 and the
   * unhashed MBI values are returned by default.
   *
   * <p>V1 handles the response differently, namely returning historical MBIs whereas v2 doesnt,
   * which means this test needs to be split between v1/v2
   */
  @Test
  public void testPatientByPartDContractExpectUnhashedMbis() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        /* Check there is 1 unhashed bene ids returned (regression test for  BFD-525; we should have a single current mbi) */
        .body(
            "entry[0].resource.identifier.findAll { it.system == 'http://hl7.org/fhir/sid/us-mbi' }.size()",
            equalTo(1))
        .body("entry.resource.id", hasItem(String.valueOf(beneficiary.getBeneficiaryId())))
        // Check current MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Historical benes are not returned from contract search in v2, they are only
        // added when searching by mbi hash. See R4PatientResourceProvider.queryDatabaseByHash
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that searching by a known existing part D contract number with paging requested
   * returns a 200 as well as the expected paging links.
   */
  @Test
  public void testPatientByPartDContractWhenPaginationExpectPagingLinks() {
    ServerTestUtils.get()
        .loadData(
            Arrays.asList(
                StaticRifResource.SAMPLE_A_BENES, StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY));
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear
            + "&_count=1";

    Response response =
        given()
            .spec(requestAuth)
            .headers(headers)
            .expect()
            .log()
            .body()
            .body("resourceType", equalTo("Bundle"))
            // Should match the paging size
            .body("entry.size()", equalTo(1))
            // Check pagination has the right number of links
            .body("link.size()", equalTo(3))
            /* Patient (specifically search by contract) uses different paging
            than all other resources, due to using bene id cursors.
            There is no "last" page or "previous", only first/next/self
            */
            .body("link.relation", hasItems("first", "next", "self"))
            .statusCode(200)
            .when()
            .get(requestString);

    // Try to get the next page
    String nextLink = testUtils.getPaginationLink(response, "next");

    // However, there is no next page. V2 Patient contract pagination doesnt check this until its
    // called
    given()
        .spec(requestAuth)
        .headers(headers)
        .urlEncodingEnabled(false)
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        // Check there were no additional results
        .body("$", not(hasKey("entry")))
        // We should only have first and self link now
        .body("link.size()", equalTo(2))
        .body("link.relation", hasItems("first", "self"))
        .statusCode(200)
        .when()
        .get(nextLink);
  }

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is not requested. (This oddly specific combo had been bugged earlier
   * and was quite tricky to resolve).
   */
  @Test
  public void testPatientByPartDContractWithNoPagingAndMultipleMbisExpectNoDupes() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResource.SAMPLE_A_BENES,
                    StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY));
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(loadedRecords);
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // Verify that the bene wasn't duplicated, per the bugfix
        .body("entry.size()", equalTo(1))
        .body("entry.resource.id", hasItem(String.valueOf(beneficiary.getBeneficiaryId())))
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Double-check that the mbi only appears once and is not duplicated
        .body(
            "entry[0].resource.identifier.findAll { it.system == '"
                + TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED
                + "' }",
            hasSize(1))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that access.json is written to within BFD-server-war via API call and has the MDC keys
   * expected for Patient by part D contract.
   */
  @Test
  public void testPatientByPartDContractHasAccessJsonWithExpectedMdcKeys() throws IOException {
    testUtils.loadSampleAData();
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear;

    List<String> additionalExpectedMdcKeys = new ArrayList<>(MDC_EXPECTED_BASE_KEYS);
    additionalExpectedMdcKeys.add(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys, headers);
  }

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is requested. (This oddly specific combo had been bugged earlier and
   * was quite tricky to resolve).
   */
  @Test
  public void testPatientByPartDContractWithPagingAndMultipleMbisExpectNoDupes() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResource.SAMPLE_A_BENES,
                    StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY));
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(loadedRecords);
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear
            + "&_count=1";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // Verify that the bene wasn't duplicated, per the bugfix
        .body("entry.size()", equalTo(1))
        .body("entry.resource.id", hasItem(String.valueOf(beneficiary.getBeneficiaryId())))
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Double-check that the mbi only appears once and is not duplicated
        .body(
            "entry[0].resource.identifier.findAll { it.system == '"
                + TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED
                + "' }",
            hasSize(1))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 when using HTTP POST with an unhashed
   * MBI.
   */
  @Test
  public void testPatientUsingPostByIdentifierUsingUnhashedMbi() {
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();

    // _search needed to distinguish the POST version of the endpoint (HAPI-FHIR)
    String requestString = patientEndpoint + "_search";
    String lookupMbi = "9AB2WW3GR44";
    String formParams =
        "_id="
            + TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED
            + "|"
            + lookupMbi;

    RequestSpecification httpRequest =
        given()
            .header("Content-Type", "application/x-www-form-urlencoded")
            .spec(requestAuth)
            .body(formParams);

    // Should return a 200
    Response response = httpRequest.post(requestString);
    assertThat(response.statusCode(), equalTo(200));
    JsonPath jsonPathEvaluator = response.jsonPath();
    assertThat(jsonPathEvaluator.get("total"), equalTo(1));
    // fetch all MBIs as a list
    List<String> mbis = jsonPathEvaluator.getList("entry.resource.identifier.value.flatten()");
    assertThat(mbis.size(), equalTo(7));
    assertThat(mbis.contains(currentMbi), is(true));
    assertThat(mbis.containsAll(historicalMbis), is(true));
  }
}
