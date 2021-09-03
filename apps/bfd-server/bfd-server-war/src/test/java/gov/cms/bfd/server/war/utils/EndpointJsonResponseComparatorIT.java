package gov.cms.bfd.server.war.utils;

import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
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
import gov.cms.bfd.server.war.stu3.providers.ClaimType;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExtraParamsInterceptor;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.TransformerUtils;
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
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Integration tests for comparing changes in the JSON from our endpoint responses. This test code
 * relies on the assumption that SAMPLE_A will have at least one bene and that every bene in it will
 * have >= 1 EOB of every type.
 */
@RunWith(Parameterized.class)
public final class EndpointJsonResponseComparatorIT {

  @Parameters(name = "endpointId = {0}")
  public static Object[][] data() {
    return new Object[][] {
      {"metadata", (Supplier<String>) EndpointJsonResponseComparatorIT::metadata},
      {"patientRead", (Supplier<String>) EndpointJsonResponseComparatorIT::patientRead},
      {
        "patientReadWithIncludeIdentifiers",
        (Supplier<String>) EndpointJsonResponseComparatorIT::patientReadWithIncludeIdentifiers
      },
      {"patientSearchById", (Supplier<String>) EndpointJsonResponseComparatorIT::patientSearchById},
      {
        "patientSearchByIdWithIncludeIdentifiers",
        (Supplier<String>) EndpointJsonResponseComparatorIT::patientSearchByIdWithIncludeIdentifiers
      },
      {
        "patientByIdentifier",
        (Supplier<String>) EndpointJsonResponseComparatorIT::patientByIdentifier
      },
      {
        "patientByIdentifierWithIncludeIdentifiers",
        (Supplier<String>)
            EndpointJsonResponseComparatorIT::patientByIdentifierWithIncludeIdentifiers
      },
      {"coverageRead", (Supplier<String>) EndpointJsonResponseComparatorIT::coverageRead},
      {
        "coverageSearchByPatientId",
        (Supplier<String>) EndpointJsonResponseComparatorIT::coverageSearchByPatientId
      },
      {"eobByPatientIdAll", (Supplier<String>) EndpointJsonResponseComparatorIT::eobByPatientIdAll},
      {
        "eobByPatientIdPaged",
        (Supplier<String>) EndpointJsonResponseComparatorIT::eobByPatientIdPaged
      },
      {"eobReadCarrier", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadCarrier},
      {
        "eobReadCarrierWithTaxNumbers",
        (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadCarrierWithTaxNumbers
      },
      {"eobReadDme", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadDme},
      {
        "eobReadDmeWithTaxNumbers",
        (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadDmeWithTaxNumbers
      },
      {"eobReadHha", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadHha},
      {"eobReadHospice", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadHospice},
      {"eobReadInpatient", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadInpatient},
      {"eobReadOutpatient", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadOutpatient},
      {"eobReadPde", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadPde},
      {"eobReadSnf", (Supplier<String>) EndpointJsonResponseComparatorIT::eobReadSnf}
    };
  }

  private final String endpointId;
  private final Supplier<String> endpointOperation;
  private static final String IGNORED_FIELD_TEXT = "IGNORED_FIELD";

  /**
   * Parameterized test constructor: JUnit will construct a new instance of this class for every
   * top-level element returned by the {@link #data()} {@link Parameters} test data generator, and
   * then run the test cases in this class using that specific test data element.
   *
   * @param endpointId the name of the operation being tested, which is also used to locate the
   *     "approved" operation response file in the src/test/resources/endpoint-responses source
   *     directory
   * @param endpointOperation the operation to be tested
   */
  public EndpointJsonResponseComparatorIT(String endpointId, Supplier<String> endpointOperation) {
    this.endpointId = endpointId;
    this.endpointOperation = endpointOperation;
  }

  /**
   * Generates current endpoint response files, comparing them to the corresponding approved
   * responses.
   */
  @Test
  public void verifyCorrectEndpointResponse() {
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
  @Ignore
  @Test
  public void generateApprovedResponseFiles() {
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

    if (endpointId == "metadata")
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
          if (fieldName == "url") {
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
    return sortMetadataSearchParamArray(jsonInterceptor.getResponse());
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
  private static String sortMetadataSearchParamArray(String unsortedResponse) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter();
    JsonNode parsedJson = null;
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

    Collections.sort(
        searchParams,
        new Comparator<JsonNode>() {
          @Override
          public int compare(JsonNode node1, JsonNode node2) {
            String name1 = node1.get("name").toString();
            String name2 = node2.get("name").toString();
            return name1.compareTo(name2);
          }
        });

    ((ArrayNode) searchParamsArray).removeAll();
    for (int i = 0; i < searchParams.size(); i++) {
      ((ArrayNode) searchParamsArray).add(searchParams.get(i));
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
   *     PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation
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
   *     PatientResourceProvider#read(org.hl7.fhir.dstu3.model.IdType)} operation when {@link
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
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
            "true"));
    fhirClient.registerInterceptor(extraParamsInterceptor);
    JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

    fhirClient.read().resource(Patient.class).withId(beneficiary.getBeneficiaryId()).execute();
    return sortPatientIdentifiers(jsonInterceptor.getResponse());
  }

  /**
   * @return the results of the {@link
   *     PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)} operation
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
   *     PatientResourceProvider#searchByLogicalId(ca.uhn.fhir.rest.param.TokenParam)} operation
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
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
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
   *     PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)} operation
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
   *     PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)} operation
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
            PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS,
            "hicn,mbi",
            PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS,
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
      Assert.assertEquals(1, entries.size());
      patient = entries.at("/0/resource");
      Assert.assertEquals("Patient", patient.get("resourceType").asText());
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
        .withId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary))
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
                TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
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
        .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
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
        .where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
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
        .withId(TransformerUtils.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.DME, dmeClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.DME, dmeClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.HHA, hhaClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.HOSPICE, hosClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.INPATIENT, inpClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.OUTPATIENT, outClaim.getClaimId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.PDE, pdeClaim.getEventId()))
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
        .withId(TransformerUtils.buildEobId(ClaimType.SNF, snfClaim.getClaimId()))
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

    ObjectMapper mapper = new ObjectMapper();
    JsonNode beforeNode = null;
    try {
      beforeNode = mapper.readTree(approvedJson);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + approvedJson, e);
    }
    JsonNode afterNode = null;
    try {
      afterNode = mapper.readTree(newJson);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + newJson, e);
    }
    JsonNode diff = JsonDiff.asJson(beforeNode, afterNode);

    // Filter out diffs that we don't care about (due to changing with each call)
    // such as "lastUpdated" fields, the port on URLs, etc. ...
    NodeFilteringConsumer consumer =
        new NodeFilteringConsumer(
            new NodeFilter() {
              @Override
              public boolean apply(JsonNode node) {
                Pattern p = getIgnoredPathsRegex();
                Matcher m = p.matcher(node.get("path").toString());
                return m.matches();
              }
            });

    diff.forEach(consumer);
    if (diff.size() > 0) {
      for (int i = 0; i < diff.size(); i++) {
        Assert.assertEquals("{}", diff.get(i).toString());
      }
    }
  }

  /** @return a regex pattern for ignored JSON paths */
  private static Pattern getIgnoredPathsRegex() {
    StringBuilder pattern = new StringBuilder();
    pattern.append("\"/id\"");
    pattern.append("|\"/date\"");
    pattern.append("|\"/link/[0-9]/url\"");
    pattern.append("|\"/implementation/url\"");
    pattern.append("|\"/entry/[0-9]/fullUrl\"");
    pattern.append("|\"/meta\"");
    pattern.append("|\"/meta/lastUpdated\"");
    pattern.append("|\"/entry/[0-9]/resource/meta/lastUpdated\"");
    pattern.append("|\"/entry/[0-9]/resource/meta\"");
    pattern.append("|\"/procedure/[0-9]/date\"");
    pattern.append("|\"/entry/[0-9]/resource/procedure/[0-9]/date\"");
    pattern.append("|\"/software/version\"");

    return Pattern.compile(pattern.toString());
  }

  /** @return a new {@link IGenericClient} fhirClient after setting the encoding to JSON */
  private static IGenericClient createFhirClientAndSetEncoding() {
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClient();
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
        Paths.get("..", "src", "test", "resources", "endpoint-responses", "v1");

    if (!Files.isDirectory(approvedResponseDir)) {
      approvedResponseDir = Paths.get("src", "test", "resources", "endpoint-responses", "v1");
    }

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

    new File(Paths.get(targetDir.toString(), "endpoint-responses", "v1").toString()).mkdirs();
    Path targetResponseDir = Paths.get(targetDir.toString(), "endpoint-responses", "v1");

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

  /**
   * Ensures that {@link PipelineTestUtils#truncateTablesInDataSource()} is called after each test
   * case.
   */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    PipelineTestUtils.get().truncateTablesInDataSource();
  }
}
