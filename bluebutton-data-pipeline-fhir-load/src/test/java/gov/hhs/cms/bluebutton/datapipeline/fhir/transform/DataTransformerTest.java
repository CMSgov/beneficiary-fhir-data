package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu3.exceptions.FHIRException;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.StringType;
import org.hl7.fhir.dstu3.model.TemporalPrecisionEnum;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;
import org.junit.Assert;
import org.junit.Test;

import ca.uhn.fhir.context.FhirContext;
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

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed an
	 * empty {@link RifRecordEvent} stream.
	 */
	@Test
	public void transformEmptyRifStream() {
		DataTransformer transformer = new DataTransformer();

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

		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.PUT, beneEntry.getRequest().getMethod());
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, beneEntry.getRequest().getUrl());
		Patient bene = (Patient) beneEntry.getResource();

		FhirContext ctx = FhirContext.forDstu3();
		String encoded = ctx.newXmlParser().encodeResourceToString(bene);
		Assert.assertEquals(false, encoded.contains("Optional"));

		Assert.assertEquals(bene.getId(), "Patient/bene-" + record.beneficiaryId);
		Assert.assertEquals(1, bene.getAddress().size());
		Assert.assertEquals(record.stateCode, bene.getAddress().get(0).getState());
		Assert.assertEquals(record.countyCode, bene.getAddress().get(0).getDistrict());
		Assert.assertEquals(record.postalCode, bene.getAddress().get(0).getPostalCode());
		Assert.assertEquals(Date.valueOf(record.birthDate), bene.getBirthDate());
		Assert.assertEquals("MALE", bene.getGender().toString().trim());
		assertExtensionCodingEquals(bene, DataTransformer.EXTENSION_US_CORE_RACE,
				DataTransformer.CODING_SYSTEM_RACE, "1");
		/*
		 * TODO Further research needs to be done so these unmapped fields are
		 * documented in a JIRA ticket "Finalize fields for Beneficiary"
		 * BENE_ENTLMT_RSN_ORIG, BENE_ENTLMT_RSN_CURR, BENE_ESRD_IND
		 */

		Assert.assertEquals(record.nameGiven, bene.getName().get(0).getGiven().get(0).toString());
		Assert.assertEquals(record.nameMiddleInitial.get().toString(),
				bene.getName().get(0).getGiven().get(1).toString());
		Assert.assertEquals(record.nameSurname, bene.getName().get(0).getFamilyAsSingleString().toString());

		// TODO Need to check the status code for partA
		// and partB (BENE_PTA_TRMNTN_CD & BENE_PTB_TRMNTN_CD) once STU3 is
		// available

		BundleEntryComponent[] coverageEntry = beneBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Coverage).toArray(BundleEntryComponent[]::new);

		Coverage partA = (Coverage) coverageEntry[0].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partA.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_A, partA.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.get(),
				((StringType) partA.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD).get(0)
						.getValue()).getValue());
		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL,
				DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_ORIGINAL, "1");
		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT,
				DataTransformer.CODING_SYSTEM_CCW_MEDICARE_ENTITLEMENT_CURRENT, "1");
		assertExtensionCodingEquals(partA, DataTransformer.CODING_SYSTEM_CCW_ESRD_INDICATOR,
				DataTransformer.CODING_SYSTEM_CCW_ESRD_INDICATOR, "N");

		Coverage partB = (Coverage) coverageEntry[1].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partB.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_B, partB.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.get(),
				((StringType) partB.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD).get(0)
						.getValue()).getValue());

		Coverage partD = (Coverage) coverageEntry[2].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partD.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_D, partD.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.get(),
				((StringType) partD.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD).get(0)
						.getValue()).getValue());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link PartDEventRow} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 * 
	 * @throws FHIRException
	 *             indicates test failure
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_PDE_ID, pdeRecord.partDEventId, eob.getIdentifier());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_CLAIM_TYPE, "pharmacy", eob.getType());
		Assert.assertEquals("Patient/bene-" + pdeRecord.beneficiaryId, eob.getPatientReference().getReference());
		Assert.assertEquals(Date.valueOf(pdeRecord.paymentDate.get()), eob.getPayment().getDate());

		Assert.assertEquals("01", pdeRecord.serviceProviderIdQualiferCode); 
		Assert.assertEquals("01", pdeRecord.prescriberIdQualifierCode);
		
		ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_ACT, "RXDINV", rxItem.getDetail().get(0).getType());

		Assert.assertEquals(Date.valueOf(pdeRecord.prescriptionFillDate), rxItem.getServicedDateType().getValue());

		Assert.assertEquals(pdeRecord.serviceProviderId, eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(pdeRecord.pharmacyTypeCode), eob.getFacilityIdentifier().getValue());

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

		Coverage coverage = (Coverage) eob.getCoverage().getCoverageReference().getResource();

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID, pdeRecord.planContractId,
				coverage.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID,
				pdeRecord.planBenefitPackageId, coverage.getIdentifier());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, coverage.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_D, coverage.getSubPlan());

	}

	/**
	 * Verifies that {@link DataTransformer} correctly sets the code system
	 * value when the compound code equals compounded.
	 * 
	 * @throws FHIRException
	 *             indicates test failure
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
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

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_ACT, "RXCINV",
				rxItem.getDetail().get(0).getType());
	}

	/**
	 * Verifies that the {@link DataTransformer} correctly uses the
	 * {@link DataTransformer#CODED_ADJUDICATION_PART_D_NONCOVERED_SUPPLEMENT
	 * code when the drug coverage status code =
	 * {@link PartDEventRow#DRUG_CVRD_STUS_CD_SUPPLEMENT}
	 * 
	 * @throws FHIRException
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
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
		DataTransformer transformer = new DataTransformer();
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
		 * Bundle should have: 1) EOB, 2) Practitioner (referrer)
		 */
		Assert.assertEquals(2, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		Assert.assertEquals(record.nearLineRecordIdCode.toString(), ((StringType) eob
				.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD).get(0).getValue()).getValue());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		Assert.assertEquals(record.carrierNumber,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentDenialCode,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());

		ReferralRequest referral = (ReferralRequest) eob.getReferralReference().getResource();
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, referral.getPatient().getReference());
		Assert.assertEquals(1, referral.getRecipient().size());
		Assert.assertEquals(claimBundle.getEntry().stream()
				.filter(entryIsPractitionerWithNpi(record.referringPhysicianNpi.get())).findAny().get()
				.getFullUrl(),
				referral.getRecipient().get(0).getReference());
		BundleEntryComponent referrerEntry = claimBundle.getEntry().stream().filter(r -> {
			if (!(r.getResource() instanceof Practitioner))
				return false;
			Practitioner referrer = (Practitioner) r.getResource();
			return referrer.getIdentifier().stream()
					.filter(i -> DataTransformer.CODING_SYSTEM_NPI_US.equals(i.getSystem()))
					.filter(i -> record.referringPhysicianNpi.get().equals(i.getValue())).findAny().isPresent();
		}).findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, referrerEntry.getRequest().getMethod());
		Assert.assertEquals(
				DataTransformer.referencePractitioner(record.referringPhysicianNpi.get()).getReference(),
				referrerEntry.getRequest().getUrl());

		assertExtensionCodingEquals(eob, DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT,
				DataTransformer.CODING_SYSTEM_CCW_PROVIDER_ASSIGNMENT, "A");

		Assert.assertEquals(6, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		Assert.assertEquals(record.clinicalTrialNumber.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_CLINICAL_TRIAL_NUMBER)
						.get(0).getValue()).getValue());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
				.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue()).getValue());

		Assert.assertEquals(recordLine1.performingPhysicianNpi.get(),
				eobItem0.getCareTeam().get(0).getProviderIdentifier().getValue());
		Assert.assertEquals(recordLine1.organizationNpi.get(),
				eobItem0.getCareTeam().get(1).getProviderIdentifier().getValue());

		Assert.assertEquals(recordLine1.providerStateCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD)
						.get(0).getValue()).getValue());

		Assert.assertEquals(recordLine1.providerZipCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_ZIP_CD).get(0)
						.getValue()).getValue());

		Assert.assertEquals(recordLine1.serviceCount, eobItem0.getQuantity().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, recordLine1.cmsServiceTypeCode,
				eobItem0.getCategory());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, recordLine1.placeOfServiceCode,
				eobItem0.getLocationCoding());
		assertExtensionCodingEquals(eobItem0.getLocation(), DataTransformer.CODING_SYSTEM_CCW_PRICING_LOCALITY,
				DataTransformer.CODING_SYSTEM_CCW_PRICING_LOCALITY, "15");

		assertDateEquals(recordLine1.firstExpenseDate, eobItem0.getServicedPeriod().getStartElement());
		assertDateEquals(recordLine1.lastExpenseDate, eobItem0.getServicedPeriod().getEndElement());

		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(0).getCode());
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());
		Assert.assertEquals(recordLine1.betosCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_BETOS).get(0).getValue())
						.getValue());
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
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT,
				recordLine1.submittedChargeAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, recordLine1.allowedChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_PHYSICIAN_ASSISTANT,
				DataTransformer.CODING_SYSTEM_PHYSICIAN_ASSISTANT_ADJUDICATION,
				"" + recordLine1.reducedPaymentPhysicianAsstCode,
				eobItem0.getAdjudication());
		assertAdjudicationReasonEquals(DataTransformer.CODED_ADJUDICATION_LINE_PROCESSING_INDICATOR,
				DataTransformer.CODING_SYSTEM_CMS_LINE_PROCESSING_INDICATOR, recordLine1.processingIndicatorCode,
				eobItem0.getAdjudication());
		assertDiagnosisLinkPresent(recordLine1.diagnosis, eob, eobItem0);

		Assert.assertEquals(recordLine1.nationalDrugCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_NDC).get(0).getValue())
						.getValue());
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
		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		Assert.assertEquals("active", eob.getStatus().toCode());
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertFalse(record.claimNonPaymentReasonCode.isPresent());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream()
				.filter(bb -> bb.getType().getCode()
						.equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL))
				.findFirst().get().getBenefitMoney().getValue()
				);

		Assert.assertEquals(record.deductibleAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_BENEFIT_DEDUCTIBLE_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.passThruPerDiemAmount,
				eob.getBenefitBalanceFirstRep().getFinancial().stream()
						.filter(bb -> bb.getType().getCode()
								.equalsIgnoreCase(DataTransformer.CODING_CLAIM_PASS_THRU_PER_DIEM_AMT))
						.findFirst().get().getBenefitMoney().getValue()
				);

		Assert.assertEquals(record.partACoinsuranceLiabilityAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_BENEFIT_COIN_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.bloodDeductibleLiabilityAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.professionalComponentCharge, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PROFFESIONAL_CHARGE_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.noncoveredCharge, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_INPATIENT_NONCOVERED_CHARGE_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.totalDeductionAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_INPATIENT_TOTAL_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.claimTotalPPSCapitalAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_TOTAL_PPS_CAPITAL_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);
		
		Assert.assertEquals(record.claimPPSCapitalFSPAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial().stream()
						.filter(bb -> bb.getType().getCode()
								.equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_CAPITAL_FEDERAL_PORTION_AMT_URL))
						.findFirst().get().getBenefitMoney().getValue()
				);

		Assert.assertEquals(record.claimPPSCapitalOutlierAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_CAPITAL_OUTLIER_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.claimPPSCapitalDisproportionateShareAmt.get(),
				eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_CAPITAL_DISPROPORTIONAL_SHARE_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.claimPPSCapitalIMEAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_CAPITAL_INDIRECT_MEDICAL_EDU_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.claimPPSCapitalExceptionAmount.get(), eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_CAPITAL_EXCEPTION_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.claimPPSOldCapitalHoldHarmlessAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_PPS_OLD_CAPITAL_HOLD_HARMLESS_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.nchDrugOutlierApprovedPaymentAmount.get(),
				eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_DRUG_OUTLIER_APPROVED_PAYMENT_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(record.claimFacilityTypeCode), eob.getFacilityIdentifier().getValue());

		Assert.assertEquals(record.claimServiceClassificationTypeCode.toString(),
				(eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD).get(0).getValue()
						.toString()));

		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.operatingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.otherPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OTHER_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(9, eob.getDiagnosis().size());

		Assert.assertEquals(record.procedureCodes.get(0).getCode(),
				eob.getProcedure().get(0).getProcedureCoding().getCode());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());
		Assert.assertEquals(record.procedureCodes.get(1).getCode(),
				eob.getProcedure().get(1).getProcedureCoding().getCode());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(1).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(1).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
				.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue()).getValue());

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		Assert.assertEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(), eobItem0.getCareTeamFirstRep().getProviderIdentifier().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());

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
		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		Assert.assertEquals("active", eob.getStatus().toCode());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());

		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.bloodDeductibleLiabilityAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_BENEFIT_BLOOD_DED_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.deductibleAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_BEN_PART_B_DED_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.coninsuranceAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_BEN_PART_B_COINSUR_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.providerPaymentAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_OUTPAT_PROVIDER_PAYMENT_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.beneficiaryPaymentAmount, eob.getBenefitBalanceFirstRep().getFinancial()
				.stream().filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_CLAIM_OUTPAT_BEN__PAYMENT_AMT_URL)).findFirst().get().getBenefitMoney().getValue()  
				);

		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(record.claimFacilityTypeCode), eob.getFacilityIdentifier().getValue());

		Assert.assertEquals(record.claimServiceClassificationTypeCode.toString(),
				(eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD).get(0)
						.getValue()
						.toString()));

		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.operatingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());
		Assert.assertFalse(record.otherPhysicianNpi.isPresent());

		Assert.assertEquals(6, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getProcedure().size());
		Assert.assertEquals(record.procedureCodes.get(0).getCode(),
				eob.getProcedure().get(0).getProcedureCoding().getCode());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());

		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
						.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue())
								.getValue());

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_NDC, recordLine1.nationalDrugCode.get(),
				eobItem0.getService());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_1ST_ANSI_CD, recordLine1.revCntr1stAnsiCd.get(),
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_2ND_ANSI_CD, recordLine1.revCntr2ndAnsiCd.get(),
				eobItem0.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_3RD_ANSI_CD, eobItem0.getAdjudication());
		assertAdjudicationNotPresent(DataTransformer.CODED_ADJUDICATION_4TH_ANSI_CD, eobItem0.getAdjudication());

		Assert.assertEquals(recordLine1.hcpcsCode.get(), eobItem0.getModifier().get(0).getCode());
		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(1).getCode());
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

		Assert.assertEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(), eobItem0.getCareTeamFirstRep().getProviderIdentifier().getValue());

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
		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		Assert.assertEquals("active", eob.getStatus().toCode());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.providerNumber, eob.getProviderIdentifier().getValue());
		Assert.assertEquals(record.claimNonPaymentReasonCode.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());

		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial().stream()
				.filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL))
				.findFirst().get().getBenefitMoney().getValue());

		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(record.claimFacilityTypeCode), eob.getFacilityIdentifier().getValue());

		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.operatingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.otherPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OTHER_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());


		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(5, eob.getDiagnosis().size());
		Assert.assertEquals(record.procedureCodes.get(0).getCode(),
				eob.getProcedure().get(0).getProcedureCoding().getCode());
		Assert.assertEquals(Date
				.from(record.procedureCodes.get(0).getProcedureDate().atStartOfDay(ZoneId.systemDefault()).toInstant()),
				eob.getProcedure().get(0).getDate());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
						.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue())
								.getValue());

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());

		Assert.assertEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(), eobItem0.getCareTeamFirstRep().getProviderIdentifier().getValue());

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
		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional).

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.providerNumber, eob.getProviderIdentifier().getValue());

		Assert.assertEquals(record.claimNonPaymentReasonCode.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());
		
		Assert.assertEquals(String.valueOf(record.patientStatusCd.get()),
				eob.getInformation().stream()
						.filter(bb -> bb.getCategory().getSystem()
								.equalsIgnoreCase(DataTransformer.CODING_SYSTEM_PATIENT_STATUS_CD))
						.findFirst().get().getCategory().getCode());

		Assert.assertEquals(new BigDecimal(record.utilizationDayCount),
				eob.getInformation().stream()
						.filter(bb -> bb.getCategory().getSystem()
								.equalsIgnoreCase(DataTransformer.CODING_SYSTEM_UTILIZATION_DAY_COUNT))
						.findFirst().get().getValueQuantity().getValue());

		Assert.assertEquals(record.beneficiaryDischargeDate.toString(),
				eob.getInformation().stream()
						.filter(bb -> bb.getCategory().getSystem()
								.equalsIgnoreCase(DataTransformer.CODING_SYSTEM_BENEFICIARY_DISCHARGE_DATE))
						.findFirst().get().getTiming().primitiveValue());

		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial().stream()
				.filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL))
				.findFirst().get().getBenefitMoney().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		
		assertDateEquals(record.claimHospiceStartDate,
				((DateTimeType)(eob.getExtensionsByUrl(DataTransformer.CLAIM_HOSPICE_START_DATE)).get(0).getValue())); 
				
		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());
		
		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(record.claimFacilityTypeCode), eob.getFacilityIdentifier().getValue());

		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.claimServiceClassificationTypeCode.toString(),
				(eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD).get(0).getValue()
						.toString()));

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
						.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue())
								.getValue());

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		Assert.assertEquals(recordLine1.revenueCenter, eobItem0.getModifier().get(0).getCode());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(), eobItem0.getService());
		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(1).getCode());
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

		Assert.assertEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(), eobItem0.getCareTeamFirstRep().getProviderIdentifier().getValue());

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
		DataTransformer transformer = new DataTransformer();
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
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());

		Assert.assertEquals(record.providerNumber, eob.getProviderIdentifier().getValue());

		Assert.assertEquals(record.claimNonPaymentReasonCode.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		
		Assert.assertEquals(record.claimServiceClassificationTypeCode.toString(),
				(eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CLAIM_SERVICE_CLASSIFICATION_TYPE_CD).get(0).getValue()
						.toString()));
		
		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial().stream()
				.filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL))
				.findFirst().get().getBenefitMoney().getValue());

		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		Assert.assertEquals(String.valueOf(record.claimFacilityTypeCode), eob.getFacilityIdentifier().getValue());

		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));

		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
						.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue())
								.getValue());

		Assert.assertEquals(record.providerStateCode, eobItem0.getLocationAddress().getState());

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_1ST_ANSI_CD, recordLine1.revCntr1stAnsiCd.get(),
				eobItem0.getAdjudication());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(), eobItem0.getService());
		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(1).getCode());
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());
			
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_RATE_AMOUNT, recordLine1.rateAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount, eobItem0.getAdjudication());
		
		Assert.assertEquals(recordLine1.revenueCenterRenderingPhysicianNPI.get(), eobItem0.getCareTeamFirstRep().getProviderIdentifier().getValue());

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
		DataTransformer transformer = new DataTransformer();
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
		 * Bundle should have: 1) EOB, 2) Practitioner (referrer)
		 */
		Assert.assertEquals(2, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		assertOptionalNotPresent(eob);

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (professional)

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		Assert.assertEquals(record.nearLineRecordIdCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD).get(0).getValue())
						.getValue());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());

		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		Assert.assertEquals(record.carrierNumber,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentDenialCode,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		Assert.assertEquals("active", eob.getStatus().toCode());
		
		Assert.assertEquals(record.primaryPayerPaidAmount, eob.getBenefitBalanceFirstRep().getFinancial().stream()
				.filter(bb -> bb.getType().getCode().equalsIgnoreCase(DataTransformer.CODING_NCH_PRIMARY_PAYER_URL))
				.findFirst().get().getBenefitMoney().getValue());

		Assert.assertEquals(record.clinicalTrialNumber.get(),
				(eob.getExtensionsByUrl(DataTransformer.CLAIM_CLINICAL_TRIAL_NUMBER).get(0).getValue()
						.toString()));
		
		ReferralRequest referral = (ReferralRequest) eob.getReferralReference().getResource();
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, referral.getPatient().getReference());
		Assert.assertEquals(1, referral.getRecipient().size());
		Assert.assertEquals(claimBundle.getEntry().stream()
				.filter(entryIsPractitionerWithNpi(record.referringPhysicianNpi.get())).findAny().get()
				.getFullUrl(),
				referral.getRecipient().get(0).getReference());
		BundleEntryComponent referrerEntry = claimBundle.getEntry().stream().filter(r -> {
			if (!(r.getResource() instanceof Practitioner))
				return false;
			Practitioner referrer = (Practitioner) r.getResource();
			return referrer.getIdentifier().stream()
					.filter(i -> DataTransformer.CODING_SYSTEM_NPI_US.equals(i.getSystem()))
					.filter(i -> record.referringPhysicianNpi.get().equals(i.getValue())).findAny().isPresent();
		}).findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, referrerEntry.getRequest().getMethod());
		Assert.assertEquals(
				DataTransformer.referencePractitioner(record.referringPhysicianNpi.get()).getReference(),
				referrerEntry.getRequest().getUrl());

		Assert.assertEquals(3, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));
		
		Assert.assertEquals(DataTransformer.CODED_EOB_ITEM_TYPE_CLINICAL_SERVICES_AND_PRODUCTS,
				((StringType) eobItem0.getDetail().get(0)
						.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE).get(0).getValue())
								.getValue());

		Assert.assertEquals(recordLine1.providerNPI, eobItem0.getCareTeam().get(0).getProviderIdentifier().getValue());

		Assert.assertEquals(recordLine1.providerStateCode,
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PROVIDER_STATE_CD)
						.get(0).getValue()).getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_TYPE_SERVICE, recordLine1.cmsServiceTypeCode,
				eobItem0.getCategory());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_FHIR_EOB_ITEM_LOCATION, recordLine1.placeOfServiceCode,
				eobItem0.getLocationCoding());

		assertDateEquals(recordLine1.firstExpenseDate, eobItem0.getServicedPeriod().getStartElement());
		assertDateEquals(recordLine1.lastExpenseDate, eobItem0.getServicedPeriod().getEndElement());

		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(0).getCode());
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());
		Assert.assertEquals(recordLine1.betosCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_BETOS).get(0).getValue())
						.getValue());
						
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
		assertDiagnosisLinkPresent(recordLine1.diagnosis, eob, eobItem0);

		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_PURCHASE_PRICE_AMOUNT,
				recordLine1.purchasePriceAmount, eobItem0.getAdjudication());

		Assert.assertEquals(recordLine1.nationalDrugCode.get(),
				((StringType) eobItem0.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_NDC).get(0).getValue())
						.getValue());
	}

	/**
	 * @param npi
	 *            the NPI to verify that the {@link Practitioner} resource in
	 *            the specified {@link BundleEntryComponent} has
	 * @return a {@link Predicate} that will match if the specified
	 *         {@link BundleEntryComponent} has a {@link Practitioner} resource
	 *         with the specified NPI
	 */
	private Predicate<? super BundleEntryComponent> entryIsPractitionerWithNpi(String npi) {
		return e -> {
			// First, check the resource type.
			if (!(e.getResource() instanceof Practitioner))
				return false;
			Practitioner p = (Practitioner) e.getResource();

			// Then, verify that the Practitioner has the expected NPI.
			return p.getIdentifier().stream().filter(i -> DataTransformer.CODING_SYSTEM_NPI_US.equals(i.getSystem()))
					.filter(i -> npi.equals(i.getValue())).findAny().isPresent();
		};
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
	 * @param actual
	 *            the actual {@link Coding} to verify
	 */
	private static void assertCodingEquals(String expectedSystem, String expectedCode, Coding actual) {
		Assert.assertEquals(expectedSystem, actual.getSystem());
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
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
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
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
		Assert.assertTrue(adjudication.isPresent());
		Assert.assertEquals(expectedReasonSystem, adjudication.get().getReason().getSystem());
		Assert.assertEquals(expectedReasonCode, adjudication.get().getReason().getCode());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link AdjudicationComponent#getCategory()} to find and verify
	 * @param expectedCode
	 *            the expected {@link AdjudicationComponent#getReason()}
	 * @param actuals
	 *            the actual {@link AdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationEquals(String expectedCategoryCode, String expectedCode,
			List<AdjudicationComponent> actuals) {
		Optional<AdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
		Assert.assertTrue(adjudication.isPresent());
		Assert.assertEquals(expectedCode, adjudication.get().getReason().getCode());
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
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
		Assert.assertFalse(adjudication.isPresent());
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
				.filter(d -> expectedDiagnosis.getVersion().getFhirSystem().equals(d.getDiagnosis().getSystem()))
				.filter(d -> expectedDiagnosis.getCode().equals(d.getDiagnosis().getCode())).findAny();
		Assert.assertTrue(eobDiagnosis.isPresent());
		Assert.assertTrue(eobItem.getDiagnosisLinkId().stream()
				.filter(l -> eobDiagnosis.get().getSequence() == l.getValue()).findAny().isPresent());
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
	 * @param eob
	 *            - Verify FHIR resource doesn't contain "Optional" value
	 */

	private static void assertOptionalNotPresent(ExplanationOfBenefit eob) {
		FhirContext ctx = FhirContext.forDstu3();
		String encoded = ctx.newXmlParser().encodeResourceToString(eob);
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
		DataTransformer transformer = new DataTransformer();
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
}
