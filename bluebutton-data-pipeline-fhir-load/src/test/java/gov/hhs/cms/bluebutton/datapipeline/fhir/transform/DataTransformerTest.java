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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
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
	private PartDEventRow pdeRecord;

	@Before
	public void setup() {
		// Initialize a default version of the PDE record
		pdeRecord = new PartDEventRow();
		pdeRecord.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		pdeRecord.recordAction = RecordAction.INSERT;
		pdeRecord.partDEventId = "766";
		pdeRecord.beneficiaryId = "94444";
		pdeRecord.prescriptionFillDate = LocalDate.of(2015, 5, 12);
		pdeRecord.paymentDate = Optional.of(LocalDate.of(2015, 5, 27));
		pdeRecord.serviceProviderIdQualiferCode = "01";
		pdeRecord.serviceProviderId = "111222333";
		pdeRecord.prescriberIdQualifierCode = "01";
		pdeRecord.prescriberId = "444555666";
		pdeRecord.prescriptionReferenceNumber = new Long(799999);
		pdeRecord.nationalDrugCode = "987654331212";
		pdeRecord.planContractId = "H9999";
		pdeRecord.planBenefitPackageId = "020";
		pdeRecord.compoundCode = CompoundCode.NOT_COMPOUNDED;
		pdeRecord.dispenseAsWrittenProductSelectionCode = "0";
		pdeRecord.quantityDispensed = new BigDecimal(60);
		pdeRecord.daysSupply = new Integer(30);
		pdeRecord.fillNumber = new Integer(3);
		pdeRecord.dispensingStatusCode = Optional.of(new Character('P'));
		pdeRecord.drugCoverageStatusCode = DrugCoverageStatus.COVERED;
		pdeRecord.adjustmentDeletionCode = Optional.of(new Character('A'));
		pdeRecord.nonstandardFormatCode = Optional.of(new Character('X'));
		pdeRecord.pricingExceptionCode = Optional.of(new Character('M'));
		pdeRecord.catastrophicCoverageCode = Optional.of(new Character('C'));
		pdeRecord.grossCostBelowOutOfPocketThreshold = new BigDecimal("995.34");
		pdeRecord.grossCostAboveOutOfPocketThreshold = new BigDecimal("15.25");
		pdeRecord.patientPaidAmount = new BigDecimal("235.85");
		pdeRecord.otherTrueOutOfPocketPaidAmount = new BigDecimal("17.30");
		pdeRecord.lowIncomeSubsidyPaidAmount = new BigDecimal("122.23");
		pdeRecord.patientLiabilityReductionOtherPaidAmount = new BigDecimal("42.42");
		pdeRecord.partDPlanCoveredPaidAmount = new BigDecimal("126.99");
		pdeRecord.partDPlanNonCoveredPaidAmount = new BigDecimal("17.98");
		pdeRecord.totalPrescriptionCost = new BigDecimal("550.00");
		pdeRecord.prescriptionOriginationCode = Optional.of(new Character('3'));
		pdeRecord.gapDiscountAmount = new BigDecimal("317.22");
		pdeRecord.brandGenericCode = new Character('G');
		pdeRecord.pharmacyTypeCode = "01";
		pdeRecord.patientResidenceCode = "02";
		pdeRecord.submissionClarificationCode = Optional.of("08");
		pdeRecord.serviceProviderIdQualiferCode = "01"; 
		pdeRecord.prescriberIdQualifierCode = "01";
	}

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
    // Read data from file
		RifFilesEvent filesEvent1 = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_BENES.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesEvent1);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_BENES.getRifFileType(), rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof BeneficiaryRow);
    
    BeneficiaryRow record = (BeneficiaryRow) rifRecordEvent.getRecord();
		
		// Create Mock

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent beneRecordEvent = new RifRecordEvent<BeneficiaryRow>(filesEvent, file, record);

		Stream source = Arrays.asList(beneRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle beneBundleWrapper = resultList.get(0);
		Assert.assertNotNull(beneBundleWrapper);
		Assert.assertSame(beneRecordEvent, beneBundleWrapper.getSource());
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
		Assert.assertEquals(bene.getId(), "Patient/bene-" + record.beneficiaryId);
		Assert.assertEquals(1, bene.getAddress().size());
		Assert.assertEquals(record.stateCode, bene.getAddress().get(0).getState());
		Assert.assertEquals(record.countyCode, bene.getAddress().get(0).getDistrict());
		Assert.assertEquals(record.postalCode, bene.getAddress().get(0).getPostalCode());
		Assert.assertEquals(Date.valueOf(record.birthDate), bene.getBirthDate());
		Assert.assertEquals("MALE", bene.getGender().toString().trim());
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
		Bundle pdeBundle = getBundle(pdeRecord);
		/*
		 * Bundle should have: 1) EOB
		 */
		Assert.assertEquals(1, pdeBundle.getEntry().size());

		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());

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

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, pdeRecord.pharmacyTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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
		RifFilesEvent filesRifEvent = new RifFilesEvent(Instant.now(), StaticRifResource.SAMPLE_A_CARRIER.toRifFile());
		RifFilesProcessor processor = new RifFilesProcessor();
		List<Stream<RifRecordEvent<?>>> rifEvents = processor.process(filesRifEvent);

		Assert.assertNotNull(rifEvents);
		Assert.assertEquals(1, rifEvents.size());
		List<RifRecordEvent<?>> rifEventsList = rifEvents.get(0).collect(Collectors.toList());
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRecordCount(), rifEventsList.size());

		RifRecordEvent<?> rifRecordEvent = rifEventsList.get(0);
		Assert.assertEquals(StaticRifResource.SAMPLE_A_CARRIER.getRifFileType(),
				rifRecordEvent.getFile().getFileType());
		Assert.assertNotNull(rifRecordEvent.getRecord());
		Assert.assertTrue(rifRecordEvent.getRecord() instanceof CarrierClaimGroup);
    
    // Verify the claim header.
		CarrierClaimGroup record = (CarrierClaimGroup) rifRecordEvent.getRecord();
		
		// Verify one of the claim lines.
		CarrierClaimLine recordLine1 = record.lines.get(0);
		
		// Creating Mock	
		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent carrierRecordEvent = new RifRecordEvent<CarrierClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(carrierRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle carrierBundleWrapper = resultList.get(0);
		Assert.assertNotNull(carrierBundleWrapper);
		Assert.assertSame(carrierRecordEvent, carrierBundleWrapper.getSource());
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
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_SUBMITTED_CHARGE_AMOUNT,
				recordLine1.submittedChargeAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE, recordLine1.allowedChargeAmount,
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
		// Create the mock bene to test against.
		InpatientClaimGroup record = new InpatientClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "345543";
		record.claimId = "333333222222";
		record.claimTypeCode = "60";
		record.dateFrom = LocalDate.of(2016, 01, 15);
		record.dateThrough = LocalDate.of(2016, 01, 27);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = 'V';
		record.claimServiceClassificationTypeCode = '1';
		record.claimFrequencyCode = '1';
		record.claimPrimaryPayerCode = 'A';
		record.claimNonPaymentReasonCode = Optional.empty();
		record.providerNumber = "7777766666";
		record.providerStateCode = "IA";
		record.paymentAmount = new BigDecimal("7699.48");
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.totalChargeAmount = new BigDecimal("84999.37");
		record.organizationNpi = Optional.of("5555553305");
		record.attendingPhysicianNpi = Optional.of("161999999");
		record.operatingPhysicianNpi = Optional.of("3333444555");
		record.otherPhysicianNpi = Optional.of("161943433");
		record.mcoPaidSw = Optional.of('0');
		record.claimFacilityTypeCode = '1';
		record.passThruPerDiemAmount = new BigDecimal("10.00");
		record.deductibleAmount = new BigDecimal("112.00");
		record.partACoinsuranceLiabilityAmount = new BigDecimal("5.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.professionalComponentCharge = new BigDecimal("4.00");
		record.noncoveredCharge = new BigDecimal("33.00");
		record.totalDeductionAmount = new BigDecimal("14.00");
		record.claimTotalPPSCapitalAmount = Optional.of(new BigDecimal("646.23"));
		record.claimPPSCapitalFSPAmount = Optional.of(new BigDecimal("552.56"));
		record.claimPPSCapitalOutlierAmount = Optional.of(new BigDecimal("0"));
		record.claimPPSCapitalDisproportionateShareAmt = Optional.of(new BigDecimal("25.09"));
		record.claimPPSCapitalIMEAmount = Optional.of(new BigDecimal("68.58"));
		record.claimPPSCapitalExceptionAmount = Optional.of(new BigDecimal("0"));
		record.claimPPSOldCapitalHoldHarmlessAmount = Optional.of(new BigDecimal("0"));
		record.nchDrugOutlierApprovedPaymentAmount = Optional.of(new BigDecimal("23.99"));
		record.diagnosisAdmitting = new IcdCode(IcdVersion.ICD_10, "R4444");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "R5555");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R6666", "Y"));
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "A7777", "N"));
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R8888", "N"));
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "K1234", "N"));
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "I888", "N"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "R7654"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "I9988", "N"));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "0TCDDEE", LocalDate.of(2016, 01, 16)));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "302DDAA", LocalDate.of(2016, 01, 16)));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "302ZZXX", LocalDate.of(2016, 01, 15)));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "5566AAA", LocalDate.of(2016, 10, 17)));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "6677BBB", LocalDate.of(2016, 01, 24)));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "5588DDD", LocalDate.of(2016, 01, 24)));

		InpatientClaimLine recordLine1 = new InpatientClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = Optional.of("M55");
		recordLine1.unitCount = new BigDecimal("111.00");
		recordLine1.rateAmount = new BigDecimal("5.00");
		recordLine1.totalChargeAmount = new BigDecimal("84888.88");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("3699.00");
		recordLine1.nationalDrugCodeQuantity = Optional.of(new Integer(77));
		recordLine1.nationalDrugCodeQualifierCode = Optional.of("GG");
		recordLine1.revenueCenterRenderingPhysicianNPI = Optional.of("345345345");

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent inpatientRecordEvent = new RifRecordEvent<InpatientClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(inpatientRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle inpatientBundleWrapper = resultList.get(0);
		Assert.assertNotNull(inpatientBundleWrapper);
		Assert.assertSame(inpatientRecordEvent, inpatientBundleWrapper.getSource());
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

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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
		// Create the mock bene to test against.
		OutpatientClaimGroup record = new OutpatientClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "444444";
		record.claimId = "1234567890";
		record.claimTypeCode = "40";
		record.dateFrom = LocalDate.of(2011, 01, 24);
		record.dateThrough = LocalDate.of(2011, 01, 24);
		record.patientDischargeStatusCode = Optional.of("01");
		record.nearLineRecordIdCode = 'W';
		record.claimNonPaymentReasonCode = Optional.of("A");
		record.providerNumber = "9999999";
		record.providerStateCode = "KY";
		record.paymentAmount = new BigDecimal("693.11");
		record.totalChargeAmount = new BigDecimal("8888.85");
		record.claimServiceClassificationTypeCode = '3';
		record.claimFrequencyCode = '1';
		record.claimPrimaryPayerCode = 'A';
		record.organizationNpi = Optional.of("1111111111");
		record.attendingPhysicianNpi = Optional.of("2222222222");
		record.operatingPhysicianNpi = Optional.of("3333333333");
		record.otherPhysicianNpi = Optional.empty();
		record.mcoPaidSw = Optional.of('0');
		record.claimFacilityTypeCode = '1';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.professionalComponentCharge = new BigDecimal("66.89");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "R5555");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R8888"));
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "I9999"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "R2222"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "I5050"));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_10, "0AABBZZ", LocalDate.of(2016, 01, 16)));
		record.diagnosesReasonForVisit.add(new IcdCode(IcdVersion.ICD_10, "R1122"));
		record.deductibleAmount = new BigDecimal("112.00");
		record.coninsuranceAmount = new BigDecimal("175.73");
		record.providerPaymentAmount = new BigDecimal("693.92");
		record.beneficiaryPaymentAmount = new BigDecimal("44.00");
		OutpatientClaimLine recordLine1 = new OutpatientClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 25;
		recordLine1.revCntr1stAnsiCd = Optional.of("CO120");
		recordLine1.revCntr2ndAnsiCd = Optional.of("CR121");
		recordLine1.revCntr3rdAnsiCd = Optional.empty();
		recordLine1.revCntr4thAnsiCd = Optional.empty();
		recordLine1.hcpcsCode = Optional.of("M99");
		recordLine1.hcpcsInitialModifierCode = Optional.of("XX");
		recordLine1.hcpcsSecondModifierCode = Optional.empty();
		recordLine1.nationalDrugCode = Optional.of("987654321");
		recordLine1.unitCount = new BigDecimal("111.00");
		recordLine1.rateAmount = new BigDecimal("5.00");
		recordLine1.bloodDeductibleAmount = new BigDecimal("10.45");
		recordLine1.cashDeductibleAmount = new BigDecimal("12.89");
		recordLine1.wageAdjustedCoinsuranceAmount = new BigDecimal("15.23");
		recordLine1.reducedCoinsuranceAmount = new BigDecimal("11.00");
		recordLine1.firstMspPaidAmount = new BigDecimal("55.00");
		recordLine1.secondMspPaidAmount = new BigDecimal("65.00");
		recordLine1.providerPaymentAmount = new BigDecimal("200.00");
		recordLine1.benficiaryPaymentAmount = new BigDecimal("300.00");
		recordLine1.patientResponsibilityAmount = new BigDecimal("500.00");
		recordLine1.paymentAmount = new BigDecimal("5000.00");
		recordLine1.totalChargeAmount = new BigDecimal("9999.85");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("134.00");
		recordLine1.nationalDrugCodeQuantity = Optional.of(new Integer(77));
		recordLine1.nationalDrugCodeQualifierCode = Optional.of("GG");
		recordLine1.revenueCenterRenderingPhysicianNPI = Optional.of("1234AA");

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent OutpatientRecordEvent = new RifRecordEvent<OutpatientClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(OutpatientRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle OutpatientBundleWrapper = resultList.get(0);
		Assert.assertNotNull(OutpatientBundleWrapper);
		Assert.assertSame(OutpatientRecordEvent, OutpatientBundleWrapper.getSource());
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

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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
		// Create the mock bene to test against.
		SNFClaimGroup record = new SNFClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "74744444";
		record.claimId = "777777777";
		record.claimTypeCode = "20";
		record.dateFrom = LocalDate.of(2013, 12, 01);
		record.dateThrough = LocalDate.of(2013, 12, 18);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "2999999";
		record.paymentAmount = new BigDecimal("3333.33");
		record.totalChargeAmount = new BigDecimal("5555.03");
		record.organizationNpi = Optional.of("1111111111");
		record.attendingPhysicianNpi = Optional.of("2222222222");
		record.operatingPhysicianNpi = Optional.of("3333333333");
		record.otherPhysicianNpi = Optional.of("4444444444");
		record.mcoPaidSw = Optional.of('0');
		record.claimFacilityTypeCode = '2';
		record.claimServiceClassificationTypeCode = '1';
		record.claimFrequencyCode = '1';
		record.claimPrimaryPayerCode = 'A';
		record.providerStateCode = "FL";
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.deductibleAmount = new BigDecimal("112.00");
		record.partACoinsuranceLiabilityAmount = new BigDecimal("5.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.noncoveredCharge = new BigDecimal("33.00");
		record.totalDeductionAmount = new BigDecimal("14.00");
		record.diagnosisAdmitting = new IcdCode(IcdVersion.ICD_9, "V11111");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_9, "V22222");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_9, "V33333"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_9, "V44444"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_9, "V55555"));
		record.procedureCodes.add(new IcdCode(IcdVersion.ICD_9, "0TCCCCC", LocalDate.of(2016, 01, 16)));
		SNFClaimLine recordLine1 = new SNFClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = Optional.of("MMM");
		recordLine1.rateAmount = new BigDecimal("5.00");
		recordLine1.totalChargeAmount = new BigDecimal("95.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("88.00");
		recordLine1.revenueCenterRenderingPhysicianNPI = Optional.of("123456789");

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent SNFRecordEvent = new RifRecordEvent<SNFClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(SNFRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle SNFBundleWrapper = resultList.get(0);
		Assert.assertNotNull(SNFBundleWrapper);
		Assert.assertSame(SNFRecordEvent, SNFBundleWrapper.getSource());
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

		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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
		// Create the mock bene to test against.
		HospiceClaimGroup record = new HospiceClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "1111114444";
		record.claimId = "9992223422";
		record.claimTypeCode = "50";
		record.dateFrom = LocalDate.of(2014, 01, 01);
		record.dateThrough = LocalDate.of(2014, 01, 30);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.claimServiceClassificationTypeCode = '1';
		record.claimFrequencyCode = '1';
		record.claimPrimaryPayerCode = 'A';
		record.providerNumber = "12345";
		record.providerStateCode = "AZ";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = Optional.of("999999999");
		record.attendingPhysicianNpi = Optional.of("8888888888");
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_9, "33444");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_9, "55555"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "999888"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "654321"));
		record.claimHospiceStartDate = LocalDate.of(2014, 07, 06);
		HospiceClaimLine recordLine1 = new HospiceClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.rateAmount = new BigDecimal("5.00");
		recordLine1.hcpcsCode = Optional.of("A5C");
		recordLine1.hcpcsInitialModifierCode = Optional.of("Q9999");
		recordLine1.hcpcsSecondModifierCode = Optional.empty();
		recordLine1.providerPaymentAmount = new BigDecimal("29.00");
		recordLine1.benficiaryPaymentAmount = new BigDecimal("28.00");
		recordLine1.paymentAmount = new BigDecimal("26.00");
		recordLine1.totalChargeAmount = new BigDecimal("2555.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("300.00");
		recordLine1.revenueCenterRenderingPhysicianNPI = Optional.of("12ZZZZ");

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent HospiceRecordEvent = new RifRecordEvent<HospiceClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(HospiceRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle HospiceBundleWrapper = resultList.get(0);
		Assert.assertNotNull(HospiceBundleWrapper);
		Assert.assertSame(HospiceRecordEvent, HospiceBundleWrapper.getSource());
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
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional).

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPayment().getAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getTotalCost().getValue());
		
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_CLAIM_TYPE, record.claimTypeCode, eob.getType());
		
		assertDateEquals(record.claimHospiceStartDate,
				((DateTimeType)(eob.getExtensionsByUrl(DataTransformer.CLAIM_HOSPICE_START_DATE)).get(0).getValue())); 
				
		Assert.assertEquals(record.attendingPhysicianNpi.get(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());
		
		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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

		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(0).getCode());
		Assert.assertFalse(recordLine1.hcpcsSecondModifierCode.isPresent());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode.get(),
				eobItem0.getService());

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
		// Create the mock bene to test against.
		HHAClaimGroup record = new HHAClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "140";
		record.claimId = "2925555555";
		record.claimTypeCode = "10";
		record.dateFrom = LocalDate.of(2015, 06, 23);
		record.dateThrough = LocalDate.of(2015, 06, 23);
		record.patientDischargeStatusCode = "30";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("188.00");
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.providerStateCode = "UT";
		record.organizationNpi = Optional.of("1811111111");
		record.attendingPhysicianNpi = Optional.of("2222222222");
		record.claimFacilityTypeCode = '3';
		record.claimServiceClassificationTypeCode = '2';
		record.claimFrequencyCode = '1';
		record.claimPrimaryPayerCode = 'A';
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_9, "9999");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_9, "8888"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "7777"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "6666"));
		HHAClaimLine recordLine1 = new HHAClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.revCntr1stAnsiCd = Optional.of("CO120");
		recordLine1.rateAmount = new BigDecimal("5.00");
		recordLine1.hcpcsCode = Optional.of("2GGGG");
		recordLine1.hcpcsInitialModifierCode = Optional.of("KO");
		recordLine1.hcpcsSecondModifierCode = Optional.empty();
		recordLine1.paymentAmount = new BigDecimal("26.00");
		recordLine1.totalChargeAmount = new BigDecimal("25.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("24.00");
		recordLine1.revenueCenterRenderingPhysicianNPI = Optional.of("1234BBB");

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent HHARecordEvent = new RifRecordEvent<HHAClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(HHARecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle HHABundleWrapper = resultList.get(0);
		Assert.assertNotNull(HHABundleWrapper);
		Assert.assertSame(HHARecordEvent, HHABundleWrapper.getSource());
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
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatientReference().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
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
		
		Assert.assertEquals(record.organizationNpi.get(), eob.getOrganizationIdentifier().getValue());

		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				eob.castToCoding(eob.getFacility()));

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
		Assert.assertEquals(recordLine1.hcpcsInitialModifierCode.get(), eobItem0.getModifier().get(0).getCode());
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
		// Create the mock bene to test against.
		DMEClaimGroup record = new DMEClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "666666";
		record.claimId = "2188888888";
		record.claimTypeCode = "82";
		record.dateFrom = LocalDate.of(2014, 02, 03);
		record.dateThrough = LocalDate.of(2014, 02, 03);
		record.nearLineRecordIdCode = '1';
		record.claimDispositionCode = "01";
		record.carrierNumber = "99999";
		record.paymentDenialCode = "1";
		record.paymentAmount = new BigDecimal("777.75");
		record.referringPhysicianNpi = Optional.of("222333222");
		record.providerPaymentAmount = new BigDecimal("666.75");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "222333");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "444555"));
		record.clinicalTrialNumber = Optional.of("0");
		DMEClaimLine recordLine1 = new DMEClaimLine();
		record.lines.add(recordLine1);
		recordLine1.number = 1;
		recordLine1.cmsServiceTypeCode = "P";
		recordLine1.hcpcsCode = Optional.of("345");
		recordLine1.hcpcsInitialModifierCode = Optional.of("YY");
		recordLine1.hcpcsSecondModifierCode = Optional.empty();
		recordLine1.hcpcsThirdModifierCode = Optional.empty();
		recordLine1.hcpcsFourthModifierCode = Optional.empty();
		recordLine1.betosCode = Optional.of("D9Z");
		recordLine1.paymentAmount = new BigDecimal("123.45");
		recordLine1.beneficiaryPaymentAmount = new BigDecimal("11.00");
		recordLine1.providerPaymentAmount = new BigDecimal("120.20");
		recordLine1.beneficiaryPartBDeductAmount = new BigDecimal("18.00");
		recordLine1.primaryPayerPaidAmount = new BigDecimal("11.00");
		recordLine1.coinsuranceAmount = new BigDecimal("20.20");
		recordLine1.primaryPayerAllowedChargeAmount = new BigDecimal("20.29");
		recordLine1.submittedChargeAmount = new BigDecimal("130.45");
		recordLine1.allowedChargeAmount = new BigDecimal("129.45");
		recordLine1.diagnosis = new IcdCode(IcdVersion.ICD_10, "G6666");
		recordLine1.purchasePriceAmount = new BigDecimal("82.29");
		recordLine1.placeOfServiceCode = "12";
		recordLine1.providerNPI = "1244444444";
		recordLine1.providerStateCode = "MO";
		recordLine1.firstExpenseDate = LocalDate.of(2014, 02, 03);
		recordLine1.lastExpenseDate = LocalDate.of(2014, 02, 03);;

		recordLine1.nationalDrugCode = Optional.of(new String("55555009902"));

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent carrierRecordEvent = new RifRecordEvent<DMEClaimGroup>(filesEvent, file, record);

		Stream source = Arrays.asList(carrierRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle carrierBundleWrapper = resultList.get(0);
		Assert.assertNotNull(carrierBundleWrapper);
		Assert.assertSame(carrierRecordEvent, carrierBundleWrapper.getSource());
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
}
