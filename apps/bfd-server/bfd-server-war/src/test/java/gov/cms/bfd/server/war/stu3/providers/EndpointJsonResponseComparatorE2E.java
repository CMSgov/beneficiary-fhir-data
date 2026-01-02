package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.EndpointJsonComparatorBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * This set of tests compare the application's current responses to a set of previously-recorded
 * responses. This achieves several goals:
 *
 * <ul>
 *   <li>It helps us to ensure that we're not accidentally changing the application's responses
 *   <li>It helps us to maintain backwards compatibility.
 *   <li>As any changes in an operation's output will have to include a change to the recorded
 *       response, it makes it much easier to tell what our PRs are actually doing.
 * </ul>
 *
 * <p>There SHALL be a 1:1 relationship between test cases here and the application's operations;
 * every supported operation should have a test case.
 *
 * <p>Note that our responses include timestamps and have other differences from request to request
 * (e.g. element ordering). Each test case must ignore or otherwise work around such differences so
 * that tests work reliably.
 *
 * <p>To re-generate the recorded responses, build the application with -DgenerateTestData=true
 * which will run * the test that creates the endpoint responses.
 */
public final class EndpointJsonResponseComparatorE2E extends EndpointJsonComparatorBase {

  /**
   * Returns data for parameterized tests.
   *
   * @return the data
   */
  public static Stream<Arguments> data() {
    return Stream.of(
        arguments("metadata", (Supplier<String>) EndpointJsonResponseComparatorE2E::metadata),
        arguments("patientRead", (Supplier<String>) EndpointJsonResponseComparatorE2E::patientRead),
        arguments(
            "patientReadWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorE2E::patientReadWithIncludeIdentifiers),
        arguments(
            "patientSearchById",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::patientSearchById),
        arguments(
            "patientSearchByIdWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorE2E::patientSearchByIdWithIncludeIdentifiers),
        arguments(
            "patientByIdentifier",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::patientByIdentifier),
        arguments(
            "patientByIdentifierWithoutReferenceYear",
            (Supplier<String>)
                EndpointJsonResponseComparatorE2E::patientByIdentifierWithoutReferenceYear),
        arguments(
            "patientByIdentifierWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorE2E::patientByIdentifierWithIncludeIdentifiers),
        arguments(
            "coverageRead", (Supplier<String>) EndpointJsonResponseComparatorE2E::coverageRead),
        arguments(
            "coverageReadWithoutReferenceYear",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::coverageReadWithoutReferenceYear),
        arguments(
            "coverageSearchByPatientId",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::coverageSearchByPatientId),
        arguments(
            "eobByPatientIdAll",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobByPatientIdAll),
        arguments(
            "eobByPatientIdAllWithSamhsa",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobByPatientIdAllWithSamhsa),
        arguments(
            "eobByPatientIdPaged",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobByPatientIdPaged),
        arguments(
            "eobReadCarrier", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadCarrier),
        arguments(
            "eobReadCarrierWithTaxNumbers",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadCarrierWithTaxNumbers),
        arguments(
            "eobReadCarrierMultipleLines",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadCarrierWithMultipleLines),
        arguments("eobReadDme", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadDme),
        arguments(
            "eobReadDmeWithTaxNumbers",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadDmeWithTaxNumbers),
        arguments("eobReadHha", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadHha),
        arguments(
            "eobReadHospice", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadHospice),
        arguments(
            "eobReadInpatient",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadInpatient),
        arguments(
            "eobReadOutpatient",
            (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadOutpatient),
        arguments("eobReadPde", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadPde),
        arguments("eobReadSnf", (Supplier<String>) EndpointJsonResponseComparatorE2E::eobReadSnf));
  }

  /**
   * Gets the results of the {@link IGenericClient#fetchConformance()} operation.
   *
   * @return the metadata
   */
  public static String metadata() {
    String response = getJsonResponseFor(baseServerUrl + "/v1/fhir/metadata");
    return sortMetadataSearchParamArray(response);
  }

  /**
   * FIXME: Additional workaround due to HAPI not always returning array elements in the same order
   * for a specific searchParam JSON array in the capability statement. This method is only
   * necessary until the following issue has been resolved with HAPI:
   * https://github.com/jamesagnew/hapi-fhir/issues/1183
   *
   * <p>Before: { "type": "Patient", "profile": { "reference": "http://hl7.org/fhir/Profile/Patient"
   * }, "interaction": [ { "code": "read" }, { "code": "search-type" } ], "searchParam": [ { "name":
   * "identifier", "type": "token", "documentation": "A patient identifier" }, { "name": "_id",
   * "type": "token", "documentation": "The ID of the resource" } ] }
   *
   * <p>After: { "type": "Patient", "profile": { "reference": "http://hl7.org/fhir/Profile/Patient"
   * }, "interaction": [ { "code": "read" }, { "code": "search-type" } ], "searchParam": [ { "name":
   * "_id", "type": "token", "documentation": "The ID of the resource" }, { "name": "identifier",
   * "type": "token", "documentation": "A patient identifier" } ] }
   *
   * @param unsortedResponse the JSON string with an unsorted searchParam array
   * @return the JSON string with the sorted searchParam array
   */
  private static String sortMetadataSearchParamArray(String unsortedResponse) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    JsonNode parsedJson;
    try {
      parsedJson = mapper.readTree(unsortedResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }

    // This returns the searchParam node for the resource type='Patient' from
    // metadata.json
    JsonNode searchParamsArray = parsedJson.at("/rest/0/resource/3/searchParam");

    Iterator<JsonNode> searchParamsArrayIterator = searchParamsArray.elements();
    List<JsonNode> searchParams = new ArrayList<JsonNode>();
    while (searchParamsArrayIterator.hasNext()) {
      searchParams.add(searchParamsArrayIterator.next());
    }

    searchParams.sort(
        new Comparator<>() {
          /** {@inheritDoc} */
          @Override
          public int compare(JsonNode node1, JsonNode node2) {
            String name1 = node1.get("name").toString();
            String name2 = node2.get("name").toString();
            return name1.compareTo(name2);
          }
        });

    ((ArrayNode) searchParamsArray).removeAll();
    for (JsonNode searchParam : searchParams) {
      ((ArrayNode) searchParamsArray).add(searchParam);
    }

    String jsonResponse;
    try {
      jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }
    return jsonResponse;
  }

  /**
   * FIXME: Additional workaround due to HAPI not always returning array elements in the same order
   * for a specific searchParam JSON array in the capability statement. This method is only
   * necessary until the following issue has been resolved with HAPI:
   * https://github.com/jamesagnew/hapi-fhir/issues/1183
   *
   * <p>Before: { "type" : [ {"coding" : [ {"system" :
   * "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type", "code" : "principal",
   * "display" : "The single medical diagnosis that is most relevant to the patient's chief
   * complaint or need for treatment." ] }, {"coding" : [ {"system" :
   * "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type", "code" :
   * "external-first","display" : "The code used to identify the 1st external cause of injury,
   * poisoning, or other adverse effect."} } ]} ]}
   *
   * <p>After: { "type" : [ {"coding" : [ {"system" :
   * "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type", "code" :
   * "external-first","display" : "The code used to identify the 1st external cause of injury,
   * poisoning, or other adverse effect."} ] }, {"coding" : [ {"system" :
   * "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type","code" : "principal",
   * "display" : "The single medical diagnosis that is most relevant to the patient's chief
   * complaint or need for treatment."} ]} ]}
   *
   * @param unsortedResponse the JSON string with an unsorted diagnosisType array
   * @param parseStringAt the JSON string with the search string
   * @return the JSON string with the sorted diagnosis type array
   */
  private static String sortDiagnosisTypes(String unsortedResponse, String parseStringAt) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    JsonNode parsedJson;
    try {
      parsedJson = mapper.readTree(unsortedResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }

    // This returns the DiagnosisType array node for the resource
    JsonNode diagnosisTypeArray = parsedJson.at(parseStringAt);

    Iterator<JsonNode> diagnosisTypeArrayIterator = diagnosisTypeArray.elements();
    List<JsonNode> diagnosisTypes = new ArrayList<JsonNode>();
    while (diagnosisTypeArrayIterator.hasNext()) {
      diagnosisTypes.add(diagnosisTypeArrayIterator.next());
    }

    diagnosisTypes.sort(
        new Comparator<>() {
          /** {@inheritDoc} */
          @Override
          public int compare(JsonNode node1, JsonNode node2) {
            String name1 = node1.get("coding").get(0).get("code").toString();
            String name2 = node2.get("coding").get(0).get("code").toString();
            return name1.compareTo(name2);
          }
        });

    ((ArrayNode) diagnosisTypeArray).removeAll();
    for (JsonNode diagnosisType : diagnosisTypes) {
      ((ArrayNode) diagnosisTypeArray).add(diagnosisType);
    }

    String jsonResponse;
    try {
      jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }
    return jsonResponse;
  }

  /**
   * Executes a search against the Patient endpoint using the sample A bene id and returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String patientRead() {
    Beneficiary beneficiary = getSampleABene();

    String response =
        getJsonResponseFor(baseServerUrl + "/v1/fhir/Patient/" + beneficiary.getBeneficiaryId());

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint using the sample A bene id and {@link
   * PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi", then returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String patientReadWithIncludeIdentifiers() {
    Beneficiary beneficiary = getSampleABene();

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "hicn,mbi"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true"));
    String response =
        getJsonResponseFor(
            baseServerUrl + "/v1/fhir/Patient/" + beneficiary.getBeneficiaryId(), headers);

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint (search by logical id) using the sample A bene
   * id and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientSearchById() {
    Beneficiary beneficiary = getSampleABene();

    String response =
        getJsonResponseFor(
            baseServerUrl + "/v1/fhir/Patient?_id=" + beneficiary.getBeneficiaryId());

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint (search by logical id) using the sample A bene
   * id, {@link PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi", {@link
   * PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} set to true, and then returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String patientSearchByIdWithIncludeIdentifiers() {
    Beneficiary beneficiary = getSampleABene();

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "hicn,mbi"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true"));
    String response =
        getJsonResponseFor(
            baseServerUrl + "/v1/fhir/Patient?_id=" + beneficiary.getBeneficiaryId(), headers);

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn and then returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifier() {
    Beneficiary beneficiary = getSampleABene();

    String response =
        getJsonResponseFor(
            baseServerUrl
                + "/v1/fhir/Patient/?identifier="
                + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
                + "|"
                + beneficiary.getHicn());

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn hash and then returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifierWithoutReferenceYear() {
    Beneficiary beneficiary = getSampleABeneWithoutRefYear();

    String response =
        getJsonResponseFor(
            baseServerUrl
                + "/v1/fhir/Patient/?identifier="
                + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
                + "|"
                + beneficiary.getHicn());

    return sortPatientIdentifiers(response);
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn hash, {@link PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi",
   * {@link PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} set to true, and then
   * returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifierWithIncludeIdentifiers() {
    Beneficiary beneficiary = getSampleABene();

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, "hicn,mbi"),
            new Header(PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, "true"));

    String response =
        getJsonResponseFor(
            baseServerUrl
                + "/v1/fhir/Patient/?identifier="
                + TransformerConstants.CODING_BBAPI_BENE_HICN_HASH
                + "|"
                + beneficiary.getHicn(),
            headers);

    return sortPatientIdentifiers(response);
  }

  /**
   * Sorts the patient identifiers for testing consistency.
   *
   * @param unsortedResponse the JSON to fix up
   * @return the same JSON, but with the contents of <code>Patient.identifiers</code> sorted
   */
  private static String sortPatientIdentifiers(String unsortedResponse) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    JsonNode parsedJson;
    try {
      parsedJson = mapper.readTree(unsortedResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }

    // Is this a Bundle or a single Patient? If a Bundle, grab the (single) Patient
    // from it.
    JsonNode rootResourceType = parsedJson.at("/resourceType");
    JsonNode patient;
    if (rootResourceType.asText().equals("Patient")) {
      patient = parsedJson;
    } else if (rootResourceType.asText().equals("Bundle")) {
      JsonNode entries = parsedJson.at("/entry");
      assertEquals(1, entries.size());
      patient = entries.at("/0/resource");
      assertEquals("Patient", patient.get("resourceType").asText());
    } else {
      throw new IllegalArgumentException("Unsupported resourceType: " + rootResourceType.asText());
    }

    // Grab the Patient.identifiers node.
    JsonNode identifiers = patient.at("/identifier");

    // Pull out an unsorted List all of the identifier entries.
    List<JsonNode> identiferEntries = new ArrayList<>();
    identifiers.elements().forEachRemaining(identiferEntries::add);

    /*
     * Sort that List of identifier entries in a stable fashion: first, compare
     * identifier.system, then identifier.value, then (if present) identifier.extension[0].valueCoding.code.
     */
    Comparator<JsonNode> systemComparator = Comparator.comparing(e -> e.at("/system").asText());
    Comparator<JsonNode> valueComparator = Comparator.comparing(e -> e.at("/value").asText());
    Comparator<JsonNode> codeComparator =
        Comparator.comparing(e -> e.at("/extension/0/valueCoding/code").asText());
    Comparator<JsonNode> identifiersComparator =
        systemComparator.thenComparing(valueComparator).thenComparing(codeComparator);
    identiferEntries =
        identiferEntries.stream().sorted(identifiersComparator).collect(Collectors.toList());

    ((ArrayNode) identifiers).removeAll();
    for (JsonNode identiferEntry : identiferEntries) {
      ((ArrayNode) identifiers).add(identiferEntry);
    }

    String jsonResponse;
    try {
      jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }
    return jsonResponse;
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A bene coverage id and returns
   * the sorted results.
   *
   * @return the sorted results
   */
  public static String coverageRead() {
    Beneficiary beneficiary = getSampleABene();

    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary);
    return getJsonResponseFor(baseServerUrl + "/v1/fhir/Coverage/" + coverageId.getIdPart());
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A bene coverage id (with null
   * ref year) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String coverageReadWithoutReferenceYear() {
    Beneficiary beneficiary = getSampleABeneWithoutRefYear();

    IdDt coverageId = CommonTransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary);
    return getJsonResponseFor(baseServerUrl + "/v1/fhir/Coverage/" + coverageId.getIdPart());
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A bene id and returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String coverageSearchByPatientId() {
    Beneficiary beneficiary = getSampleABene();

    return getJsonResponseFor(
        baseServerUrl + "/v1/fhir/Coverage/?beneficiary=" + beneficiary.getBeneficiaryId());
  }

  /**
   * Executes a search against the EOB endpoint using the sample A bene id and returns the sorted
   * results.
   *
   * @return the sorted results
   */
  public static String eobByPatientIdAll() {
    Beneficiary beneficiary = getSampleABene();

    String response =
        getJsonResponseFor(
            baseServerUrl
                + "/v1/fhir/ExplanationOfBenefit/?patient="
                + beneficiary.getBeneficiaryId());

    return sortDiagnosisTypes(response, "/entry/3/resource/diagnosis/7/type");
  }

  /**
   * Executes a search against the EOB endpoint using the sample A bene id and returns the sorted
   * results.
   *
   * @return the sorted results
   */
  public static String eobByPatientIdAllWithSamhsa() {
    Beneficiary beneficiary = getSampleABeneSamhsa();

    String url =
        baseServerUrl
            + "/v1/fhir/ExplanationOfBenefit/?patient="
            + beneficiary.getBeneficiaryId()
            + "&_count="
            + 2
            + "&_page="
            + (6);
    return getJsonResponseFor(url, getRequestAuth(SAMHSA_KEYSTORE));
  }

  /**
   * Executes a search against the EOB endpoint using the sample A bene id and returns the sorted
   * results with paging.
   *
   * @return the sorted results
   */
  public static String eobByPatientIdPaged() {
    Beneficiary beneficiary = getSampleABene();

    String response =
        getJsonResponseFor(
            baseServerUrl
                + "/v1/fhir/ExplanationOfBenefit/?patient="
                + beneficiary.getBeneficiaryId()
                + "&_count=8");

    return sortDiagnosisTypes(response, "/entry/3/resource/diagnosis/7/type");
  }

  /**
   * Executes a search against the EOB endpoint using a carrier claim id (based on the sample a
   * data) and returns the sorted results with tax numbers included.
   *
   * @return the sorted results
   */
  public static String eobReadCarrierWithTaxNumbers() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String carrClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.CARRIER);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.CARRIER, carrClaimId);

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(R4PatientResourceProvider.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true"));

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId, headers);
  }

  /**
   * Executes a search against the EOB endpoint using a carrier claim id (based on the sample a
   * data) and returns the sorted results with tax numbers not included and multiple lines.
   *
   * <p>This is a integration test to make sure CareTeamComponent entries and their extensions are
   * not duplicated when there are multiple carrier claim lines present
   *
   * @return the sorted results
   */
  public static String eobReadCarrierWithMultipleLines() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResourceGroup.SAMPLE_A_MULTIPLE_CARRIER_LINES.getResources()));

