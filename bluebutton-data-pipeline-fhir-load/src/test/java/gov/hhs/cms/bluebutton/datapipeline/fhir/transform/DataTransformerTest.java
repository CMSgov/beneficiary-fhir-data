package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Observation;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.context.FhirContext;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirTestUtilities;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CompoundCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DMEClaimGroup.DMEClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DrugCoverageStatus;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HHAClaimGroup.HHAClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.HospiceClaimGroup.HospiceClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.InpatientClaimGroup.InpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.OutpatientClaimGroup.OutpatientClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.SNFClaimGroup.SNFClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.StaticRifResource;

/**
 * Unit tests for {@link DataTransformer}.
 */
public final class DataTransformerTest {
	public static final Logger LOGGER = LoggerFactory.getLogger(DataTransformerTest.class);

	/**
	 * Verifies that {@link DataTransformer} works correctly when passed an
	 * empty {@link RifRecordEvent} stream.
	 */
	@Test
	public void transformEmptyRifStream() {
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());

		Stream<RifRecordEvent<?>> source = new ArrayList<RifRecordEvent<?>>().stream();
		Stream<TransformedBundle> result = transformer.transform(source);
		Assert.assertNotNull(result);
		Assert.assertEquals(0, result.count());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link BeneficiaryRow} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertBeneficiaryEvent() {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_BENES);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryRow);
		BeneficiaryRow record = (BeneficiaryRow) rifRecordEvent.getRecord();
		
		// Create Mock
		Stream source = Arrays.asList(rifRecordEvent).stream();

		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle beneBundleWrapper = resultList.get(0);
		Assert.assertNotNull(beneBundleWrapper);
		Assert.assertSame(rifRecordEvent, beneBundleWrapper.getSource());
		Assert.assertNotNull(beneBundleWrapper.getResult());
		/*
		 * Bundle should have: 1) Patient, 2) Coverage (part A), 3) Coverage
		 * (part B), 4) Coverage (part D).
		 */
		Bundle beneBundle = beneBundleWrapper.getResult();
		Assert.assertEquals(4, beneBundle.getEntry().size());
		BundleEntryComponent beneEntry = beneBundle.getEntry().stream().filter(r -> r.getResource() instanceof Patient)
				.findAny().get();
		Patient bene = (Patient) beneEntry.getResource();

		FhirContext ctx = FhirContext.forDstu3();
		String encoded = ctx.newXmlParser().encodeResourceToString(bene);
		// System.out.println(encoded);

		Assert.assertEquals(false, encoded.contains("Optional"));

		Assert.assertEquals(1, bene.getAddress().size());
		Assert.assertEquals(record.stateCode, bene.getAddress().get(0).getState());
		Assert.assertEquals(record.countyCode, bene.getAddress().get(0).getDistrict());
		Assert.assertEquals(record.postalCode, bene.getAddress().get(0).getPostalCode());
		Assert.assertEquals(Date.valueOf(record.birthDate), bene.getBirthDate());
		Assert.assertEquals("MALE", bene.getGender().toString().trim());
		assertExtensionCodingEquals(bene, DataTransformer.EXTENSION_US_CORE_RACE,
				DataTransformer.CODING_SYSTEM_RACE, "1");
		Assert.assertEquals(record.nameGiven, bene.getName().get(0).getGiven().get(0).toString());
		Assert.assertEquals(record.nameMiddleInitial.get().toString(),
				bene.getName().get(0).getGiven().get(1).toString());
		Assert.assertEquals(record.nameSurname, bene.getName().get(0).getFamily());

		BundleEntryComponent[] coverageEntry = beneBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Coverage).toArray(BundleEntryComponent[]::new);

		Coverage partA = (Coverage) coverageEntry[0].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partA.getGrouping().getSubGroup());
		Assert.assertEquals(CoverageStatus.ACTIVE, partA.getStatus());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_A, partA.getGrouping().getSubPlan());

		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
				DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, record.medicareEnrollmentStatusCode.get());

		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL,
				DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL, "1");
		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT,
				DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT, "1");
		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_ESRD_INDICATOR,
				DataTransformer.CODING_SYSTEM_CCW_ESRD_INDICATOR, "N");

		FhirContext ctxPartA = FhirContext.forDstu3();
		String encodedPartA = ctxPartA.newXmlParser().encodeResourceToString(partA);
		// System.out.println(encodedPartA);

		Coverage partB = (Coverage) coverageEntry[1].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN,
				partB.getGrouping().getSubGroup());
		Assert.assertEquals(CoverageStatus.ACTIVE, partB.getStatus());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_B, partB.getGrouping().getSubPlan());
		assertExtensionCodingEquals(partB, DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
				DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, record.medicareEnrollmentStatusCode.get());

		Coverage partD = (Coverage) coverageEntry[2].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partD.getGrouping().getSubGroup());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_D, partD.getGrouping().getSubPlan());
		assertExtensionCodingEquals(partB, DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD,
				DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD, record.medicareEnrollmentStatusCode.get());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when passed a
	 * single {@link BeneficiaryRow} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}, where all {@link Optional} fields are missing
	 * values.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertBeneficiaryEventWithOptionalsMissing() {
		// Read sample data from text file and strip Optionals.
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_BENES);
		rifRecordEvent = copyWithoutOptionalValues(rifRecordEvent);

		// Just ensure that the transform succeeds without errors.
		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());
	}

	/**
	 * Verify that the {@link DataTransformer#computeHicnHash(String)} algorithm
	 * doesn't produce any collisions. Such collisions would result in
	 * beneficiaries being able to see other beneficiaries' data. That'd be bad.
	 */
	@Test
	@Ignore("Won't ever complete. Needs too much RAM.")
	public void checkForHicnHashCollisions() {
		/*
		 * Actual HICNs follow a format, but it's poorly documented so for our
		 * purposes here we'll simplify it to the following: "123456789X". The
		 * first nine digits are social security numbers, which are followed by
		 * a one-character suffix of 'A, 'B', 'C', 'D', or 'E'.
		 */

		/*
		 * If we don't limit the SSN search space, this test will run more-or
		 * less forever. So instead, we limit our search to a random 60M SSNs,
		 * which has the added benefit of allowing this test to estimate the
		 * time that HICN hashing will add to an initial load.
		 */
		// FIXME we can get through 7M with a 12GB heap before mem pressure
		int maxValidSsn = 899999999;
		int startSsn = new Random().nextInt(maxValidSsn - 6000000 + 1);

		LoadAppOptions options = FhirTestUtilities.getLoadOptions();
		SecureRandom rng = new SecureRandom();
		byte[] randomPepper = new byte[128 / 8]; // 128 bits
		rng.nextBytes(randomPepper);
		options = new LoadAppOptions(1000, randomPepper, options.getFhirServer(), options.getKeyStorePath(),
				options.getKeyStorePassword(), options.getTrustStorePath(), options.getTrustStorePassword(),
				options.getLoaderThreads());

		Map<String, String> knownHashes = new HashMap<>();
		for (int ssn = startSsn; ssn <= maxValidSsn; ssn++) {
			for (char suffix = 'A'; suffix <= 'E'; suffix++) {
				// (This will left-pad the SSNs with zeroes.)
				String hicn = String.format("%09d%c", ssn, suffix);

				String hash = DataTransformer.computeHicnHash(options, hicn);
				Assert.assertFalse(String.format("Hash collision for HICN hash of '%s': '%s and '%s'.", hash, hicn,
						knownHashes.get(hash)), knownHashes.containsKey(hash));
				knownHashes.put(hash, hicn);
			}

			if ((ssn - startSsn) % 10000 == 0)
				LOGGER.info("Completed SSN {}", (ssn - startSsn));
		}
	}

	/**
	 * Runs a couple of fake HICNs through
	 * {@link DataTransformer#computeHicnHash(LoadAppOptions, String)} to verify
	 * that the expected result is produced.
	 */
	@Test
	public void verifyHicnHashOutput() {
		LoadAppOptions options = FhirTestUtilities.getLoadOptions();
		options = new LoadAppOptions(1000, "nottherealpepper".getBytes(StandardCharsets.UTF_8), options.getFhirServer(),
				options.getKeyStorePath(), options.getKeyStorePassword(), options.getTrustStorePath(),
				options.getTrustStorePassword(), options.getLoaderThreads());

		/*
		 * These are the two samples from `dev/design-decisions-readme.md` that
		 * the frontend and backend both have tests to verify the result of.
		 */
		Assert.assertEquals("d95a418b0942c7910fb1d0e84f900fe12e5a7fd74f312fa10730cc0fda230e9a",
				DataTransformer.computeHicnHash(options, "123456789A"));
		Assert.assertEquals("6357f16ebd305103cf9f2864c56435ad0de5e50f73631159772f4a4fcdfe39a5",
				DataTransformer.computeHicnHash(options, "987654321E"));
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link PartDEventRow} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             indicates test failure
	 */

	@Test
	public void transformInsertPartDEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_PDE);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);

		PartDEventRow pdeRecord = (PartDEventRow) rifRecordEvent.getRecord();
		Bundle pdeBundle = getBundle(pdeRecord);

		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, pdeBundle.getEntry().size());

		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_PDE_ID, pdeRecord.partDEventId,
				eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, pdeRecord.claimGroupId,
				eob.getIdentifier());
		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_CLAIM_TYPE, "pharmacy", eob.getType());
		Assert.assertEquals(DataTransformer.referencePatient(pdeRecord.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		Assert.assertEquals(Date.valueOf(pdeRecord.paymentDate.get()), eob.getPayment().getDate());

		Assert.assertEquals("01", pdeRecord.serviceProviderIdQualiferCode);
		Assert.assertEquals("01", pdeRecord.prescriberIdQualifierCode);

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_ACT, "RXDINV", rxItem.getDetail().get(0).getType());

		Assert.assertEquals(Date.valueOf(pdeRecord.prescriptionFillDate), rxItem.getServicedDateType().getValue());

		assertReferenceEquals(DataTransformer.CODING_SYSTEM_NPI_US, pdeRecord.serviceProviderId, eob.getOrganization());
		assertReferenceEquals(DataTransformer.CODING_SYSTEM_NPI_US, pdeRecord.serviceProviderId, eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, pdeRecord.pharmacyTypeCode);

		// Default case has drug coverage status code as Covered
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PART_D_COVERED,
				pdeRecord.partDPlanCoveredPaidAmount, rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT,
				rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC,
				rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PATIENT_PAY, pdeRecord.patientPaidAmount,
				rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_OTHER_TROOP_AMOUNT,
				pdeRecord.otherTrueOutOfPocketPaidAmount, rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LOW_INCOME_SUBSIDY_AMOUNT,
				pdeRecord.lowIncomeSubsidyPaidAmount, rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PATIENT_LIABILITY_REDUCED_AMOUNT,
				pdeRecord.patientLiabilityReductionOtherPaidAmount, rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_COST, pdeRecord.totalPrescriptionCost,
				rxItem.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_GAP_DISCOUNT_AMOUNT, pdeRecord.gapDiscountAmount,
				rxItem.getAdjudication());

		Coverage coverage = (Coverage) eob.getInsurance().getCoverage().getResource();

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID, pdeRecord.planContractId,
				coverage.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID,
				pdeRecord.planBenefitPackageId, coverage.getIdentifier());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, coverage.getGrouping().getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_D, coverage.getGrouping().getSubPlan());

	}

	/**
	 * Verifies that {@link DataTransformer} correctly sets the code system
	 * value when the compound code equals compounded.
	 * 
	 * @throws FHIRException
	 *             indicates test failure
	 */

	@Test
	public void transformInsertPartDEventCompound() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_PDE);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);
		PartDEventRow pdeRecord = (PartDEventRow) rifRecordEvent.getRecord();

		pdeRecord.compoundCode = CompoundCode.COMPOUNDED;

		Bundle pdeBundle = getBundle(pdeRecord);

		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_ACT, "RXCINV", rxItem.getDetail().get(0).getType());
	}

	/**
	 * Verifies that the {@link DataTransformer} correctly uses the
	 * {@link DataTransformer#CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT
	 * code when the drug coverage status code =
	 * {@link PartDEventRow#DRUG_CVRD_STUS_CD_SUPPLEMENT}
	 * 
	 * @throws FHIRException
	 */

	@Test
	public void transformInsertPartDEventNonCoveredSupplement() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_PDE);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);
		PartDEventRow pdeRecord = (PartDEventRow) rifRecordEvent.getRecord();

		pdeRecord.drugCoverageStatusCode = DrugCoverageStatus.SUPPLEMENTAL;

		Bundle pdeBundle = getBundle(pdeRecord);
		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		/*
		 * Assert that when the drug coverage status code equals non-covered
		 * supplement, the adjudication uses the
		 * PartDEventRow.partDPlanNonCoveredPaidAmount field.
		 */
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT,
				pdeRecord.partDPlanNonCoveredPaidAmount, rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_COVERED, rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC,
				rxItem.getAdjudication());
	}

	/**
	 * Verifies that the {@link DataTransformer} correctly uses the
	 * {@link DataTransformer#CODED_ADJUDICATION_PART_D_NONCOVERED_OTC code when
	 * the drug coverage status code =
	 * {@link PartDEventRow#DRUG_CVRD_STUS_CD_OTC}
	 * 
	 * @throws FHIRException
	 */

	@Test
	public void transformInsertPartDEventNonCoveredOTC() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_PDE);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof PartDEventRow);
		PartDEventRow pdeRecord = (PartDEventRow) rifRecordEvent.getRecord();

		pdeRecord.drugCoverageStatusCode = DrugCoverageStatus.OVER_THE_COUNTER;

		Bundle pdeBundle = getBundle(pdeRecord);
		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		/*
		 * Assert that when the drug coverage status code equals non-covered
		 * OTC, the adjudication uses the
		 * PartDEventRow.partDPlanNonCoveredPaidAmount field.
		 */
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_OTC,
				pdeRecord.partDPlanNonCoveredPaidAmount, rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_COVERED, rxItem.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT,
				rxItem.getAdjudication());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link CarrierClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertCarrierClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_CARRIER);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaimGroup);
    
		// Verify the claim header.
		CarrierClaimGroup record = (CarrierClaimGroup) rifRecordEvent.getRecord();
		
		// Verify one of the claim lines.
		CarrierClaimLine recordLine1 = record.lines.get(0);
		
		// Creating Mock	
		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle carrierBundleWrapper = resultList.get(0);
		Assert.assertNotNull(carrierBundleWrapper);
		Assert.assertSame(rifRecordEvent, carrierBundleWrapper.getSource());
		Assert.assertNotNull(carrierBundleWrapper.getResult());

		Bundle claimBundle = carrierBundleWrapper.getResult();
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());
		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertExtensionCodingEquals(eob.getType(), DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD,
				DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD, record.nearLineRecordIdCode.toString());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, record.carrierNumber);
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD, record.paymentDenialCode);
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				referral.getSubject().getReference());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.referringPhysicianNpi.get(),
				referral.getRequester().getAgent());
		Assert.assertEquals(1, referral.getRecipient().size());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.referringPhysicianNpi.get(),
				referral.getRecipientFirstRep());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
				DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT, "A");

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE, DataTransformer.CODED_ADJUDICATION_PAYMENT_B,
				record.providerPaymentAmount, eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT, record.submittedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, record.allowedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		Assert.assertEquals(6, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
				DataTransformer.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER, record.clinicalTrialNumber.get());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		assertCareTeamEquals(recordLine1.performingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		CareTeamComponent performingCareTeamEntry = findCareTeamEntryForProviderIdentifier(
				recordLine1.performingPhysicianNpi.get(), eob.getCareTeam());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
				recordLine1.providerSpecialityCode.get(), performingCareTeamEntry.getQualification());
		assertExtensionCodingEquals(performingCareTeamEntry, DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_TYPE_CD, "" + recordLine1.providerTypeCode);
		assertExtensionCodingEquals(performingCareTeamEntry,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
				"" + recordLine1.providerParticipatingIndCode.get());
		assertExtensionCodingEquals(performingCareTeamEntry, DataTransformer.CODING_SYSTEM_NPI_US,
				DataTransformer.CODING_SYSTEM_NPI_US, "" + recordLine1.organizationNpi.get());

		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD, recordLine1.providerStateCode.get());
		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD, recordLine1.providerZipCode.get());

		Assert.assertEquals(recordLine1.serviceCount, eobItem0.getQuantity().getValue());

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, recordLine1.cmsServiceTypeCode,
				eobItem0.getCategory());

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, recordLine1.placeOfServiceCode,
				eobItem0.getLocationCodeableConcept());
		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CCW_PRICING_LOCALITY,
				DataTransformer.CODING_SYSTEM_CCW_PRICING_LOCALITY, "15");

		assertDateEquals(recordLine1.firstExpenseDate.get(), eobItem0.getServicedPeriod().getStartElement());
		assertDateEquals(recordLine1.lastExpenseDate.get(), eobItem0.getServicedPeriod().getEndElement());

		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, "" + record.hcpcsYearCode.get(),
				recordLine1.hcpcsCode.get(), eobItem0.getService());
		Assert.assertEquals(1, eobItem0.getModifier().size());
		assertHasCoding(DataTransformer.HCPCS_INITIAL_MODIFIER_CODE1, "" + record.hcpcsYearCode.get(),
				recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(0));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_BETOS, DataTransformer.CODING_SYSTEM_BETOS,
				recordLine1.betosCode.get());

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH,
				DataTransformer.CODING_SYSTEM_CMS_LINE_DEDUCTIBLE_SWITCH, "" + recordLine1.serviceDeductibleCode.get());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		AdjudicationComponent adjudicationForPayment = eobItem0.getAdjudication().stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(),
						DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, DataTransformer.CODED_ADJUDICATION_PAYMENT))
				.findAny()
				.get();
		assertExtensionCodingEquals(adjudicationForPayment,
				DataTransformer.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH,
				DataTransformer.CODING_SYSTEM_CMS_LINE_PAYMENT_INDICATOR_SWITCH, "" + recordLine1.paymentCode.get());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				recordLine1.beneficiaryPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT_B, recordLine1.providerPaymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_DEDUCTIBLE,
				recordLine1.beneficiaryPartBDeductAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				recordLine1.primaryPayerPaidAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT,
				recordLine1.coinsuranceAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT,
				recordLine1.submittedChargeAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, recordLine1.allowedChargeAmount,
				eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_MTUS_CD,
				DataTransformer.CODING_SYSTEM_MTUS_CD, String.valueOf(recordLine1.mtusCode.get()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_MTUS_COUNT,
				DataTransformer.CODING_SYSTEM_MTUS_COUNT, String.valueOf(recordLine1.mtusCount));

		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT,
				DataTransformer.CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION,
				"" + recordLine1.reducedPaymentPhysicianAsstCode,
				eobItem0.getAdjudication());
		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR,
				DataTransformer.CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR, recordLine1.processingIndicatorCode.get(),
				eobItem0.getAdjudication());

		assertDiagnosisLinkPresent(recordLine1.diagnosis, eob, eobItem0);

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_NDC, DataTransformer.CODING_SYSTEM_NDC,
				recordLine1.nationalDrugCode.get());

		List<Extension> hctHgbObservationExtension = eobItem0
				.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_HCT_OR_HGB_RESULTS);
		Assert.assertEquals(1, hctHgbObservationExtension.size());
		Assert.assertTrue(hctHgbObservationExtension.get(0).getValue() instanceof Reference);
		Reference hctHgbReference = (Reference) hctHgbObservationExtension.get(0).getValue();
		Assert.assertTrue(hctHgbReference.getResource() instanceof Observation);
		Observation hctHgbObservation = (Observation) hctHgbReference.getResource();
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CMS_HCT_OR_HGB_TEST_TYPE, recordLine1.hctHgbTestTypeCode.get(),
				hctHgbObservation.getCode().getCodingFirstRep());
		Assert.assertEquals(recordLine1.hctHgbTestResult, hctHgbObservation.getValueQuantity().getValue());

		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CLIA_LAB_NUM,
				DataTransformer.CODING_SYSTEM_CLIA_LAB_NUM, recordLine1.cliaLabNumber.get());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link InpatientClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertInpatientClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_INPATIENT);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof InpatientClaimGroup);

		// Verify the claim header.
		InpatientClaimGroup record = (InpatientClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		InpatientClaimLine recordLine1 = record.lines.get(0);

		// Create Mock
		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle inpatientBundleWrapper = resultList.get(0);
		Assert.assertNotNull(inpatientBundleWrapper);
		Assert.assertSame(rifRecordEvent, inpatientBundleWrapper.getSource());
		Assert.assertNotNull(inpatientBundleWrapper.getResult());

		Bundle claimBundle = inpatientBundleWrapper.getResult();

		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());
		Assert.assertEquals("active", eob.getStatus().toCode());
		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		assertExtensionCodingEquals(eob.getBillablePeriod(), DataTransformer.CODING_SYSTEM_QUERY_CD,
				DataTransformer.CODING_SYSTEM_QUERY_CD, String.valueOf(record.claimQueryCode));

		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		assertDateEquals(record.claimAdmissionDate.get(), eob.getHospitalization().getStartElement());
		assertDateEquals(record.beneficiaryDischargeDate.get(), eob.getHospitalization().getEndElement());

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_BENEFIT_DEDUCTIBLE_AMT_URL, record.deductibleAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PASS_THRU_PER_DIEM_AMT, record.passThruPerDiemAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BENEFIT_COIN_AMT_URL, record.partACoinsuranceLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL, record.bloodDeductibleLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_PROFFESIONAL_CHARGE_URL, record.professionalComponentCharge,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL, record.noncoveredCharge,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_INPATIENT_TOTAL_AMT_URL, record.totalDeductionAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL, record.claimTotalPPSCapitalAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL, record.claimPPSCapitalFSPAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL, record.claimPPSCapitalOutlierAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL,
				record.claimPPSCapitalDisproportionateShareAmt.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL,
				record.claimPPSCapitalIMEAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL, record.claimPPSCapitalExceptionAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL,
				record.claimPPSOldCapitalHoldHarmlessAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_UTILIZATION_DAY_COUNT, record.utilizationDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_COINSURANCE_DAY_COUNT, record.coinsuranceDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT, record.nonUtilizationDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY, record.bloodPintsFurnishedQty,
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL,
				record.nchDrugOutlierApprovedPaymentAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getOrganization());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(record.claimFacilityTypeCode));

		assertExtensionCodingEquals(eob.getType(),
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(record.claimServiceClassificationTypeCode));

		assertCareTeamEquals(record.attendingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		assertCareTeamEquals(record.operatingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_ASSISTING, eob);
		assertCareTeamEquals(record.otherPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_OTHER, eob);

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> DataTransformer.isCodeInConcept(i.getCategory(),
						DataTransformer.CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
						String.valueOf(record.diagnosisRelatedGroupCd.get()))));

		Assert.assertEquals(9, eob.getDiagnosis().size());

		assertHasCoding(DataTransformer.computeFhirSystem(record.procedureCodes.get(0)),
				record.procedureCodes.get(0).getCode(),
				eob.getProcedure().get(0).getProcedureCodeableConcept());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());
		assertHasCoding(DataTransformer.computeFhirSystem(record.procedureCodes.get(1)),
				record.procedureCodes.get(1).getCode(), eob.getProcedure().get(1).getProcedureCodeableConcept());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(1).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(1).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertCareTeamEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(),
				DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);

		assertHasCoding(DataTransformer.CODING_SYSTEM_REVENUE_CENTER, recordLine1.revenueCenter, eobItem0.getRevenue());

		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0.getRevenue(), DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(recordLine1.deductibleCoinsuranceCd.get()));

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link OutpatientClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertOutpatientClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_OUTPATIENT);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof OutpatientClaimGroup);

		// Verify the claim header.
		OutpatientClaimGroup record = (OutpatientClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		OutpatientClaimLine recordLine1 = record.lines.get(0);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle OutpatientBundleWrapper = resultList.get(0);
		Assert.assertNotNull(OutpatientBundleWrapper);
		Assert.assertSame(rifRecordEvent, OutpatientBundleWrapper.getSource());
		Assert.assertNotNull(OutpatientBundleWrapper.getResult());

		Bundle claimBundle = OutpatientBundleWrapper.getResult();

		/*
		 * Bundle should have: 1) EOB
		 */

		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());
		Assert.assertEquals("active", eob.getStatus().toCode());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		assertExtensionCodingEquals(eob.getBillablePeriod(), DataTransformer.CODING_SYSTEM_QUERY_CD,
				DataTransformer.CODING_SYSTEM_QUERY_CD, String.valueOf(record.claimQueryCode));
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, record.claimNonPaymentReasonCode.get());

		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());

		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BEN_PART_B_DED_AMT_URL, record.deductibleAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL, record.bloodDeductibleLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_PROFFESIONAL_CHARGE_URL, record.professionalComponentCharge,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BEN_PART_B_COINSUR_AMT_URL, record.coninsuranceAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PAYMENT_B, record.providerPaymentAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_OUTPAT_BEN__PAYMENT_AMT_URL, record.beneficiaryPaymentAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getOrganization());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(record.claimFacilityTypeCode));

		assertExtensionCodingEquals(eob.getType(),
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(record.claimServiceClassificationTypeCode));

		assertCareTeamEquals(record.attendingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		assertCareTeamEquals(record.operatingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_ASSISTING, eob);

		Assert.assertEquals(6, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getProcedure().size());
		assertHasCoding(DataTransformer.computeFhirSystem(record.procedureCodes.get(0)),
				record.procedureCodes.get(0).getCode(), eob.getProcedure().get(0).getProcedureCodeableConcept());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertHasCoding(DataTransformer.CODING_SYSTEM_NDC, recordLine1.nationalDrugCode.get(),
				eobItem0.getService());

		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_1ST_ANSI_CD,
				DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, recordLine1.revCntr1stAnsiCd.get(),
				eobItem0.getAdjudication());
		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_2ND_ANSI_CD,
				DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, recordLine1.revCntr2ndAnsiCd.get(),
				eobItem0.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_3RD_ANSI_CD, eobItem0.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_4TH_ANSI_CD, eobItem0.getAdjudication());

		assertHasCoding(DataTransformer.CODING_SYSTEM_REVENUE_CENTER, recordLine1.revenueCenter, eobItem0.getRevenue());
		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getModifier().get(0));
		assertHasCoding(DataTransformer.HCPCS_INITIAL_MODIFIER_CODE1, recordLine1.hcpcsInitialModifierCode.get(),
				eobItem0.getModifier().get(1));
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BLOOD_DEDUCTIBLE,
				recordLine1.bloodDeductibleAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_CASH_DEDUCTIBLE,
				recordLine1.cashDeductibleAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT,
				recordLine1.wageAdjustedCoinsuranceAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT,
				recordLine1.reducedCoinsuranceAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_1ST_MSP_AMOUNT, recordLine1.firstMspPaidAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_2ND_MSP_AMOUNT, recordLine1.secondMspPaidAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT,
				recordLine1.providerPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				recordLine1.benficiaryPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PATIENT_RESPONSIBILITY_AMOUNT,
				recordLine1.patientResponsibilityAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount, eobItem0.getAdjudication());

		assertCareTeamEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(),
				DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link SNFClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertSNFClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_SNF);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof SNFClaimGroup);

		// Verify the claim header.
		SNFClaimGroup record = (SNFClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		SNFClaimLine recordLine1 = record.lines.get(0);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle SNFBundleWrapper = resultList.get(0);
		Assert.assertNotNull(SNFBundleWrapper);
		Assert.assertSame(rifRecordEvent, SNFBundleWrapper.getSource());
		Assert.assertNotNull(SNFBundleWrapper.getResult());

		Bundle claimBundle = SNFBundleWrapper.getResult();
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());

		Assert.assertEquals("active", eob.getStatus().toCode());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		assertExtensionCodingEquals(eob.getBillablePeriod(), DataTransformer.CODING_SYSTEM_QUERY_CD,
				DataTransformer.CODING_SYSTEM_QUERY_CD, String.valueOf(record.claimQueryCode));

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_PROVIDER_NUMBER, record.providerNumber,
				eob.getProvider());
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, record.claimNonPaymentReasonCode.get());

		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getOrganization());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(record.claimFacilityTypeCode));

		assertCareTeamEquals(record.attendingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		assertCareTeamEquals(record.operatingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_ASSISTING, eob);
		assertCareTeamEquals(record.otherPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_OTHER, eob);

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_BENEFIT_DEDUCTIBLE_AMT_URL, record.deductibleAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BENEFIT_COIN_AMT_URL, record.partACoinsuranceLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL, record.bloodDeductibleLiabilityAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL, record.noncoveredCharge,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_NCH_INPATIENT_TOTAL_AMT_URL, record.totalDeductionAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL, record.claimPPSCapitalFSPAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL, record.claimPPSCapitalOutlierAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL,
				record.claimPPSCapitalDisproportionateShareAmt.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL,
				record.claimPPSCapitalIMEAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL, record.claimPPSCapitalExceptionAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL,
				record.claimPPSOldCapitalHoldHarmlessAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial());

		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_UTILIZATION_DAY_COUNT, record.utilizationDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_COINSURANCE_DAY_COUNT, record.coinsuranceDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_NON_UTILIZATION_DAY_COUNT, record.nonUtilizationDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_BLOOD_PINTS_FURNISHED_QTY, record.bloodPintsFurnishedQty,
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertInformationPeriodEquals(DataTransformer.BENEFIT_COVERAGE_DATE,
				DataTransformer.CODING_SYSTEM_QUALIFIED_STAY_DATE,
				record.qualifiedStayFromDate.get(), record.qualifiedStayThroughDate.get(), eob.getInformation());

		assertInformationPeriodEquals(DataTransformer.BENEFIT_COVERAGE_DATE,
				DataTransformer.CODING_SYSTEM_NONCOVERED_STAY_DATE,
				record.noncoveredStayFromDate.get(), record.noncoveredStayThroughDate.get(), eob.getInformation());

		assertInformationDateEquals(DataTransformer.BENEFIT_COVERAGE_DATE,
				DataTransformer.CODING_SYSTEM_BENEFITS_EXHAUSTED_DATE,
				record.medicareBenefitsExhaustedDate.get(), eob.getInformation());

		assertDateEquals(record.claimAdmissionDate.get(), eob.getHospitalization().getStartElement());
		assertDateEquals(record.beneficiaryDischargeDate.get(), eob.getHospitalization().getEndElement());

		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> DataTransformer.isCodeInConcept(i.getCategory(),
						DataTransformer.CODING_SYSTEM_DIAGNOSIS_RELATED_GROUP_CD,
						String.valueOf(record.diagnosisRelatedGroupCd.get()))));

		Assert.assertEquals(5, eob.getDiagnosis().size());
		assertHasCoding(DataTransformer.computeFhirSystem(record.procedureCodes.get(0)),
				record.procedureCodes.get(0).getCode(), eob.getProcedure().get(0).getProcedureCodeableConcept());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertHasCoding(DataTransformer.CODING_SYSTEM_REVENUE_CENTER, recordLine1.revenueCenter, eobItem0.getRevenue());
		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0.getRevenue(), DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(recordLine1.deductibleCoinsuranceCd.get()));

		assertCareTeamEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(),
				DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link HospiceClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertHospiceClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_HOSPICE);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HospiceClaimGroup);

		// Verify the claim header.
		HospiceClaimGroup record = (HospiceClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		HospiceClaimLine recordLine1 = record.lines.get(0);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle HospiceBundleWrapper = resultList.get(0);
		Assert.assertNotNull(HospiceBundleWrapper);
		Assert.assertSame(rifRecordEvent, HospiceBundleWrapper.getSource());
		Assert.assertNotNull(HospiceBundleWrapper.getResult());

		Bundle claimBundle = HospiceBundleWrapper.getResult();
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_PROVIDER_NUMBER, record.providerNumber,
				eob.getProvider());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, record.claimNonPaymentReasonCode.get());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());
		
		Assert.assertTrue(eob.getInformation().stream()
				.anyMatch(i -> DataTransformer.isCodeInConcept(i.getCategory(),
						DataTransformer.CODING_SYSTEM_PATIENT_STATUS_CD,
						String.valueOf(record.patientStatusCd.get()))));

		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_UTILIZATION_DAY_COUNT, record.utilizationDayCount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());

		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		
		assertDateEquals(record.claimHospiceStartDate.get(), eob.getHospitalization().getStartElement());
		assertDateEquals(record.beneficiaryDischargeDate.get(), eob.getHospitalization().getEndElement());
				
		assertCareTeamEquals(record.attendingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getOrganization());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(record.claimFacilityTypeCode));

		assertExtensionCodingEquals(eob.getType(),
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(record.claimServiceClassificationTypeCode));

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertHasCoding(DataTransformer.CODING_SYSTEM_REVENUE_CENTER, recordLine1.revenueCenter, eobItem0.getRevenue());

		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(), eobItem0.getService());
		assertHasCoding(DataTransformer.HCPCS_INITIAL_MODIFIER_CODE1, recordLine1.hcpcsInitialModifierCode.get(),
				eobItem0.getModifier().get(0));
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());


		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PROVIDER_PAYMENT_AMOUNT,
				recordLine1.providerPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				recordLine1.benficiaryPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount, eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0.getRevenue(), DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(recordLine1.deductibleCoinsuranceCd.get()));

		assertCareTeamEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(),
				DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link HHAClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertHHAClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_HHA);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof HHAClaimGroup);

		// Verify the claim header.
		HHAClaimGroup record = (HHAClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		HHAClaimLine recordLine1 = record.lines.get(0);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle HHABundleWrapper = resultList.get(0);
		Assert.assertNotNull(HHABundleWrapper);
		Assert.assertSame(rifRecordEvent, HHABundleWrapper.getSource());
		Assert.assertNotNull(HHABundleWrapper.getResult());

		Bundle claimBundle = HHABundleWrapper.getResult();
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId,
				eob.getIdentifier());
		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_PROVIDER_NUMBER, record.providerNumber,
				eob.getProvider());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD, record.claimNonPaymentReasonCode.get());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		
		assertExtensionCodingEquals(eob.getType(),
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD,
				String.valueOf(record.claimServiceClassificationTypeCode));
		
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());

		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getOrganization());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi.get(),
				eob.getFacility());

		assertExtensionCodingEquals(eob.getFacility(), DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD,
				DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, String.valueOf(record.claimFacilityTypeCode));

		assertCareTeamEquals(record.attendingPhysicianNpi.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);

		Assert.assertEquals(4, eob.getDiagnosis().size());

		assertBenefitBalanceUsedEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODING_SYSTEM_HHA_VISIT_COUNT, record.totalVisitCount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		assertDateEquals(record.careStartDate.get(), eob.getHospitalization().getStartElement());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_1ST_ANSI_CD,
				DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, recordLine1.revCntr1stAnsiCd.get(),
				eobItem0.getAdjudication());

		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(), eobItem0.getService());
		assertHasCoding(DataTransformer.HCPCS_INITIAL_MODIFIER_CODE1, recordLine1.hcpcsInitialModifierCode.get(),
				eobItem0.getModifier().get(0));
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());
			
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount, eobItem0.getAdjudication());
		
		assertExtensionCodingEquals(eobItem0.getRevenue(), DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				DataTransformer.CODING_SYSTEM_DEDUCTIBLE_COINSURANCE_CD,
				String.valueOf(recordLine1.deductibleCoinsuranceCd.get()));
		
		assertCareTeamEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(),
				DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link DMEClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertDMEClaimEvent() throws FHIRException {
		// Read sample data from text file
		RifRecordEvent rifRecordEvent = getSampleATestData(StaticRifResource.SAMPLE_A_DME);
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof DMEClaimGroup);

		// Verify the claim header.
		DMEClaimGroup record = (DMEClaimGroup) rifRecordEvent.getRecord();

		// Verify one of the claim lines.
		DMEClaimLine recordLine1 = record.lines.get(0);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle dmeBundleWrapper = resultList.get(0);
		Assert.assertNotNull(dmeBundleWrapper);
		Assert.assertSame(rifRecordEvent, dmeBundleWrapper.getSource());
		Assert.assertNotNull(dmeBundleWrapper.getResult());

		Bundle claimBundle = dmeBundleWrapper.getResult();
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_GRP_ID, record.claimGroupId, eob.getIdentifier());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				eob.getPatient().getReference());
		assertExtensionCodingEquals(eob.getType(), DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD,
				DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD, record.nearLineRecordIdCode.toString());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());

		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER,
				DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER, record.carrierNumber);
		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD, record.paymentDenialCode);
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER,
				DataTransformer.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER, record.clinicalTrialNumber.get());
		
		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals(DataTransformer.referencePatient(record.beneficiaryId).getReference(),
				referral.getSubject().getReference());
		assertReferenceIdentifierEquals(DataTransformer.CODING_SYSTEM_NPI_US, record.referringPhysicianNpi.get(),
				referral.getRequester().getAgent());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
				DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT, "A");

		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE, DataTransformer.CODED_ADJUDICATION_PAYMENT_B,
				record.providerPaymentAmount, eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT, record.submittedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());
		assertBenefitBalanceEquals(DataTransformer.BENEFIT_BALANCE_TYPE,
				DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, record.allowedChargeAmount,
				eob.getBenefitBalanceFirstRep().getFinancial());

		Assert.assertEquals(3, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));
		
		assertCareTeamEquals(recordLine1.providerNPI.get(), DataTransformer.CARE_TEAM_ROLE_PRIMARY, eob);
		CareTeamComponent performingCareTeamEntry = findCareTeamEntryForProviderIdentifier(
				recordLine1.providerNPI.get(),
				eob.getCareTeam());
		assertHasCoding(DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_SPECIALTY_CD,
				recordLine1.providerSpecialityCode.get(), performingCareTeamEntry.getQualification());
		assertExtensionCodingEquals(performingCareTeamEntry,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_PARTICIPATING_CD,
				"" + recordLine1.providerParticipatingIndCode.get());

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE,
				(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS));

		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD,
				DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD, recordLine1.providerStateCode);

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, recordLine1.cmsServiceTypeCode,
				eobItem0.getCategory());

		assertHasCoding(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, recordLine1.placeOfServiceCode,
				eobItem0.getLocationCodeableConcept());

		assertDateEquals(recordLine1.firstExpenseDate.get(), eobItem0.getServicedPeriod().getStartElement());
		assertDateEquals(recordLine1.lastExpenseDate.get(), eobItem0.getServicedPeriod().getEndElement());

		assertHasCoding(DataTransformer.HCPCS_INITIAL_MODIFIER_CODE1, recordLine1.hcpcsInitialModifierCode.get(),
				eobItem0.getModifier().get(0));
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());

		assertHasCoding(DataTransformer.CODING_SYSTEM_HCPCS, "" + record.hcpcsYearCode.get(),
				recordLine1.hcpcsCode.get(),
				eobItem0.getService());
		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_BETOS, DataTransformer.CODING_SYSTEM_BETOS,
				recordLine1.betosCode.get());
						
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BENEFICIARY_PAYMENT_AMOUNT,
				recordLine1.beneficiaryPaymentAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT_B, recordLine1.providerPaymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_DEDUCTIBLE,
				recordLine1.beneficiaryPartBDeductAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				recordLine1.primaryPayerPaidAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT,
				recordLine1.coinsuranceAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_PRIMARY_PAYER_ALLOWED_CHARGE,
				recordLine1.primaryPayerAllowedChargeAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT,
				recordLine1.submittedChargeAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, recordLine1.allowedChargeAmount,
				eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD,
				DataTransformer.CODING_SYSTEM_CCW_PROCESSING_INDICATOR_CD, recordLine1.processingIndicatorCode.get());

		assertDiagnosisLinkPresent(recordLine1.diagnosis, eob, eobItem0);

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT,
				recordLine1.purchasePriceAmount, eobItem0.getAdjudication());

		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_PRICING_STATE_CD,
				DataTransformer.CODING_SYSTEM_PRICING_STATE_CD, recordLine1.pricingStateCode.get());

		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_SUPPLIER_TYPE_CD,
				DataTransformer.CODING_SYSTEM_SUPPLIER_TYPE_CD, String.valueOf(recordLine1.supplierTypeCode.get()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_MTUS_CD,
				DataTransformer.CODING_SYSTEM_MTUS_CD, String.valueOf(recordLine1.mtusCode.get()));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_MTUS_COUNT,
				DataTransformer.CODING_SYSTEM_MTUS_COUNT, String.valueOf(recordLine1.mtusCount));

		assertExtensionCodingEquals(eobItem0, DataTransformer.CODING_SYSTEM_NDC, DataTransformer.CODING_SYSTEM_NDC,
				recordLine1.nationalDrugCode.get());
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the {@link Identifier#getSystem()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param expectedIdentifierValue
	 *            the {@link Identifier#getValue()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param careTeam
	 *            the {@link List} of {@link CareTeamComponent}s to search
	 * @return the {@link CareTeamComponent} whose
	 *         {@link CareTeamComponent#getProvider()} is an {@link Identifier}
	 *         with the specified provider NPI, or else <code>null</code> if no
	 *         such {@link CareTeamComponent} was found
	 */
	private static CareTeamComponent findCareTeamEntryForProviderIdentifier(String expectedIdentifierSystem,
			String expectedIdentifierValue, List<CareTeamComponent> careTeam) {
		Optional<CareTeamComponent> careTeamEntry = careTeam.stream()
				.filter(ctc -> doesReferenceMatchIdentifier(expectedIdentifierSystem, expectedIdentifierValue,
						ctc.getProvider()))
				.findFirst();
		return careTeamEntry.orElse(null);
	}

	/**
	 * @param expectedProviderNpi
	 *            the {@link Identifier#getValue()} of the provider to find a
	 *            matching {@link CareTeamComponent} for
	 * @param careTeam
	 *            the {@link List} of {@link CareTeamComponent}s to search
	 * @return the {@link CareTeamComponent} whose
	 *         {@link CareTeamComponent#getProvider()} is an {@link Identifier}
	 *         with the specified provider NPI, or else <code>null</code> if no
	 *         such {@link CareTeamComponent} was found
	 */
	private static CareTeamComponent findCareTeamEntryForProviderIdentifier(String expectedProviderNpi,
			List<CareTeamComponent> careTeam) {
		return findCareTeamEntryForProviderIdentifier(DataTransformer.CODING_SYSTEM_NPI_US, expectedProviderNpi,
				careTeam);
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} to match
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} to match
	 * @param actualReference
	 *            the {@link Reference} to check
	 * @return <code>true</code> if the specified {@link Reference} matches the
	 *         expected {@link Identifier}
	 */
	private static boolean doesReferenceMatchIdentifier(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference actualReference) {
		if (!actualReference.hasIdentifier())
			return false;
		return expectedIdentifierSystem.equals(actualReference.getIdentifier().getSystem())
				&& expectedIdentifierValue.equals(actualReference.getIdentifier().getValue());
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} to match
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} to match
	 * @param actualReference
	 *            the {@link Reference} to check
	 */
	private static void assertReferenceEquals(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference actualReference) {
		Assert.assertTrue("Reference doesn't match: " + actualReference,
				doesReferenceMatchIdentifier(expectedIdentifierSystem, expectedIdentifierValue, actualReference));
	}

	/**
	 * @param expected
	 *            the expected {@link LocalDate}
	 * @param actual
	 *            the actual {@link DateTimeType} to verify
	 */
	private static void assertDateEquals(LocalDate expected, DateTimeType actual) {
		Assert.assertEquals(Date.from(expected.atStartOfDay(ZoneId.systemDefault()).toInstant()), actual.getValue());
		Assert.assertEquals(TemporalPrecisionEnum.DAY, actual.getPrecision());
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actualConcept
	 *            the actual {@link CodeableConcept} to verify
	 */
	private static void assertHasCoding(String expectedSystem, String expectedCode, CodeableConcept actualConcept) {
		assertHasCoding(expectedSystem, null, expectedCode, actualConcept);
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedVersion
	 *            the expected {@link Coding#getVersion()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actualConcept
	 *            the actual {@link CodeableConcept} to verify
	 */
	private static void assertHasCoding(String expectedSystem, String expectedVersion, String expectedCode,
			CodeableConcept actualConcept) {
		Assert.assertTrue("No matching Coding found: " + actualConcept.toString(),
				DataTransformer.isCodeInConcept(actualConcept, expectedSystem, expectedVersion, expectedCode));
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actual
	 *            the actual {@link Coding} to verify
	 */
	private static void assertCodingEquals(String expectedSystem, String expectedCode, Coding actual) {
		assertCodingEquals(expectedSystem, null, expectedCode, actual);
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} value
	 * @param expectedVersion
	 *            the expected {@link Coding#getVersion()} value
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()} value
	 * @param actual
	 *            the actual {@link Coding} to verify
	 */
	private static void assertCodingEquals(String expectedSystem, String expectedVersion, String expectedCode,
			Coding actual) {
		Assert.assertEquals(expectedSystem, actual.getSystem());
		Assert.assertEquals(expectedVersion, actual.getVersion());
		Assert.assertEquals(expectedCode, actual.getCode());
	}

	/**
	 * @param fhirElement
	 *            the FHIR element to check the extension of
	 * @param expectedExtensionUrl
	 *            the expected {@link Extension#getUrl()} of the
	 *            {@link Extension} to look for
	 * @param expectedCodingSystem
	 *            the expected {@link Coding#getSystem()}
	 * @param expectedCode
	 *            the expected {@link Coding#getCode()}
	 */
	private static void assertExtensionCodingEquals(IBaseHasExtensions fhirElement, String expectedExtensionUrl,
			String expectedCodingSystem, String expectedCode) {
		assertCodingEquals(expectedCodingSystem, expectedCode,
				fhirElement.getExtension().stream().filter(e -> e.getUrl().equals(expectedExtensionUrl))
						.map(e -> (CodeableConcept) e.getValue()).map(c -> c.getCodingFirstRep()).findAny().get());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to find and verify
	 * @param expectedAmount
	 *            the expected {@link AdjudicationComponent#getAmount()}
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationEquals(String expectedCategoryCode, BigDecimal expectedAmount,
			List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(),
						DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, expectedCategoryCode))
				.findAny();
		Assert.assertTrue(adjudication.isPresent());
		Assert.assertEquals(expectedAmount, adjudication.get().getAmount().getValue());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to find and verify
	 * @param expectedReasonSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link AdjudicationComponent#getReason()} to find and verify
	 * @param expectedReasonCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getReason()} to find and verify
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationReasonEquals(String expectedCategoryCode, String expectedReasonSystem,
			String expectedReasonCode, List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(),
						DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, expectedCategoryCode))
				.findAny();
		Assert.assertTrue(adjudication.isPresent());
		assertHasCoding(expectedReasonSystem, expectedReasonCode, adjudication.get().getReason());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to verify is not
	 *            present
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationNotPresent(String expectedCategoryCode, List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(),
						DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS, expectedCategoryCode))
				.findAny();
		Assert.assertFalse(adjudication.isPresent());
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected {@link BenefitComponent#getBenefitMoney}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */

	private static void assertBenefitBalanceEquals(String expectedFinancialTypeSystem, String expectedFinancialTypeCode,
			BigDecimal expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getType(), expectedFinancialTypeSystem,
						expectedFinancialTypeCode))
				.findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			Assert.assertEquals(expectedAmount, benefitComponent.get().getAllowedMoney().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected
	 *            {@link BenefitComponent#getBenefitUnsignedIntType()}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */

	private static void assertBenefitBalanceEquals(String expectedFinancialTypeSystem, String expectedFinancialTypeCode,
			Integer expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream().filter(a -> DataTransformer
				.isCodeInConcept(a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode)).findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			Assert.assertEquals(expectedAmount, benefitComponent.get().getAllowedUnsignedIntType().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedFinancialTypeSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedFinancialTypeCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link BenefitComponent#getCode)} to find and verify
	 * @param expectedAmount
	 *            the expected
	 *            {@link BenefitComponent#getBenefitUsedUnsignedIntType}
	 * @param actuals
	 *            the actual {@link BenefitComponent}s to verify
	 */

	private static void assertBenefitBalanceUsedEquals(String expectedFinancialTypeSystem,
			String expectedFinancialTypeCode, Integer expectedAmount, List<BenefitComponent> actuals) {
		Optional<BenefitComponent> benefitComponent = actuals.stream().filter(a -> DataTransformer
				.isCodeInConcept(a.getType(), expectedFinancialTypeSystem, expectedFinancialTypeCode)).findFirst();
		Assert.assertTrue(benefitComponent.isPresent());
		try {
			Assert.assertEquals(expectedAmount, benefitComponent.get().getUsedUnsignedIntType().getValue());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedPractitioner
	 *            {@link CareTeamComponent#getProviderIdentifier)} to find and
	 *            verify
	 * @param expectedPractitionerRole
	 *            {@link CareTeamComponent#getRole)} to find and verify
	 * @param eob
	 *            the actual {@link ExplanationOfBenefit}s to verify
	 */

	private static void assertCareTeamEquals(String expectedPractitioner, String expectedPractitionerRole,
			ExplanationOfBenefit eob) {
		CareTeamComponent careTeamEntry = findCareTeamEntryForProviderIdentifier(
				DataTransformer.CODING_SYSTEM_NPI_US, expectedPractitioner, eob.getCareTeam());
		Assert.assertNotNull(careTeamEntry);
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CARE_TEAM_ROLE, expectedPractitionerRole,
				careTeamEntry.getRole().getCodingFirstRep());
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedCode
	 *            the expected {@link Coding#getCoding()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedFromDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTimingPeriod().getStartElement()}
	 * @param expectedThruDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTimingPeriod().getEndElement()}
	 * @param actuals
	 *            the actual {@link SupportingInformationComponent}s to verify
	 */
	private static void assertInformationPeriodEquals(String expectedSystem, String expectedCode,
			LocalDate expectedFromDate,
			LocalDate expectedThruDate, List<SupportingInformationComponent> actuals) {
		Optional<SupportingInformationComponent> supportingInformationComponent = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(), expectedSystem, expectedCode)).findAny();
		Assert.assertTrue(supportingInformationComponent.isPresent());
		try {
			assertDateEquals(expectedFromDate,
					supportingInformationComponent.get().getTimingPeriod().getStartElement());
			assertDateEquals(expectedThruDate, supportingInformationComponent.get().getTimingPeriod().getEndElement());
		} catch (FHIRException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Coding#getSystem()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedCode
	 *            the expected {@link Coding#getCoding()} of the
	 *            {@link SupportingInformationComponent#getCategory()} to find
	 *            and verify
	 * @param expectedDate
	 *            the expected
	 *            {@link SupportingInformationComponent#getTiming().primitiveValue()}
	 * @param actuals
	 *            the actual {@link SupportingInformationComponent}s to verify
	 */
	private static void assertInformationDateEquals(String expectedSystem, String expectedCode, LocalDate expectedDate,
			List<SupportingInformationComponent> actuals) {
		Optional<SupportingInformationComponent> supportingInformationComponent = actuals.stream()
				.filter(a -> DataTransformer.isCodeInConcept(a.getCategory(), expectedSystem, expectedCode)).findAny();
		Assert.assertTrue(supportingInformationComponent.isPresent());
		Assert.assertEquals(expectedDate.toString(), supportingInformationComponent.get().getTiming().primitiveValue());
	}

	/**
	 * @param expectedDiagnosis
	 *            the expected {@link IcdCode} to verify the presence of in the
	 *            {@link ItemComponent}
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to verify
	 * @param eobItem
	 *            the {@link ItemComponent} to verify
	 */
	private static void assertDiagnosisLinkPresent(IcdCode expectedDiagnosis, ExplanationOfBenefit eob,
			ItemComponent eobItem) {
		Optional<DiagnosisComponent> eobDiagnosis = eob.getDiagnosis().stream()
				.filter(d -> d.getDiagnosis() instanceof CodeableConcept)
				.filter(d -> DataTransformer.isCodeInConcept((CodeableConcept) d.getDiagnosis(),
						DataTransformer.computeFhirSystem(expectedDiagnosis), expectedDiagnosis.getCode()))
				.findAny();
		Assert.assertTrue(eobDiagnosis.isPresent());
		Assert.assertTrue(eobItem.getDiagnosisLinkId().stream()
				.filter(l -> eobDiagnosis.get().getSequence() == l.getValue()).findAny().isPresent());
	}

	/**
	 * @param expectedDiagnosis
	 *            the expected {@link IcdCode} to verify the presence of in the
	 *            {@link ItemComponent}
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to verify
	 * @param eobItem
	 *            the {@link ItemComponent} to verify
	 */
	private static void assertDiagnosisLinkPresent(Optional<IcdCode> expectedDiagnosis, ExplanationOfBenefit eob,
			ItemComponent eobItem) {
		if (expectedDiagnosis.isPresent())
			assertDiagnosisLinkPresent(expectedDiagnosis.get(), eob, eobItem);
	}

	/**
	 * @param expectedSystem
	 *            the expected {@link Identifier#getSystem()} value
	 * @param expectedId
	 *            the expected {@link Identifier#getValue()} value
	 * @param actuals
	 *            the actual {@link Identifier} to verify
	 */
	private static void assertIdentifierExists(String expectedSystem, String expectedId, List<Identifier> actuals) {
		Assert.assertTrue(actuals.stream().filter(i -> expectedSystem.equals(i.getSystem()))
				.anyMatch(i -> expectedId.equals(i.getValue())));
	}

	/**
	 * @param expectedIdentifierSystem
	 *            the expected {@link Identifier#getSystem()} value
	 * @param expectedIdentifierValue
	 *            the expected {@link Identifier#getValue()} value
	 * @param reference
	 *            the actual {@link Reference} to verify
	 */
	private static void assertReferenceIdentifierEquals(String expectedIdentifierSystem, String expectedIdentifierValue,
			Reference reference) {
		Assert.assertTrue("Bad reference: " + reference, reference.hasIdentifier());
		Assert.assertEquals(expectedIdentifierSystem, reference.getIdentifier().getSystem());
		Assert.assertEquals(expectedIdentifierValue, reference.getIdentifier().getValue());
	}

	/**
	 * @param eob
	 *            - Verify FHIR resource doesn't contain "Optional" value
	 */

	private static void assertOptionalNotPresent(ExplanationOfBenefit eob) {
		FhirContext ctx = FhirContext.forDstu3();
		String encoded = ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString(eob);
		System.out.println(encoded);
		Assert.assertEquals(false, encoded.contains("Optional"));
	}

	/**
	 * @return a bundle for the Rif record passed in
	 */
	private Bundle getBundle(Object record) {
		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent rifRecordEvent = new RifRecordEvent(filesEvent, file, record);

		Stream source = Arrays.asList(rifRecordEvent).stream();
		DataTransformer transformer = new DataTransformer(FhirTestUtilities.getLoadOptions());
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());

		TransformedBundle bundleWrapper = resultList.get(0);
		Bundle bundle = bundleWrapper.getResult();

		return bundle;
	}

	/**
	 * @return a RifRecordEvent for the sample a test file data
	 */
	private RifRecordEvent getSampleATestData(StaticRifResource resourceType) {
		// Read data from sample-a-* text file
		RifFilesEvent filesRifEvent = new RifFilesEvent(Instant.now(), resourceType.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesRifEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(resourceType.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(resourceType.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());

		return rifRecordEvent;
	}

	/**
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to create an altered copy of
	 * @return a copy of the specified {@link RifRecordEvent}, but with all
	 *         {@link Optional} fields in {@link RifRecordEvent#getRecord()}
	 *         adjusted to have missing values
	 */
	@SuppressWarnings("unchecked")
	private static <R> RifRecordEvent<R> copyWithoutOptionalValues(RifRecordEvent<R> rifRecordEvent) {
		R record = rifRecordEvent.getRecord();
		try {
			R recordCopy = (R) record.getClass().getConstructor().newInstance();

			for (Field field : recordCopy.getClass().getFields()) {
				if (!Optional.class.isAssignableFrom(field.getType()))
					field.set(recordCopy, field.get(record));
				else
					field.set(recordCopy, Optional.empty());
			}

			return new RifRecordEvent<R>(rifRecordEvent.getFilesEvent(), rifRecordEvent.getFile(), recordCopy);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new BadCodeMonkeyException(e);
		}
	}
}
