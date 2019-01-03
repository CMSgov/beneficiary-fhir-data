package gov.hhs.cms.bluebutton.server.app.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

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

public final class EndpointJsonResponseComparatorIT {

	private static JsonInterceptor jsonInterceptor;
	private static IGenericClient fhirClient;
	private static List<Object> loadedRecords;
	private static String beneficiaryId;

	@Test
	public void compareEndpointJsonResponse() {
		Path approvedResponseDir = Paths.get(".", "src", "test", "resources", "endpoint-responses");
		if (!Files.isDirectory(approvedResponseDir))
			throw new IllegalStateException();

		Path targetResponseDir = Paths.get(".", "target", "generated-test-sources", "endpoint-responses");
		if (!Files.isDirectory(targetResponseDir))
			new File(targetResponseDir.toString()).mkdirs();

		// Create fhir client and register the jsonInterceptor
		loadedRecords = ServerTestUtils
				.loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
		fhirClient = ServerTestUtils.createFhirClient();
		fhirClient.setEncoding(EncodingEnum.JSON);
		jsonInterceptor = new JsonInterceptor();
		fhirClient.registerInterceptor(jsonInterceptor);

		createEndpointResponseFiles(targetResponseDir);

		createEndpointDiffFiles(approvedResponseDir, targetResponseDir);
	}

