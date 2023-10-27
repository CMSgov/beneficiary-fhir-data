package gov.cms.bfd.server.war.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.newrelic.relocated.JsonArray;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.stu3.providers.ExtraParamsInterceptor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
 * <p>To re-generate the recorded responses, re-enable the {@link
 * EndpointJsonResponseComparatorIT#generateApprovedResponseFiles} "test case" and run it. It will
 * regenerate ALL operation recordings. It's then your responsibility to ensure that only MEANINGFUL
 * differences to those responses are included in your PR, by clearing out any incidental noise,
 * e.g. timestamps.
 */
public final class EndpointJsonResponseComparatorV2IT extends ServerRequiredTest {

  /** Test to use for an ignored field. */
  private static final String IGNORED_FIELD_TEXT = "IGNORED_FIELD";

  /** A set of ignored paths for testing. */
  private static final Set<String> IGNORED_PATHS =
      ImmutableSet.of(
          "\"/id\"",
          "\"/date\"",
          "\"/created\"",
          "\"/link/[0-9]/url\"",
          "\"/implementation/url\"",
          "\"/entry/[0-9]/fullUrl\"",
          "\"/meta\"",
          "\"/meta/lastUpdated\"",
          "\"/entry/[0-9]/resource/meta/lastUpdated\"",
          "\"/entry/[0-9]/resource/meta\"",
          "\"/entry/[0-9]/resource/created\"",
          "\"/procedure/[0-9]/date\"",
          "\"/entry/[0-9]/resource/procedure/[0-9]/date\"",
          "\"/software/version\"");

  /**
   * Returns data for parameterized tests.
   *
   * @return the data
   */
  public static Stream<Arguments> data() {
    return Stream.of(
        arguments("metadata", (Supplier<String>) EndpointJsonResponseComparatorV2IT::metadata),
        arguments(
            "patientRead", (Supplier<String>) EndpointJsonResponseComparatorV2IT::patientRead),
        arguments(
            "patientReadWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2IT::patientReadWithIncludeIdentifiers),
        arguments(
            "patientSearchById",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::patientSearchById),
        arguments(
            "patientSearchByIdWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2IT::patientSearchByIdWithIncludeIdentifiers),
        arguments(
            "patientByIdentifier",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::patientByIdentifier),
        arguments(
            "patientByIdentifierWithoutReferenceYear",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2IT::patientByIdentifierWithoutReferenceYear),
        arguments(
            "patientByIdentifierWithIncludeIdentifiers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2IT::patientByIdentifierWithIncludeIdentifiers),
        arguments(
            "coverageRead", (Supplier<String>) EndpointJsonResponseComparatorV2IT::coverageRead),
        arguments(
            "coverageSearchByPatientId",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::coverageSearchByPatientId),
        arguments(
            "coverageSearchByPatientIdWithoutReferenceYear",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2IT::coverageSearchByPatientIdWithoutReferenceYear),
        arguments(
            "eobByPatientIdAll",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobByPatientIdAll),
        arguments(
            "eobByPatientIdPaged",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobByPatientIdPaged),
        arguments(
            "eobReadCarrier",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadCarrier),
        arguments(
            "eobReadCarrierWithTaxNumbers",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadCarrierWithTaxNumbers),
        arguments(
            "eobReadCarrierWithMultipleLines",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadCarrierWithMultipleLines),
        arguments("eobReadDme", (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadDme),
        arguments(
            "eobReadDmeWithTaxNumbers",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadDmeWithTaxNumbers),
        arguments("eobReadHha", (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadHha),
        arguments(
            "eobReadHospice",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadHospice),
        arguments(
            "eobReadInpatient",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadInpatient),
        arguments(
            "eobReadOutpatient",
            (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadOutpatient),
        arguments("eobReadPde", (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadPde),
        arguments("eobReadSnf", (Supplier<String>) EndpointJsonResponseComparatorV2IT::eobReadSnf));
  }

  /**
   * Generates current endpoint response files, comparing them to the corresponding approved
   * responses.
   *
   * @param endpointId the endpoint id
   * @param endpointOperation the endpoint operation
   */
  @ParameterizedTest(name = "endpointId = {0}")
  @MethodSource("data")
  public void verifyCorrectEndpointResponse(String endpointId, Supplier<String> endpointOperation) {
    Path targetResponseDir = getTargetResponseDir();

    // Call the server endpoint and save its result out to a file corresponding to
    // the endpoint Id.
    String endpointResponse = endpointOperation.get();
    writeFile(endpointResponse, generateFileName(targetResponseDir, endpointId));

    assertJsonDiffIsEmpty(endpointId);
  }

  /**
   * Generates the "golden" files, i.e. the approved responses to compare to. Purpose of this
   * testing is to perform regression testing against the "Golden Beneficiary Data" at a specific
   * point in time. It is important to note that this testing focuses on checking for regressions
   * against the data at that particular moment, and not necessarily against data artifacts.
   *
   * @param endpointId the endpoint id
   * @param endpointOperation the endpoint operation
   */
  @EnabledIfSystemProperty(
      // disabled on all but 64bit OS
      named = "os.arch",
      matches = ".*64.*")
  @ParameterizedTest(name = "endpointId = {0}")
  @MethodSource("data")
  public void generateApprovedResponseFiles(String endpointId, Supplier<String> endpointOperation) {
    Path approvedResponseDir = getApprovedResponseDir();

    // Call the server endpoint and save its result out to a file corresponding to
    // the endpoint Id.
    String endpointResponse = endpointOperation.get();

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode = null;
    try {
      jsonNode = mapper.readTree(endpointResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + endpointResponse, e);
    }

    replaceIgnoredFieldsWithFillerText(
        jsonNode,
        "id",
        Optional.of(
            Pattern.compile(
                "[A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{12}")));
    replaceIgnoredFieldsWithFillerText(
        jsonNode, "url", Optional.of(Pattern.compile("(https://localhost:)([0-9]{4})(.*)")));
    replaceIgnoredFieldsWithFillerText(jsonNode, "lastUpdated", Optional.empty());
    replaceIgnoredFieldsWithFillerText(jsonNode, "created", Optional.empty());

    if (endpointId.equals("metadata"))
      replaceIgnoredFieldsWithFillerText(jsonNode, "date", Optional.empty());

    String jsonResponse = null;
    try {
      jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + endpointResponse, e);
    }
    writeFile(jsonResponse, generateFileName(approvedResponseDir, endpointId));
  }

  /**
   * Replaces ignored fields with filler text.
   *
   * @param parent the {@link JsonNode} on which to perform the replacement
   * @param fieldName the {@link String} name of the field that is being replaced
   * @param pattern an optional {@link Pattern} pattern to correctly identify fields needing to be
   *     replaced
   */
  private static void replaceIgnoredFieldsWithFillerText(
      JsonNode parent, String fieldName, Optional<Pattern> pattern) {
    if (parent.has(fieldName)) {
      if (pattern.isPresent()) {
        Pattern p = pattern.get();
        Matcher m = p.matcher(parent.get(fieldName).toString());
        if (m.find())
          if (fieldName.equals("url")) {
            // Only replace the port numbers (m.group(2)) on urls
            String replacementUrl = m.group(1) + IGNORED_FIELD_TEXT + m.group(3);
            ((ObjectNode) parent)
                .put(fieldName, replacementUrl.substring(0, replacementUrl.length() - 1));
          } else ((ObjectNode) parent).put(fieldName, IGNORED_FIELD_TEXT);
      } else ((ObjectNode) parent).put(fieldName, IGNORED_FIELD_TEXT);
    }

    // Now, recursively invoke this method on all properties
    for (JsonNode child : parent) {
      replaceIgnoredFieldsWithFillerText(child, fieldName, pattern);
    }
  }

  /**
   * Gets the results of the {@link IGenericClient#fetchConformance()} operation.
   *
   * @return the metadata
   */
  public static String metadata() {
    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient.capabilities().ofType(CapabilityStatement.class).execute();
    return sortMetadataResponse(jsonInterceptor.getResponse());
  }

  /**
   * FIXME: Additional workaround due to HAPI not always returning array elements in the same order
   * for a specific searchParam {@link JsonArray} in the capability statement. This method is only
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
  private static String sortMetadataResponse(String unsortedResponse) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    JsonNode parsedJson = null;
    try {
      parsedJson = mapper.readTree(unsortedResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + unsortedResponse, e);
    }

    sortMetaDataResources(parsedJson.at("/rest/0/resource"));

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
   * Sorts the metadata resources.
   *
   * @param resources the resources
   */
  static void sortMetaDataResources(JsonNode resources) {
    for (JsonNode resource : resources) {
      sortMetaDataSearchParamArray(resource);
    }

    List<JsonNode> resourceList = Lists.newArrayList(resources.elements());

    resourceList.sort(Comparator.comparing(node -> node.get("type").toString()));

    ((ArrayNode) resources).removeAll();
    resourceList.forEach(((ArrayNode) resources)::add);
  }

  /**
   * Sort the metadata search param array.
   *
   * @param resource the resource
   */
  static void sortMetaDataSearchParamArray(JsonNode resource) {
    if (resource.has("searchParam")) {
      JsonNode searchParamsArray = resource.at("/searchParam");

      List<JsonNode> searchParams = Lists.newArrayList(searchParamsArray.elements());

      searchParams.sort(Comparator.comparing(node -> node.get("name").toString()));

      ((ArrayNode) searchParamsArray).removeAll();
      searchParams.forEach(((ArrayNode) searchParamsArray)::add);
    }
  }

  /**
   * FIXME: Additional workaround due to HAPI not always returning array elements in the same order
   * for a specific searchParam {@link JsonArray} in the capability statement. This method is only
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
    JsonNode parsedJson = null;
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

    Collections.sort(
        diagnosisTypes,
        new Comparator<JsonNode>() {
          /** {@inheritDoc} */
          @Override
          public int compare(JsonNode node1, JsonNode node2) {
            String name1 = node1.get("coding").get(0).get("code").toString();
            String name2 = node2.get("coding").get(0).get("code").toString();
            return name1.compareTo(name2);
          }
        });

    ((ArrayNode) diagnosisTypeArray).removeAll();
    for (int i = 0; i < diagnosisTypes.size(); i++) {
      ((ArrayNode) diagnosisTypeArray).add(diagnosisTypes.get(i));
    }

    String jsonResponse = null;
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
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint using the sample A bene id and {@link
   * R4PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi", then returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String patientReadWithIncludeIdentifiers() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint (search by logical id) using the sample A bene
   * id and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientSearchById() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            Patient.RES_ID
                .exactly()
                .systemAndIdentifier(null, String.valueOf(beneficiary.getBeneficiaryId())))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint (search by logical id) using the sample A bene
   * id, {@link R4PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi", {@link
   * R4PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} set to true, and then returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String patientSearchByIdWithIncludeIdentifiers() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            Patient.RES_ID
                .exactly()
                .systemAndIdentifier(null, String.valueOf(beneficiary.getBeneficiaryId())))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn and then returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifier() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            Patient.IDENTIFIER
                .exactly()
                .systemAndIdentifier(
                    TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn hash and then returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifierWithoutReferenceYear() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResourceGroup.SAMPLE_A_WITHOUT_REFERENCE_YEAR.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            Patient.IDENTIFIER
                .exactly()
                .systemAndIdentifier(
                    TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * Executes a search against the Patient endpoint (search by identifier) using the sample A bene
   * hicn hash, {@link R4PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} set to "hicn,mbi",
   * {@link R4PatientResourceProvider#HEADER_NAME_INCLUDE_ADDRESS_FIELDS} set to true, and then
   * returns the sorted results.
   *
   * @return the sorted results
   */
  public static String patientByIdentifierWithIncludeIdentifiers() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(
        RequestHeaders.getHeaderWrapper(
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            R4PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Patient.class)
        .where(
            Patient.IDENTIFIER
                .exactly()
                .systemAndIdentifier(
                    TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
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
    JsonNode parsedJson = null;
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
    List<JsonNode> identiferEntries = new ArrayList<JsonNode>();
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
    for (int i = 0; i < identiferEntries.size(); i++) {
      ((ArrayNode) identifiers).add(identiferEntries.get(i));
    }

    String jsonResponse = null;
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
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .read()
        .resource(Coverage.class)
        .withId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_A, beneficiary))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A bene coverage id (with null
   * ref year) and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String coverageReadWithNullReferenceYear() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    beneficiary.setBeneEnrollmentReferenceYear(Optional.empty());
    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .read()
        .resource(Coverage.class)
        .withId(TransformerUtilsV2.buildCoverageId(MedicareSegment.PART_A, beneficiary))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A bene id and returns the
   * sorted results.
   *
   * @return the sorted results
   */
  public static String coverageSearchByPatientId() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Coverage.class)
        .where(
            Coverage.BENEFICIARY.hasId(
                TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
        .returnBundle(Bundle.class)
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * Executes a search against the Coverage endpoint using the sample A (with no ref year) bene id
   * and returns the sorted results.
   *
   * @return the sorted results
   */
  public static String coverageSearchByPatientIdWithoutReferenceYear() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResourceGroup.SAMPLE_A_WITHOUT_REFERENCE_YEAR.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(Coverage.class)
        .where(
            Coverage.BENEFICIARY.hasId(
                TransformerUtilsV2.buildPatientId(beneficiary.getBeneficiaryId())))
        .returnBundle(Bundle.class)
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * Executes a search against the EOB endpoint using the sample A bene id and returns the sorted
   * results.
   *
   * @return the sorted results
   */
  public static String eobByPatientIdAll() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(ExplanationOfBenefit.class)
        .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
        .returnBundle(Bundle.class)
        .execute();
    return sortDiagnosisTypes(jsonInterceptor.getResponse(), "/entry/3/resource/diagnosis/7/type");
  }

  /**
   * Executes a search against the EOB endpoint using the sample A bene id and returns the sorted
   * results with paging.
   *
   * @return the sorted results
   */
  public static String eobByPatientIdPaged() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient
        .search()
        .forResource(ExplanationOfBenefit.class)
        .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtilsV2.buildPatientId(beneficiary)))
        .count(8)
        .returnBundle(Bundle.class)
        .execute();
    return sortDiagnosisTypes(jsonInterceptor.getResponse(), "/entry/3/resource/diagnosis/7/type");
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");

    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(requestHeader);
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    CarrierClaim carrClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "false");

    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(requestHeader);
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    CarrierClaim carrClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    CarrierClaim carrClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    RequestHeaders requestHeader =
        RequestHeaders.getHeaderWrapper(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true");

    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setHeaders(requestHeader);
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.DME, dmeClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    DMEClaim dmeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(r -> (DMEClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.DME, dmeClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    HHAClaim hhaClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(r -> (HHAClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.HHA, hhaClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    HospiceClaim hosClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(r -> (HospiceClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.HOSPICE, hosClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    InpatientClaim inpClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.INPATIENT, inpClaim.getClaimId()))
        .execute();
    return sortDiagnosisTypes(jsonInterceptor.getResponse(), "/diagnosis/7/type");
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    OutpatientClaim outClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(r -> (OutpatientClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.OUTPATIENT, outClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    PartDEvent pdeClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(r -> (PartDEvent) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.PDE, pdeClaim.getEventId()))
        .execute();
    return jsonInterceptor.getResponse();
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

    IGenericClient fhirClient = createFhirClientAndSetEncoding();
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    SNFClaim snfClaim =
        loadedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(r -> (SNFClaim) r)
            .findFirst()
            .get();
    fhirClient
        .read()
        .resource(ExplanationOfBenefit.class)
        .withId(TransformerUtilsV2.buildEobId(ClaimType.SNF, snfClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * Compares the approved and current responses for an endpoint.
   *
   * @param endpointId the name of the operation being tested, used to determine which files to
   *     compare
   */
  private static void assertJsonDiffIsEmpty(String endpointId) {
    Path approvedResponseDir = getApprovedResponseDir();
    Path targetResponseDir = getTargetResponseDir();

    String approvedJson = readFile(generateFileName(approvedResponseDir, endpointId));
    String newJson = readFile(generateFileName(targetResponseDir, endpointId));
    AssertUtils.assertJsonEquals(approvedJson, newJson, IGNORED_PATHS);
  }

  /**
   * Creates a fhir client and sets JSON encoding.
   *
   * @return a new {@link IGenericClient} fhirClient after setting the encoding to JSON
   */
  private static IGenericClient createFhirClientAndSetEncoding() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
    fhirClient.setEncoding(EncodingEnum.JSON);

    return fhirClient;
  }

  /**
   * Create and register json interceptor.
   *
   * @param fhirClient the {@link IGenericClient} to register the interceptor to.
   * @return a new {@link JsonInterceptor} after registering it with the fhirClient
   */
  private static JsonInterceptor createAndRegisterJsonInterceptor(IGenericClient fhirClient) {
    JsonInterceptor jsonInterceptor = new JsonInterceptor();
    fhirClient.registerInterceptor(jsonInterceptor);

    return jsonInterceptor;
  }

  /**
   * Gets the approved response directory.
   *
   * @return the path to the approved endpoint response directory
   */
  private static Path getApprovedResponseDir() {
    Path approvedResponseDir =
        Paths.get("..", "src", "test", "resources", "endpoint-responses", "v2");
    if (!Files.isDirectory(approvedResponseDir))
      approvedResponseDir = Paths.get("src", "test", "resources", "endpoint-responses", "v2");
    if (!Files.isDirectory(approvedResponseDir)) {
      throw new IllegalStateException();
    }

    return approvedResponseDir;
  }

  /**
   * Gets the target response directory.
   *
   * @return the path to the target endpoint response directory
   */
  private static Path getTargetResponseDir() {
    Path targetDir = Paths.get("..", "target");
    if (!Files.isDirectory(targetDir)) {
      targetDir = Paths.get("target");
    }
    if (!Files.isDirectory(targetDir)) {
      throw new IllegalStateException();
    }

    new File(Paths.get(targetDir.toString(), "endpoint-responses", "v2").toString()).mkdirs();
    Path targetResponseDir = Paths.get(targetDir.toString(), "endpoint-responses", "v2");

    return targetResponseDir;
  }

  /**
   * Generates a path to use as a filename.
   *
   * @param directory the path to where the file should be written
   * @param endpoint the string to identify which endpoint's response the file contents contain
   * @return a path to use as a filename
   */
  private static Path generateFileName(Path directory, String endpoint) {
    return Paths.get(directory.toString(), endpoint + ".json");
  }

  /**
   * Writes a file to the specified path.
   *
   * @param contents the string to be written to a file
   * @param fileName the path to name the file
   */
  private static void writeFile(String contents, Path fileName) {
    File jsonFile = new File(fileName.toString());
    try (OutputStreamWriter streamWriter =
        new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8); ) {
      streamWriter.write(contents);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write file at " + fileName.toString(), e);
    }
  }

  /**
   * Reads a file at the specified location.
   *
   * @param path the path to the file
   * @return the contents of the file as a string.
   */
  private static String readFile(Path path) {
    byte[] encoded = null;
    try {
      encoded = Files.readAllBytes(path);
    } catch (IOException e) {
      throw new UncheckedIOException("Can't read file at " + path.toString(), e);
    }
    return new String(encoded, StandardCharsets.UTF_8);
  }

  /**
   * NodeFilter is a simple interface with one method that takes a single argument, {@link
   * JsonNode}, and returns true if the JsonNode satisfies the filter.
   */
  private static interface NodeFilter {
    /**
     * Applies the filter to the node.
     *
     * @param node the node
     * @return {@code true} if the node satisfies the filter
     */
    boolean apply(JsonNode node);
  }

  /**
   * NodeFilteringConsumer implements the {@link Consumer} interface, and is used to filter out
   * fields in a JsonNode that meet requirements as specified by a given {@link NodeFilter}.
   */
  private static class NodeFilteringConsumer implements Consumer<JsonNode> {

    /** The filter. */
    private NodeFilter f;

    /**
     * Instantiates a new Node filtering consumer.
     *
     * @param f the filter
     */
    public NodeFilteringConsumer(NodeFilter f) {
      this.f = f;
    }

    /** {@inheritDoc} */
    @Override
    public void accept(JsonNode t) {
      if (f.apply(t)) {
        ObjectNode node = (ObjectNode) t;
        node.removeAll();
      }
    }
  }
}
