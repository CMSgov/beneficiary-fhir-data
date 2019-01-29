package gov.hhs.cms.bluebutton.server.app.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flipkart.zjsonpatch.JsonDiff;
import com.google.gson.JsonArray;

import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.EncodingEnum;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.samples.StaticRifResourceGroup;
import gov.hhs.cms.bluebutton.server.app.ServerTestUtils;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.ClaimType;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.MedicareSegment;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerConstants;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerUtils;

/**
 * Integration tests for comparing changes in the JSON from our endpoint
 * responses. This test code relies on the assumption that SAMPLE_A will have at
 * least one bene and that every bene in it will have >= 1 EOB of every type.
 */
public final class EndpointJsonResponseComparatorIT {

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the capability statement.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void metadata() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.fetchConformance().ofType(CapabilityStatement.class).execute();

		// writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir,
		// "metadata"));
		writeFile(sortMetadataSearchParamArray(jsonInterceptor.getResponse()),
				generateFileName(targetResponseDir, "metadata"));

		assertJsonDiffIsEmpty("metadata");
	}

	/**
	 * FIXME: Additional workaround due to HAPI not always returning array elements
	 * in the same order for a specific searchParam {@link JsonArray} in the
	 * capability statement. This method is only necessary until the following issue
	 * has been resolved with HAPI:
	 * https://github.com/jamesagnew/hapi-fhir/issues/1183
	 * 
	 * Before: 
	 *   {
     *     "type": "Patient",
     *     "profile": {
     *       "reference": "http://hl7.org/fhir/Profile/Patient"
     *     },
     *     "interaction": [
     *       {
     *         "code": "read"
     *       },
     *       {
     *         "code": "search-type"
     *       }
     *     ],
	 * 	   "searchParam": [
     *       {
     *         "name": "identifier",
     *         "type": "token",
     *         "documentation": "A patient identifier"
     *       },
     *       {
     *         "name": "_id",
     *         "type": "token",
     *         "documentation": "The ID of the resource"
     *       }
     *     ]
     *   }  
	 * 
	 * After:
	 *   {
     *     "type": "Patient",
     *     "profile": {
     *       "reference": "http://hl7.org/fhir/Profile/Patient"
     *     },
     *     "interaction": [
     *       {
     *         "code": "read"
     *       },
     *       {
     *         "code": "search-type"
     *       }
     *     ],
     *     "searchParam": [
     *       {
     *         "name": "_id",
     *         "type": "token",
     *         "documentation": "The ID of the resource"
     *       },
     *       {
     *         "name": "identifier",
     *         "type": "token",
     *         "documentation": "A patient identifier"
     *       }
     *     ]
     *   }
	 * 
	 * @param unsortedResponse
	 *            The JSON string with an unsorted searchParam array
	 * @return The JSON string with the sorted searchParam array
	 * @throws IOException
	 */
	private String sortMetadataSearchParamArray(String unsortedResponse) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writerWithDefaultPrettyPrinter();
		JsonNode parsedJson = mapper.readTree(unsortedResponse);
		
		// This returns the searchParam node for the resource type='Patient' from
		// metadata.json
		JsonNode searchParamsArray = parsedJson.at("/rest/0/resource/3/searchParam");

		Iterator<JsonNode> searchParamsArrayIterator = searchParamsArray.elements();
		List<JsonNode> searchParams = new ArrayList<JsonNode>();
		while (searchParamsArrayIterator.hasNext()) {
			searchParams.add(searchParamsArrayIterator.next());
		}
		
		Collections.sort(searchParams, new Comparator<JsonNode>() {
			public int compare(JsonNode node1, JsonNode node2) {
				String name1 = node1.get("name").toString();
				String name2 = node2.get("name").toString();
				return name1.compareTo(name2);
			}
		});

		((ArrayNode) searchParamsArray).removeAll();
		for (int i = 0; i < searchParams.size(); i++) {
			((ArrayNode) searchParamsArray).add((ObjectNode) searchParams.get(i));
		}

		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsedJson);
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the patient read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void patientRead() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.read(Patient.class, beneficiary.getBeneficiaryId());
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "patientRead"));

		assertJsonDiffIsEmpty("patientRead");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the patient search endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void patientSearchById() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiary.getBeneficiaryId()))
				.returnBundle(Bundle.class).execute();
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "patientSearchById"));

		assertJsonDiffIsEmpty("patientSearchById");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the patient search by hicn endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void patientByIdentifier() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
				.returnBundle(Bundle.class).execute();
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "patientByIdentifier"));

		assertJsonDiffIsEmpty("patientByIdentifier");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the coverage read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void coverageRead() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.read(Coverage.class,
				TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary.getBeneficiaryId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "coverageRead"));

		assertJsonDiffIsEmpty("coverageRead");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the coverage search endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void coverageSearchByPatientId() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.search().forResource(Coverage.class)
				.where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary.getBeneficiaryId())))
				.returnBundle(Bundle.class).execute();
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "coverageSearchByPatientId"));

		assertJsonDiffIsEmpty("coverageSearchByPatientId");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the patient search by id endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobByPatientIdAll() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
				.returnBundle(Bundle.class).execute();
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobByPatientIdAll"));

		assertJsonDiffIsEmpty("eobByPatientIdAll");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the patient search by id with paging endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobByPatientIdPaged() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		fhirClient.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary))).count(8)
				.returnBundle(Bundle.class).execute();
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobByPatientIdPaged"));

		assertJsonDiffIsEmpty("eobByPatientIdPaged");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the carrier read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadCarrier() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		CarrierClaim carrClaim = loadedRecords.stream().filter(r -> r instanceof CarrierClaim)
				.map(r -> (CarrierClaim) r).findFirst().get();
		fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadCarrier"));

		assertJsonDiffIsEmpty("eobReadCarrier");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the dme read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadDme() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		DMEClaim dmeClaim = loadedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> (DMEClaim) r).findFirst()
				.get();
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.DME, dmeClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadDme"));

		assertJsonDiffIsEmpty("eobReadDme");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the hha read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadHha() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		HHAClaim hhaClaim = loadedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> (HHAClaim) r).findFirst()
				.get();
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.HHA, hhaClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadHha"));

		assertJsonDiffIsEmpty("eobReadHha");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the hospice read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadHospice() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		HospiceClaim hosClaim = loadedRecords.stream().filter(r -> r instanceof HospiceClaim).map(r -> (HospiceClaim) r)
				.findFirst().get();
		fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.HOSPICE, hosClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadHospice"));

		assertJsonDiffIsEmpty("eobReadHospice");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the inpatient read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadInpatient() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		InpatientClaim inpClaim = loadedRecords.stream().filter(r -> r instanceof InpatientClaim)
				.map(r -> (InpatientClaim) r).findFirst().get();
		fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.INPATIENT, inpClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadInpatient"));

		assertJsonDiffIsEmpty("eobReadInpatient");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the outpatient read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadOutpatient() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		OutpatientClaim outClaim = loadedRecords.stream().filter(r -> r instanceof OutpatientClaim)
				.map(r -> (OutpatientClaim) r).findFirst().get();
		fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.OUTPATIENT, outClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadOutpatient"));

		assertJsonDiffIsEmpty("eobReadOutpatient");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the pde read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadPde() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		PartDEvent pdeClaim = loadedRecords.stream().filter(r -> r instanceof PartDEvent).map(r -> (PartDEvent) r)
				.findFirst().get();
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.PDE, pdeClaim.getEventId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadPde"));

		assertJsonDiffIsEmpty("eobReadPde");
	}

	/**
	 * Verifies that there is no change between the current and approved responses
	 * for the snf read endpoint.
	 * 
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	@Test
	public void eobReadSnf() throws IOException {
		Path targetResponseDir = getTargetResponseDir();

		List<Object> loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

		IGenericClient fhirClient = createFhirClientAndSetEncoding();
		JsonInterceptor jsonInterceptor = createAndRegisterJsonInterceptor(fhirClient);

		SNFClaim snfClaim = loadedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> (SNFClaim) r).findFirst()
				.get();
		fhirClient.read(ExplanationOfBenefit.class, TransformerUtils.buildEobId(ClaimType.SNF, snfClaim.getClaimId()));
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "eobReadSnf"));

		assertJsonDiffIsEmpty("eobReadSnf");
	}

	/**
	 * @param fileName
	 *            A string to determine which files to compare. Compares the
	 *            approved and current responses for an endpoint.
	 * @throws IOException
	 *             (indicates a JSON string could not be deserialized properly)
	 */
	private static void assertJsonDiffIsEmpty(String fileName) throws IOException {
		Path approvedResponseDir = Paths.get(".", "src", "test", "resources", "endpoint-responses");
		if (!Files.isDirectory(approvedResponseDir))
			throw new IllegalStateException();
		Path targetResponseDir = getTargetResponseDir();

		String approvedJson = readFile(generateFileName(approvedResponseDir, fileName));
		String newJson = readFile(generateFileName(targetResponseDir, fileName));

		ObjectMapper mapper = new ObjectMapper();
		JsonNode beforeNode = mapper.readTree(approvedJson);
		JsonNode afterNode = mapper.readTree(newJson);
		JsonNode diff = JsonDiff.asJson(beforeNode, afterNode);

		// Filter out diffs that we don't care about (due to changing with each call)
		// such as "lastUpdated" fields, the port on URLs, etc. ...
		NodeFilteringConsumer consumer = new NodeFilteringConsumer(new NodeFilter() {
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

	/**
	 * @return a regex pattern for ignored JSON paths.
	 */
	private static Pattern getIgnoredPathsRegex() {
		StringBuilder pattern = new StringBuilder();
		pattern.append("\"/id\"");
		pattern.append("|\"/date\"");
		pattern.append("|\"/link/[0-9]/url\"");
		pattern.append("|\"/implementation/url\"");
		pattern.append("|\"/entry/[0-9]/fullUrl\"");
		pattern.append("|\"/meta/lastUpdated\"");
		pattern.append("|\"/procedure/[0-9]/date\"");
		pattern.append("|\"/entry/[0-9]/resource/procedure/[0-9]/date\"");
		pattern.append("|\"/software/version\"");

		return Pattern.compile(pattern.toString());
	}

	/**
	 * @return a new {@link IGenericClient} fhirClient after setting the encoding to
	 *         JSON.
	 */
	private static IGenericClient createFhirClientAndSetEncoding() {
		IGenericClient fhirClient = ServerTestUtils.createFhirClient();
		fhirClient.setEncoding(EncodingEnum.JSON);

		return fhirClient;
	}

	/**
	 * @param fhirClient
	 *            The {@link IGenericClient} to register the interceptor to.
	 * 
	 * @return a new {@link JsonInterceptor} after registering it with the
	 *         fhirClient.
	 */
	private static JsonInterceptor createAndRegisterJsonInterceptor(IGenericClient fhirClient) {
		JsonInterceptor jsonInterceptor = new JsonInterceptor();
		fhirClient.registerInterceptor(jsonInterceptor);

		return jsonInterceptor;
	}

	/**
	 * @return the path to where a file should be written.
	 */
	private static Path getTargetResponseDir() {
		Path targetResponseDir = Paths.get("..", "target");
		if (!Files.isDirectory(targetResponseDir))
			targetResponseDir = Paths.get("target");
		if (!Files.isDirectory(targetResponseDir))
			throw new IllegalStateException();

		new File(targetResponseDir.toString() + "endpoint-response").mkdirs();

		return targetResponseDir;
	}

	/**
	 * @param directory
	 *            The path to where the file should should be written
	 * @param endpoint
	 *            The string to identify which endpoint's response the file contents
	 *            contain
	 * @return a path to use as a filename
	 */
	private static Path generateFileName(Path directory, String endpoint) {
		return Paths.get(directory.toString(), endpoint + ".json");
	}

	/**
	 * @param contents
	 *            The string to be written to a file
	 * @param fileName
	 *            The path to name the file
	 */
	private static void writeFile(String contents, Path fileName) {
		try {
			File jsonFile = new File(fileName.toString());
			FileWriter fileWriter = new FileWriter(jsonFile);
			fileWriter.write(contents);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param path
	 *            The path to the file
	 * @return the contents of the file as a string.
	 */
	private static String readFile(Path path) {
		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(path);
		} catch (IOException e) {
			throw new UncheckedIOException("Can't read file at " + path.toString(), e);
		}
		return new String(encoded, Charset.forName("UTF-8"));
	}

	/**
	 * NodeFilter is a simple interface with one method that takes a single
	 * argument, {@link JsonNode}, and returns true if the JsonNode satisfies the
	 * filter.
	 */
	private static interface NodeFilter {
		boolean apply(JsonNode node);
	}

	/**
	 * NodeFilteringConsumer implements the {@link Consumer} interface, and is used
	 * to filter out fields in a JsonNode that meet requirements as specified by a
	 * given {@link NodeFilter}.
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
	 * Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called after
	 * each test case.
	 */
	@After
	public void cleanDatabaseServerAfterEachTestCase() {
		ServerTestUtils.cleanDatabaseServer();
	}
}