	/*
	 * @param targetResponseDir
	 *            The path to where the files should be written
	 */
	private static void createEndpointResponseFiles(Path targetResponseDir) {
		Beneficiary beneficiary = loadedRecords.stream().filter(r -> r instanceof Beneficiary).map(r -> (Beneficiary) r)
				.findFirst().get();
		beneficiaryId = beneficiary.getBeneficiaryId();

		// Patient
		Patient patient = fhirClient.read(Patient.class, beneficiaryId);
		Assert.assertNotNull(patient);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "patient-read", beneficiaryId));

		Bundle beneficiaryBundle = fhirClient.search().forResource(Patient.class)
				.where(Patient.RES_ID.exactly().systemAndIdentifier(null, beneficiaryId)).returnBundle(Bundle.class)
				.execute();
		Assert.assertNotNull(beneficiaryBundle);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "patient-search", beneficiaryId));

		Bundle beneficiaryBundleHicn = fhirClient.search().forResource(Patient.class)
				.where(Patient.IDENTIFIER.exactly()
						.systemAndIdentifier(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, beneficiary.getHicn()))
				.returnBundle(Bundle.class).execute();
		Assert.assertNotNull(beneficiaryBundleHicn);
		writeFile(jsonInterceptor.getResponse(),
				generateFileName(targetResponseDir, "patient-search-hicn", beneficiaryId));

		// Coverage
		Coverage partACoverage = fhirClient.read(Coverage.class,
				TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiaryId));
		Assert.assertNotNull(partACoverage);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "coverage-read", beneficiaryId));

		Bundle coverageBundle = fhirClient.search().forResource(Coverage.class)
				.where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiaryId)))
				.returnBundle(Bundle.class).execute();
		Assert.assertNotNull(coverageBundle);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "coverage-search", beneficiaryId));

		// Capability Statement
		CapabilityStatement capabilities = fhirClient.fetchConformance().ofType(CapabilityStatement.class).execute();
		Assert.assertNotNull(capabilities);
		writeFile(jsonInterceptor.getResponse(),
				generateFileName(targetResponseDir, "capability-statement", beneficiaryId));

		// EOB reads and searchByPatient
		CarrierClaim carrClaim = loadedRecords.stream().filter(r -> r instanceof CarrierClaim)
				.map(r -> (CarrierClaim) r).findFirst().get();
		ExplanationOfBenefit carrEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.CARRIER, carrClaim.getClaimId()));
		Assert.assertNotNull(carrEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "carr-claim-read", beneficiaryId));

		DMEClaim dmeClaim = loadedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> (DMEClaim) r).findFirst()
				.get();
		ExplanationOfBenefit dmeEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.DME, dmeClaim.getClaimId()));
		Assert.assertNotNull(dmeEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "dme-claim-read", beneficiaryId));

		HHAClaim hhaClaim = loadedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> (HHAClaim) r).findFirst()
				.get();
		ExplanationOfBenefit hhaEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.HHA, hhaClaim.getClaimId()));
		Assert.assertNotNull(hhaEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "hha-claim-read", beneficiaryId));

		HospiceClaim hosClaim = loadedRecords.stream().filter(r -> r instanceof HospiceClaim).map(r -> (HospiceClaim) r)
				.findFirst().get();
		ExplanationOfBenefit hosEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.HOSPICE, hosClaim.getClaimId()));
		Assert.assertNotNull(hosEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "hos-claim-read", beneficiaryId));

		InpatientClaim inpClaim = loadedRecords.stream().filter(r -> r instanceof InpatientClaim)
				.map(r -> (InpatientClaim) r).findFirst().get();
		ExplanationOfBenefit inpEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.INPATIENT, inpClaim.getClaimId()));
		Assert.assertNotNull(inpEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "inp-claim-read", beneficiaryId));

		OutpatientClaim outClaim = loadedRecords.stream().filter(r -> r instanceof OutpatientClaim)
				.map(r -> (OutpatientClaim) r).findFirst().get();
		ExplanationOfBenefit outEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.OUTPATIENT, outClaim.getClaimId()));
		Assert.assertNotNull(outEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "out-claim-read", beneficiaryId));

		PartDEvent pdeClaim = loadedRecords.stream().filter(r -> r instanceof PartDEvent).map(r -> (PartDEvent) r)
				.findFirst().get();
		ExplanationOfBenefit pdeEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.PDE, pdeClaim.getEventId()));
		Assert.assertNotNull(pdeEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "pde-claim-read", beneficiaryId));

		SNFClaim snfClaim = loadedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> (SNFClaim) r).findFirst()
				.get();
		ExplanationOfBenefit snfEob = fhirClient.read(ExplanationOfBenefit.class,
				TransformerUtils.buildEobId(ClaimType.SNF, snfClaim.getClaimId()));
		Assert.assertNotNull(snfEob);
		writeFile(jsonInterceptor.getResponse(), generateFileName(targetResponseDir, "snf-claim-read", beneficiaryId));

		Bundle searchResults = fhirClient.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(beneficiary)))
				.returnBundle(Bundle.class).execute();
		Assert.assertNotNull(searchResults);
		writeFile(jsonInterceptor.getResponse(),
				generateFileName(targetResponseDir, "search-by-patient", beneficiaryId));
	}

	/*
	 * @param approvedResponseDir
	 *            The path to where the current approved responses reside
	 * @param targetResponseDir
	 *            The path to where the recently created responses reside
	 */
	private static void createEndpointDiffFiles(Path approvedResponseDir, Path targetResponseDir) {
		Path targetDiffDir = Paths.get(".", "target", "generated-test-sources", "diff-endpoint-responses");
		if (!Files.isDirectory(targetDiffDir))
			new File(targetDiffDir.toString()).mkdirs();

		File approvedResponseFolder = new File(approvedResponseDir.toString());
		File[] approvedResponseFiles = approvedResponseFolder.listFiles();

		for (File file : approvedResponseFiles) {
			try {
				String approvedJSON = readFile(approvedResponseDir.toString() + "\\" + file.getName(),
						Charset.defaultCharset());
				String newJSON = readFile(targetResponseDir.toString() + "\\" + file.getName(),
						Charset.defaultCharset());

				ObjectMapper mapper = new ObjectMapper();
				JsonNode beforeNode = mapper.readTree(approvedJSON);
				JsonNode afterNode = mapper.readTree(newJSON);
				JsonNode patch = JsonDiff.asJson(beforeNode, afterNode);

				writeFile(patch.toString(), targetDiffDir.toString() + "\\" + file.getName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * @param directory 
	 *            The path to where the file should should be written
	 * @param endpoint 
	 * 			  The string to identify which endpoint's response the
	 * 			  file contents contain
	 * @param beneId 
	 * 			  The string to identify the beneficiary record used to
	 * 			  generate the response
	 * @return Returns a string to use as a filename
	 */
	private static String generateFileName(Path directory, String endpoint, String beneId) {
		return directory.toString() + "\\" + endpoint + "-" + beneId + ".json";
	}

	/*
	 * @param contents
	 *            The string to be written to a file
	 * @param fileName
	 * 			  The string to name the file
	 */
	private static void writeFile(String contents, String fileName) {
		try {
			File jsonFile = new File(fileName);
			FileWriter fileWriter = new FileWriter(jsonFile);
			fileWriter.write(contents);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * @param path
	 *            The string with a path to the file
	 * @param encoding
	 * 			  The charset with which to decode the bytes
	 */
	private static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}