    String carrClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.CARRIER);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.CARRIER, carrClaimId);

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(R4PatientResourceProvider.HEADER_NAME_INCLUDE_TAX_NUMBERS, "false"));

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId, headers);
  }

  /**
   * Executes a search against the EOB endpoint using a carrier claim id (based on the sample a
   * data) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadCarrier() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String carrClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.CARRIER);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.CARRIER, carrClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using a dme claim id (based on the sample a data)
   * and returns the sorted results with tax numbers included.
   *
   * @return the sorted results
   */
  public static String eobReadDmeWithTaxNumbers() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String dmeClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.DME);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.DME, dmeClaimId);

    Headers headers =
        new Headers(
            new Header("Content-Type", "application/json+fhir"),
            new Header(R4PatientResourceProvider.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true"));

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId, headers);
  }

  /**
   * Executes a search against the EOB endpoint using a dme claim id (based on the sample a data)
   * and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadDme() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String dmeClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.DME);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.DME, dmeClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using an HHA claim id (based on the sample a data)
   * and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadHha() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String hhaClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.HHA);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.HHA, hhaClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using a hospice claim id (based on the sample a
   * data) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadHospice() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String hospClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.HOSPICE);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.HOSPICE, hospClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using an inpatient claim id (based on the sample a
   * data) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadInpatient() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String inpClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.INPATIENT);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.INPATIENT, inpClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using an outpatient claim id (based on the sample a
   * data) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadOutpatient() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String outClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.OUTPATIENT);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.OUTPATIENT, outClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using a PDE claim id (based on the sample a data)
   * and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadPde() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String pdeClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.PDE);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.PDE, pdeClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /**
   * Executes a search against the EOB endpoint using an SNF claim id (based on the sample a data)
   * and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String eobReadSnf() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    String snfClaimId = ServerTestUtils.getClaimIdFor(loadedRecords, ClaimType.SNF);
    String eobId = CommonTransformerUtils.buildEobId(ClaimType.SNF, snfClaimId);

    return getJsonResponseFor(baseServerUrl + "/v1/fhir/ExplanationOfBenefit/" + eobId);
  }

  /** {@inheritDoc} */
  @Override
  protected Path getExpectedJsonResponseDir() {
    Path approvedResponseDir =
        Paths.get("..", "src", "test", "resources", "endpoint-responses", "v1");
    if (!Files.isDirectory(approvedResponseDir)) {
      approvedResponseDir = Paths.get("src", "test", "resources", "endpoint-responses", "v1");
    }
    if (!Files.isDirectory(approvedResponseDir)) {
      throw new IllegalStateException();
    }

    return approvedResponseDir;
  }
}
