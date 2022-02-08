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
import com.google.gson.JsonArray;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.ClaimTypeV2;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
 * EndpointJsonResponseComparatorIT#generateApprovedResponseFiles()} "test case" and run it. It will
 * regenerate ALL operation recordings. It's then your responsibility to ensure that only MEANINGFUL
 * differences to those responses are included in your PR, by clearing out any incidental noise,
 * e.g. timestamps.
 */
public final class EndpointJsonResponseComparatorV2IT {

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

  private static final String IGNORED_FIELD_TEXT = "IGNORED_FIELD";

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
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called once to make sure
   * that any existing data is deleted from the tables before running the test suite.
   */
  @BeforeAll
  public static void cleanupDatabaseBeforeTestSuite() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @AfterEach
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }

  /**
   * Generates current endpoint response files, comparing them to the corresponding approved
   * responses.
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
   * Generates the "golden" files, i.e. the approved responses to compare to. Run by commenting out
   * the <code>@Ignore</code> annotation and running this method as JUnit.
   */
  @Disabled
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

  /** @return the results of the {@link IGenericClient#fetchConformance()} operation */
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

  static void sortMetaDataResources(JsonNode resources) {
    for (JsonNode resource : resources) {
      sortMetaDataSearchParamArray(resource);
    }

    List<JsonNode> resourceList = Lists.newArrayList(resources.elements());

    resourceList.sort(Comparator.comparing(node -> node.get("type").toString()));

    ((ArrayNode) resources).removeAll();
    resourceList.forEach(((ArrayNode) resources)::add);
  }

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
   * @return the results of the {@link
   *     R4PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation
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
   * @return the results of the {@link
   *     R4PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation when {@link
   *     ExtraParamsInterceptor#setIncludeIdentifiers(IncludeIdentifiersValues)} set to "hicn,mbi"
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
   * @return the results of the {@link
   *     R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)} operation
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
        .where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * @return the results of the {@link
   *     R4PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)} operation
   *     when {@link ExtraParamsInterceptor#setIncludeIdentifiers(IncludeIdentifiersValues)} set to
   *     "hicn, mbi"
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
        .where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
        .returnBundle(Bundle.class)
        .execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * @return the results of the {@link
   *     R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)} operation
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
   * @return the results of the {@link
   *     R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)} operation
   */
  public static String patientByIdentifierWithoutReferenceYear() {
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
   * @return the results of the {@link
   *     R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)} operation
   *     when {@link ExtraParamsInterceptor#setIncludeIdentifiers(IncludeIdentifiersValues)} set to
   *     "hicn,mbi"
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
   * @return the results of the {@link
   *     CoverageResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation
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
   * @return the results of the {@link
   *     CoverageResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation
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
   * @return the results of the {@link
   *     CoverageResourceProvider#searchByBeneficiary(ca.uhn.fhir.rest.param.ReferenceParam)}
   *     operation
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
   * @return the results of the {@link
   *     CoverageResourceProvider#searchByBeneficiary(ca.uhn.fhir.rest.param.ReferenceParam)}
   *     operation
   */
  public static String coverageSearchByPatientIdWithoutReferenceYear() {
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
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam,
   *     String, ca.uhn.fhir.rest.method.RequestDetails)} operation
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
   * @return the results of the paged {@link
   *     ExplanationOfBenefitResourceProvider#findByPatient(ca.uhn.fhir.rest.param.ReferenceParam,
   *     String, ca.uhn.fhir.rest.method.RequestDetails)} operation
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
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     Carrier claims, with the {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS} set to <code>true
   *     </code>
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, carrClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     Carrier claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, carrClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     DME claims, with the {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS} set to <code>true
   *     </code>
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.DME, dmeClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     DME claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.DME, dmeClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     HHA claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HHA, hhaClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     Hospice claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.HOSPICE, hosClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     Inpatient claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.INPATIENT, inpClaim.getClaimId()))
        .execute();
    return sortDiagnosisTypes(jsonInterceptor.getResponse(), "/diagnosis/7/type");
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     Outpatient claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.OUTPATIENT, outClaim.getClaimId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     PDE claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.PDE, pdeClaim.getEventId()))
        .execute();
    return jsonInterceptor.getResponse();
  }

  /**
   * @return the results of the {@link
   *     ExplanationOfBenefitResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation for
   *     SNF claims
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
        .withId(TransformerUtilsV2.buildEobId(ClaimTypeV2.SNF, snfClaim.getClaimId()))
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

  /** @return a new {@link IGenericClient} fhirClient after setting the encoding to JSON */
  private static IGenericClient createFhirClientAndSetEncoding() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();
    fhirClient.setEncoding(EncodingEnum.JSON);

    return fhirClient;
  }

  /**
   * @param fhirClient the {@link IGenericClient} to register the interceptor to.
   * @return a new {@link JsonInterceptor} after registering it with the fhirClient
   */
  private static JsonInterceptor createAndRegisterJsonInterceptor(IGenericClient fhirClient) {
    JsonInterceptor jsonInterceptor = new JsonInterceptor();
    fhirClient.registerInterceptor(jsonInterceptor);

    return jsonInterceptor;
  }

  /** @return the path to the approved endpoint response directory */
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

  /** @return the path to the target endpoint response directory */
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
   * @param directory the path to where the file should should be written
   * @param endpoint the string to identify which endpoint's response the file contents contain
   * @return a path to use as a filename
   */
  private static Path generateFileName(Path directory, String endpoint) {
    return Paths.get(directory.toString(), endpoint + ".json");
  }

  /**
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
    boolean apply(JsonNode node);
  }

  /**
   * NodeFilteringConsumer implements the {@link Consumer} interface, and is used to filter out
   * fields in a JsonNode that meet requirements as specified by a given {@link NodeFilter}.
   */
  private static class NodeFilteringConsumer implements Consumer<JsonNode> {

    private NodeFilter f;

    public NodeFilteringConsumer(NodeFilter f) {
      this.f = f;
    }

    @Override
    public void accept(JsonNode t) {
      if (f.apply(t)) {
        ObjectNode node = (ObjectNode) t;
        node.removeAll();
      }
    }
  }
}
