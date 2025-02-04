package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.PatientE2EBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V1 Patient endpoint. */
public class PatientE2E extends PatientE2EBase {

  /**
   * The Current HICN for verifying the unhashed HICN was returned. HICN only exists in V1, so all
   * HICN related tests are only for these V1 tests.
   */
  private final String currentHicn = "543217066U";

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set up to return mbi as base case, so we can run all the shared tests that check mbi returns
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "mbi");
    patientEndpoint = baseServerUrl + "/v1/fhir/Patient/";
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to true + address
   * returns a 200 and response which contains HICN, MBI, and address fields. Setting the field to
   * true should be equivalent to setting it to "hicn,mbi" and return both.
   */
  @Test
  public void testReadWhenIncludeIdentifiersTrueAndAddressExpectHicnMbiAndAddress() {
    headers =
        Map.of(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true");
    boolean expectHicn = true;
    boolean expectMbi = true;
    boolean expectAddressFields = true;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to false returns a
   * 200 and response which does not contain unhashed HICN or MBI. Setting the field to false should
   * be equivalent to not setting it.
   */
  @Test
  public void testReadWhenIncludeIdentifiersFalseExpectNoUnhashedHicnOrMbi() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "false");
    boolean expectHicn = false;
    boolean expectMbi = false;
    boolean expectAddressFields = false;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to hicn and mbi
   * explicitly + address returns a 200 and response which contains HICN, MBI, and address fields.
   */
  @Test
  public void testReadWhenIncludeIdentifiersExplicitAndAddressExpectHicnMbiAndAddress() {
    // Set hicn and mbi explicitly
    headers =
        Map.of(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "hicn,mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true");
    boolean expectHicn = true;
    boolean expectMbi = true;
    boolean expectAddressFields = true;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to HICN returns a
   * 200 and response which contains the unhashed HICN, but not the unhashed mbi.
   */
  @Test
  public void testReadWhenIncludeIdentifiersHicnAndAddressExpectHicn() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "hicn");
    boolean expectHicn = true;
    boolean expectMbi = false;
    boolean expectAddressFields = false;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to mbi returns a 200
   * and response which contains the unhashed mbi.
   */
  @Test
  public void testReadWhenIncludeIdentifiersMbiAndAddressExpectMbi() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "mbi");
    boolean expectHicn = false;
    boolean expectMbi = true;
    boolean expectAddressFields = false;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header set to return address fields returns a
   * 200 and response which contains the address fields.
   */
  @Test
  public void testReadWhenIncludeAddressFieldHeaderExpectAddressFields() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true");
    boolean expectHicn = false;
    boolean expectMbi = false;
    boolean expectAddressFields = true;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to an empty string
   * returns a 200 and response which does not contain unhashed HICN or MBI.
   */
  @Test
  public void testReadWhenIncludeIdentifiersBlankExpectNoUnhashedHicnOrMbi() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "");
    boolean expectHicn = false;
    boolean expectMbi = false;
    boolean expectAddressFields = false;

    verifyReadExsistingPatientWithHeaders(expectHicn, expectMbi, expectAddressFields);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to an invalid string
   * returns a 400 and error message.
   */
  @Test
  public void testReadWhenIncludeIdentifiersInvalidExpect400() {
    headers =
        Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "invalid-identifier-value");
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + patientId;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem(
                "Unsupported IncludeIdentifiers Header Value: |invalid-identifier-value|, invalid-identifier-value"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verifies patient read with an existing bene and header for identifiers set to an invalid string
   * alongside a proper value returns a 400 and error message.
   */
  @Test
  public void testReadWhenIncludeIdentifiersInvalidAndGoodMixExpect400() {
    headers =
        Map.of(
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "mbi,invalid-identifier-value");
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + patientId;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem(
                "Unsupported IncludeIdentifiers Header Value: |invalid-identifier-value|, mbi,invalid-identifier-value"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response with unhashed identifiers
   * for a {@link Patient} that exists in the DB when includeIdentifiers=true header is added.
   *
   * <p>This test only tests HICN as this is the only unique part of this test for V1; see base test
   * testPatientByLogicalIdWhenExistingBeneIdExpect200 for checking mbi with identifiers.
   */
  @Test
  public void testPatientByLogicalIdWhenExistingBeneAndIncludeIdentifiersExpectHicn() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check unhashed HICN is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentHicn))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response with no unhashed identifiers
   * for a {@link Patient} that exists in the DB when includeIdentifiers=false header is added.
   */
  @Test
  public void testPatientByLogicalIdWhenExistingBeneAndIncludeIdentifiersFalseExpectNoUnhashed() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "false");
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check neither the unhashed mbi nor HICN is returned
        .body("entry.resource.identifier.value.flatten()", not(hasItems(currentHicn, currentMbi)))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier by hashed mbi returns a 200 and does not return the
   * unhashed mbi if the IncludeIdentifiers header is set to false.
   */
  @Test
  public void testPatientByIdentifierWhenMbiHashAndIdentifierHeaderFalseExpectNoUnhashedMbi() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "false");
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String mbiHash = getMbiHash(currentMbi, false, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check unhashed MBI is not returned
        .body("entry.resource.identifier.value.flatten()", not(hasItem(currentMbi)))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and response for a {@link Patient} that
   * exists in the DB, an HICN points to a single bene id in the Beneficiaries table, and has no
   * Bene History data. Also check when include identifiers headers are set to true, we get back the
   * unhashed mbi and HICN.
   */
  @Test
  public void testPatientByIdentifierWhenHicnHashAndIncludeIdentifiersExpect200() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    boolean useMbiHashFromBeneHistory = false;
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String unhashedSearchHicn =
        testUtils.getFirstBeneficiary(loadedRecords).getHicnUnhashed().orElseThrow();

    String hicnHash = getHicnHash(unhashedSearchHicn, useMbiHashFromBeneHistory, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI and HICN is returned (due to includeIdentifiers)
        .body("entry.resource.identifier.value.flatten()", hasItems(unhashedSearchHicn, currentMbi))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and response for a {@link Patient} that
   * exists in the DB, an HICN points to a single bene id in the Beneficiaries table, and has no
   * Bene History data. Also check when include identifiers headers are set to false, we dont get
   * back the unhashed mbi or HICN.
   */
  @Test
  public void testPatientByIdentifierWhenHicnHashAndIncludeIdentifiersFalseExpect200() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "false");
    boolean useMbiHashFromBeneHistory = false;
    List<Object> loadedRecords = testUtils.loadSampleAData();
    String unhashedSearchHicn =
        testUtils.getFirstBeneficiary(loadedRecords).getHicnUnhashed().orElseThrow();

    String hicnHash = getHicnHash(unhashedSearchHicn, useMbiHashFromBeneHistory, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI or HICN not returned (due to includeIdentifiers=false)
        .body(
            "entry.resource.identifier.value.flatten()",
            not(hasItems(unhashedSearchHicn, currentMbi)))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 200 and an empty bundle when no results are
   * found for the specified hashed hicn.
   */
  @Test
  public void testPatientByIdentifierWhenNoResultsExpect200() {
    String searchHicnHash = "notfoundhicn";
    testUtils.loadSampleAData();
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + searchHicnHash;

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("total", equalTo(0))
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
  public void testPatientByIdentifierWhenMbiHashWithNoHistoryExpect200() {
    String unhashedSearchHicn = "543217066N";
    // This hicn only exists in bene data, so dont check history
    boolean useMbiHashFromBeneHistory = false;
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");

    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    String hicnHash = getHicnHash(unhashedSearchHicn, useMbiHashFromBeneHistory, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    // based on the inputs, we expect a single result
    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(unhashedSearchHicn))
        // nothing in history, so no need to check
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 404 for a searched HICN that points to more
   * than one bene id in the Beneficiaries table.
   */
  @Test
  public void testPatientByIdentifierWhenMbiHashWithBeneDupesExpect404() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    // The test setup will load additional records which make the current hicn point to 2 different
    // bene ids
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // history has the same hicn, so returns the same hash;
    // basically second param doesnt matter for this test
    String hicnHash = getHicnHash("543217066U", false, loadedRecords);

    Map<String, List<Beneficiary>> beneficiaryMap =
        loadedRecords.stream()
            .filter(Beneficiary.class::isInstance)
            .map(Beneficiary.class::cast)
            .collect(Collectors.groupingBy(Beneficiary::getHicn));

    List<Long> distinctBeneIdList =
        beneficiaryMap.get(hicnHash).stream().map(Beneficiary::getBeneficiaryId).sorted().toList();

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    // Should return a 404
    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .statusCode(404)
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem(
                "By hash query found more than one distinct BENE_ID: "
                    + distinctBeneIdList.size()
                    + ", DistinctBeneIdsList: "
                    + distinctBeneIdList))
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByIdentifier returns a 404 for a searched HICN that points to more
   * than one bene id in the Bene History table.
   */
  @Test
  public void testPatientByIdentifierWhenHicnHashWithHistoryBeneDupesExpect404() {
    // The test setup will load additional records which make the current mbi point to 2 different
    // bene ids
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    String hicnHash = getHicnHash("DUPHISTHIC", true, loadedRecords);

    Map<String, List<BeneficiaryHistory>> beneficiaryMap =
        loadedRecords.stream()
            .filter(BeneficiaryHistory.class::isInstance)
            .map(BeneficiaryHistory.class::cast)
            .collect(Collectors.groupingBy(BeneficiaryHistory::getHicn));

    List<Long> distinctBeneIdList =
        beneficiaryMap.get(hicnHash).stream()
            .map(BeneficiaryHistory::getBeneficiaryId)
            .sorted()
            .toList();

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    // Should return a 404
    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .statusCode(404)
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem(
                "By hash query found more than one distinct BENE_ID: "
                    + distinctBeneIdList.size()
                    + ", DistinctBeneIdsList: "
                    + distinctBeneIdList))
        .when()
        .get(requestString);
  }

  /**
   * Verifies that when the search HICN exists in both the Beneficiary and Bene History tables,
   * Patient searchByIdentifier returns a 200 and the data in the beneficiary table.
   *
   * <p>This verifies that if there is a choice of which table to sample data from for the same HICN
   * lookup, the Beneficiary table is used over the history table
   */
  @Test
  public void testPatientByIdentifierWhenHicnInMultipleTablesExpectDataReturnedFromBeneTable() {
    /*
     * The following scenario tests when the same mbi is in the
     * Beneficiaries and also in the BeneficiariesHistory table. The BENE_BIRTH_DT
     * is different between the tables so the bene record from the
     * Beneficiaries table can be verified to have been returned.
     *
     * mbi=SAMEMBI BENE_BIRTH_DT=17-MAR-1981 should be pulled back.
     * BENE_BIRTH_DT=17-MAR-1980 indicates a response from bene history
     */
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    String unhashedHicn = "SAMEHICN";
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // history value doesnt matter for this test
    String hicnHash = getHicnHash(unhashedHicn, true, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    // Should return a 200 with the bene table data
    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(unhashedHicn))
        // No historical MBI is returned, since the historical MBI is the same as current
        // Check the birthdate is 1981, indicating it came from the bene table instead of history
        .body("entry.resource.birthDate", hasItem("1981-03-17"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that when the search HICN exists in only the Bene History table, but has a shared bene
   * id in the Bene table, Patient searchByIdentifier returns a 200 and the data in the history
   * table as well as the related Beneficiary data.
   */
  @Test
  public void
      testPatientByIdentifierWhenSearchByHistoricalHicnExpectCurrentAndHistoricalHicnData() {
    headers = Map.of(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "true");
    String historicalHicn = "HISTHICN";
    String expectedBeneTableHicn = "565654HICN";
    List<Object> loadedRecords = loadDataWithAdditionalBeneHistory();
    // search hicn doesnt exist in the bene table, so bene history search must be true
    String hicnHash = getHicnHash(historicalHicn, true, loadedRecords);

    String requestString =
        patientEndpoint
            + "?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
            + "|"
            + hicnHash;

    // Should return a 200 with the history+bene hicns
    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        // Check search historical MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(historicalHicn))
        // Check the that the "new" mbi from the beneficiary table is also returned
        .body("entry.resource.identifier.value.flatten()", hasItem(expectedBeneTableHicn))
        .statusCode(200)
        .when()
        .get(requestString);
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
        /* Check there are 4 unhashed bene ids returned (regression test for  BFD-525; we should have 4 unique entries, 3 historical and 1 current mbi) */
        .body(
            "entry[0].resource.identifier.findAll { it.system == 'http://hl7.org/fhir/sid/us-mbi' }.size()",
            equalTo(4))
        .body("entry.resource.id", hasItem(String.valueOf(beneficiary.getBeneficiaryId())))
        // Check current MBI is returned
        .body("entry.resource.identifier.value.flatten()", hasItem(currentMbi))
        // Historical benes are returned from contract search in v1
        .body("entry.resource.identifier.value.flatten()", hasItems(historicalMbis.toArray()))
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

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // Should match the paging size
        .body("entry.size()", equalTo(1))
        // Check pagination has the right number of links
        .body("link.size()", equalTo(2))
        // V1 patient wont show a next page when there isnt one, unlike v2
        .body("link.relation", hasItems("first", "self"))
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
    additionalExpectedMdcKeys.add(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_IDENTIFIERS);

    ServerTestUtils.assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalExpectedMdcKeys, headers);
  }

  /**
   * Verifies that calling read with a known bene id and the headers as set prior to calling this
   * method will return results aligned with the specified expectations.
   *
   * @param expectHicn if the unhashed HICN is expected to be returned
   * @param expectMbi if the unhashed MBI is expected to be returned
   * @param expectAddress if the address fields are expected to be returned
   */
  private void verifyReadExsistingPatientWithHeaders(
      boolean expectHicn, boolean expectMbi, boolean expectAddress) {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + patientId;

    // Default to expecting items, and invert if the passed-in values indicate
    Matcher<Iterable<? super String>> hasHicnMatcher = hasItem(currentHicn);
    Matcher<Iterable<? super String>> hasHicnSystemMatcher =
        hasItem(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED);
    if (!expectHicn) {
      hasHicnMatcher = not(hasHicnMatcher);
      hasHicnSystemMatcher = not(hasHicnSystemMatcher);
    }

    Matcher<Iterable<? super String>> hasMbiMatcher = hasItem(currentMbi);
    Matcher<Iterable<? super String>> hasMbiSystemMatcher =
        hasItem(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED);
    if (!expectMbi) {
      hasMbiMatcher = not(hasMbiMatcher);
      hasMbiSystemMatcher = not(hasMbiSystemMatcher);
    }

    String addressBodyCheck = "address.size()";
    String addressLineCheck = "address.line.flatten().size()";

    Matcher<Integer> hasAddressMatcher = greaterThan(0);
    Matcher<Integer> addressLineMatcher = greaterThan(0);

    if (!expectAddress) {
      addressBodyCheck = "$";
      hasAddressMatcher = not(hasProperty("address"));
      /* In the event we dont have an address, we dont need to check the lines,
      so just do the address check twice. */
      addressLineCheck = addressBodyCheck;
      addressLineMatcher = hasAddressMatcher;
    }

    given()
        .spec(requestAuth)
        .headers(headers)
        .expect()
        // Check mbi
        .body("identifier.value", hasMbiMatcher)
        .body("identifier.system", hasMbiSystemMatcher)
        // Check hicn
        .body("identifier.value", hasHicnMatcher)
        .body("identifier.system", hasHicnSystemMatcher)
        // Check address fields
        .body(addressBodyCheck, hasAddressMatcher)
        .body(addressLineCheck, addressLineMatcher)
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /** Tests the pagination links using a POST request with the pagination info in the POST body. */
  @Test
  public void testPatientByPartDContractWhenPaginationExpectPagingLinksPost() {
    ServerTestUtils.get()
        .loadData(
            Arrays.asList(
                StaticRifResource.SAMPLE_A_BENES, StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY));
    String contractId =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PTDCNTRCT01) + "|S4607";
    String refYear = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.RFRNC_YR) + "|2018";
    Map<String, String> formParams =
        Map.of("_has:Coverage.extension", contractId, "_has:Coverage.rfrncyr", refYear);
    String requestString = patientEndpoint + "_search?_count=1";

    given()
        .spec(requestAuth)
        .header(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "mbi")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .formParams(formParams)
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        // Should match the paging size
        .body("entry.size()", equalTo(1))
        // Check pagination has the right number of links
        .body("link.size()", equalTo(2))
        /* Patient (specifically search by contract) uses different paging
        than all other resources, due to using bene id cursors.
        There is no "last" page or "previous", only first/next/self
        */
        .body("link.relation", hasItems("first", "self"))
        .statusCode(200)
        .when()
        .post(requestString);
  }

  /**
   * Gets the hashed HICN based on the passed unhashed HICN value using the loaded data to look for
   * the corresponding mbi hash. Will pull from the bene history data instead of beneficiary if
   * useHashFromBeneHistory is set to true.
   *
   * @param unhashedHicnValue the unhashed hicn value
   * @param useHashFromBeneHistory if the hashed hicn should be checked in the bene history table
   *     instead of beneficiaries
   * @param loadedRecords the loaded records to check for the hicn
   * @return the hicn hash, or an exception if no hicn was found that matched the input
   */
  private String getHicnHash(
      String unhashedHicnValue, boolean useHashFromBeneHistory, List<Object> loadedRecords) {

    if (useHashFromBeneHistory) {
      Stream<BeneficiaryHistory> beneficiariesHistoryStream =
          loadedRecords.stream()
              .filter(r -> r instanceof BeneficiaryHistory)
              .map(r -> (BeneficiaryHistory) r);
      List<BeneficiaryHistory> beneficiariesHistoryList = beneficiariesHistoryStream.toList();
      BeneficiaryHistory beneficiaryHistoryHashToMatchTo =
          beneficiariesHistoryList.stream()
              .filter(r -> unhashedHicnValue.equals(r.getHicnUnhashed().orElseThrow()))
              .findFirst()
              .orElseThrow();

      return beneficiaryHistoryHashToMatchTo.getHicn();
    } else {
      Stream<Beneficiary> beneficiariesStream =
          loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r);
      List<Beneficiary> beneficiariesList = beneficiariesStream.toList();
      Beneficiary beneficiaryHashToMatchTo =
          beneficiariesList.stream()
              .filter(r -> unhashedHicnValue.equals(r.getHicnUnhashed().orElseThrow()))
              .findFirst()
              .orElseThrow();

      return beneficiaryHashToMatchTo.getHicn();
    }
  }
}
