package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import ca.uhn.fhir.model.primitive.DateTimeDt;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import io.restassured.response.Response;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** Abstract base class for RestAssured Patient E2E tests shared between BFD versions. */
public class PatientE2EBase extends ServerRequiredTest {

  /** The base patient endpoint. */
  protected static String patientEndpoint;

  /**
   * A list of expected historical mbis for adding to the sample A loaded data (as data coming back
   * from the endpoint will have this added in the resource provider).
   */
  protected static final List<String> historicalMbis =
      List.of("9AB2WW3GR44", "543217066", "3456689");

  /** The current Mbi as found in the SAMPLE A data. */
  protected static final String currentMbi = "3456789";

  /** Verifies patient read with an existing bene returns a 200 and response. */
  @Test
  public void testReadWhenExistingPatientExpect200() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + patientId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Patient"))
        .body("id", equalTo(patientId))
        // Check current MBI is returned
        .body("identifier.value", hasItem(currentMbi))
        // Check historical MBIs are returned too
        .body("identifier.value", hasItems(historicalMbis.toArray()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response when the beneficiary exists
   * in the DB but has no {@link BeneficiaryHistory} or MedicareBeneficiaryIdHistory records.
   * Primarily this checks that the table joins do not cause any issue retrieving the patient when
   * there is nothing found in the history table.
   */
  @Test
  public void testReadWhenNoHistoricalMbisExpect200() {
    // Load data without bene history
    List<Object> loadedRecords =
        ServerTestUtils.get().loadData(Arrays.asList(StaticRifResource.SAMPLE_A_BENES));
    String patientId = testUtils.getPatientId(loadedRecords);
    String requestString = patientEndpoint + patientId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Patient"))
        .body("id", equalTo(patientId))
        // Check current MBI is returned
        .body("identifier.value", hasItem(currentMbi))
        // Check historical MBIs are returned too
        .body("identifier.value", not(hasItems(historicalMbis.toArray())))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and response for a {@link Patient} that
   * exists in the DB when searching by hashed mbi.
   */
  @Test
  public void testPatientByIdentifierWhenCurrentMbiHashExpect200() {
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    String mbiHash = beneficiary.getMbiHash().orElseThrow();
    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 3 entries, since we set page size to 3
        .body("entry.size()", equalTo(1))
        // Check current MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Check historical MBIs are returned too
        .body("entry.resource.identifier.value.flatten()", hasItems(historicalMbis.toArray()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 404 for a searched MBI that points to more
   * than one bene id in the Beneficiaries table.
   */
  @Test
  public void testPatientByIdentifierWhenMbiHashWithBeneDupesExpect404() {
    // The test setup will load additional records which make the current mbi point to 2 different
    // bene ids
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // history has the same mbi, so returns the same hash;
    // basically second param doesnt matter for this test
    String mbiHash = getMbiHash(currentMbi, false, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    // Should return a 404
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(404)
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("By hash query found more than one distinct BENE_ID: 5"))
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 404 for a searched MBI that points to more
   * than one bene id in the Bene History table.
   */
  @Test
  public void testPatientByIdentifierWhenMbiHashWithHistoryBeneDupesExpect404() {
    // The test setup will load additional records which make the current mbi point to 2 different
    // bene ids
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    String mbiHash = getMbiHash("DUPHISTMBI", true, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    // Should return a 404
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(404)
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("By hash query found more than one distinct BENE_ID: 2"))
        .when()
        .get(requestString);
  }

  /**
   * Verifies that when the search mbi exists in both the Beneficiary and Bene History tables,
   * Patient searchByIdentifier returns a 200 and the data in the beneficiary table.
   *
   * <p>This verifies that if there is a choice of which table to sample data from for the same MBI
   * lookup, the Beneficiary table is used over the history table
   */
  @Test
  public void testPatientByIdentifierWhenMbiInMultipleTablesExpectDataReturnedFromBeneTable() {
    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The BENE_BIRTH_DT
     * is different between the tables so the bene record from the
     * Beneficiaries table can be verified to have been returned.
     *
     * mbi=SAMEMBI BENE_BIRTH_DT=17-MAR-1981 should be pulled back.
     * BENE_BIRTH_DT=17-MAR-1980 indicates a response from bene history
     */
    String unhashedMbi = "SAMEMBI";
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // history value doesnt matter for this test
    String mbiHash = getMbiHash(unhashedMbi, true, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    // Should return a 200 with the bene table data
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(unhashedMbi))
        // No historical MBI is returned, since the historical MBI is the same as current
        // Check the birthdate is 1981, indicating it came from the bene table instead of history
        .body("entry.resource.birthDate", hasItem("1981-03-17"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that when the search mbi exists in only the Bene History table, but has a shared bene
   * id in the Bene table, Patient searchByIdentifier returns a 200 and the data in the history
   * table as well as the related Beneficiary data.
   */
  @Test
  public void testPatientByIdentifierWhenSearchByHistoricalMbiExpectCurrentAndHistoricalMbiData() {
    String historicalMbi = "HISTMBI";
    String expectedBeneTableMbi = "565654MBI";
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // search mbi doesnt exist in the bene table, so bene history search must be true
    String mbiHash = getMbiHash(historicalMbi, true, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    // Should return a 200 with the history+bene mbis
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search historical MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(historicalMbi))
        // Check the that the "new" mbi from the beneficiary table is also returned
        .body("entry.resource.identifier.value.flatten()", hasItem(expectedBeneTableMbi))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and response for a {@link Patient} that
   * exists in the DB, an MBI points to a single bene id in the Beneficiaries table, and has no Bene
   * History data.
   */
  @Test
  public void testPatientByIdentifierWhenMbiHashWithNoDupesExpect200() {
    String unhashedSearchMbi = "3456789N";
    // This mbi only exists in bene data, so dont check history
    boolean useMbiHashFromBeneHistory = false;

    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    String mbiHash = getMbiHash(unhashedSearchMbi, useMbiHashFromBeneHistory, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    // based on the inputs, we expect a single result
    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(unhashedSearchMbi))
        // nothing in history, so no need to check
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and an empty bundle when no results are
   * found for the specified hashed mbi.
   */
  @Test
  public void testPatientByIdentifierWhenNoResultsExpect200() {
    String searchMbiHash = "notfoundmbi";

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + searchMbiHash;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("total", equalTo(0))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response for a {@link Patient} that
   * exists in the DB.
   */
  @Test
  public void testPatientByLogicalIdWhenExistingBeneIdExpect200() {

    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check current MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Check historical MBIs are returned too
        .body("entry.resource.identifier.value.flatten()", hasItems(historicalMbis.toArray()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that searching by lastUpdated with its various supported prefixes returns results as
   * expected.
   */
  @Test
  public void testPatientByLogicalIdWithLastUpdatedExpectFilteredResults() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());

    // Build up a list of lastUpdatedURLs that return > all values
    String nowDateTime = new DateTimeDt(Date.from(Instant.now().plusSeconds(1))).getValueAsString();
    String earlyDateTime = "2019-10-01T00:00:00+00:00";
    List<String> datesWithOneExpectedResult =
        Arrays.asList(
            "_lastUpdated=gt" + earlyDateTime,
            "_lastUpdated=ge" + earlyDateTime,
            "_lastUpdated=le" + nowDateTime,
            "_lastUpdated=ge" + earlyDateTime + "&_lastUpdated=le" + nowDateTime,
            "_lastUpdated=gt" + earlyDateTime + "&_lastUpdated=lt" + nowDateTime);

    // Search for each lastUpdated value which should return 1 result
    for (String lastUpdatedValue : datesWithOneExpectedResult) {
      String theSearchUrl = patientEndpoint + "?_id=" + patientId + "&" + lastUpdatedValue;

      given()
          .spec(requestAuth)
          .expect()
          .body("resourceType", equalTo("Bundle"))
          .body("entry.size()", equalTo(1))
          .statusCode(200)
          .when()
          .get(theSearchUrl);
    }

    List<String> datesWithZeroExpectedResult =
        Arrays.asList("_lastUpdated=lt" + earlyDateTime, "_lastUpdated=le" + earlyDateTime);

    for (String lastUpdatedValue : datesWithZeroExpectedResult) {
      String theSearchUrl = patientEndpoint + "?_id=" + patientId + "&" + lastUpdatedValue;

      given()
          .spec(requestAuth)
          .expect()
          .body("resourceType", equalTo("Bundle"))
          .body("total", equalTo(0))
          .statusCode(200)
          .when()
          .get(theSearchUrl);
    }
  }

  /**
   * Verifies that searching by a known existing part D contract number returns a 200 and the
   * unhashed MBI values are returned by default.
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
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        .body("entry.resource.id", hasItem(String.valueOf(beneficiary.getBeneficiaryId())))
        // Check current MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Historical benes are not returned from contract search, since they are only
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
            .expect()
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

    // However, there is no next page. Patient contract pagination doesnt check this until its
    // called
    given()
        .spec(requestAuth)
        .urlEncodingEnabled(false)
        .expect()
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
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} works as expected,
   * when an invalid year is specified.
   */
  @Test
  public void testPatientByPartDContractWithInvalidYearExpect400() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResource.SAMPLE_A_BENES,
                    StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY));
    Beneficiary beneficiary = testUtils.getFirstBeneficiary(loadedRecords);
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|ABC";
    String requestString =
        patientEndpoint
            + "?_has:Coverage.extension="
            + contractId
            + "&_has:Coverage.rfrncyr="
            + refYear;

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("Contract year must be a number."))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Patient throws a 400 error when the paging start (startIndex) is set higher than
   * the maximum number of results.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartBeyondMaxExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=2&startIndex=12";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (12) must be less than than result size (1)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Patient throws a 400 error when the paging start (startIndex) is set to the maximum
   * number of results, since the highest index must be less than the number of results as a 0-based
   * index.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartSetToMaxResultsExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=1&startIndex=1";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (1) must be less than than result size (1)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Patient does not error when the paging start (startIndex) is set to be equal to one
   * less than the maximum number of results.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartOneLessThanMaxExpect200() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=1&startIndex=0";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        .body("total", equalTo(1))
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
  public void testPatientByLogicalIdWithNoResultsAndPaginationRequestedExpect200() {
    String patientId = "0";
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=50";

    given()
        .spec(requestAuth)
        .expect()
        .body("total", equalTo(0))
        .statusCode(200)
        .when()
        .get(requestString);

    // check with startIndex as well
    requestString = patientEndpoint + "?_id=" + patientId + "&startIndex=2";

    given()
        .spec(requestAuth)
        .expect()
        .body("total", equalTo(0))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Loads the sample A data with additional bene history records loaded. Used to test various
   * hashed mbi cases.
   *
   * @return the list of loaded records
   */
  private List<Object> loadDataWithAdditionalBeneHistory() {

    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    // load additional Beneficiary and Beneficiary History records
    loadedRecords.addAll(
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_HICN_MULT_BENES.getResources())));

    return loadedRecords;
  }

  /**
   * Gets the hashed mbi based on the passed unhashed MBI value using the loaded data to look for
   * the corresponding mbi hash. Will pull from the bene history data instead of beneficiary if
   * useMbiFromHistoryTable is set to true.
   *
   * @param unhashedMbiValue the unhashed mbi value
   * @param useMbiHashFromBeneHistory if the hashed mbi should be checked in the bene history table
   *     instead of beneficiaries
   * @param loadedRecords the loaded records to check for the mbi
   * @return the mbi hash, or an exception if no mbi was found that matched the input
   */
  private String getMbiHash(
      String unhashedMbiValue, boolean useMbiHashFromBeneHistory, List<Object> loadedRecords) {

    if (useMbiHashFromBeneHistory) {
      Stream<BeneficiaryHistory> beneficiariesHistoryStream =
          loadedRecords.stream()
              .filter(r -> r instanceof BeneficiaryHistory)
              .map(r -> (BeneficiaryHistory) r);
      List<BeneficiaryHistory> beneficiariesHistoryList = beneficiariesHistoryStream.toList();
      BeneficiaryHistory beneficiaryHistoryMbiToMatchTo =
          beneficiariesHistoryList.stream()
              .filter(r -> unhashedMbiValue.equals(r.getMedicareBeneficiaryId().orElseThrow()))
              .findFirst()
              .orElseThrow();

      return beneficiaryHistoryMbiToMatchTo.getMbiHash().orElseThrow();
    } else {
      Stream<Beneficiary> beneficiariesStream =
          loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r);
      List<Beneficiary> beneficiariesList = beneficiariesStream.toList();
      Beneficiary beneficiaryMbiToMatchTo =
          beneficiariesList.stream()
              .filter(r -> unhashedMbiValue.equals(r.getMedicareBeneficiaryId().orElseThrow()))
              .findFirst()
              .orElseThrow();

      return beneficiaryMbiToMatchTo.getMbiHash().orElseThrow();
    }
  }
}
