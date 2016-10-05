package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.exceptions.FHIRException;
import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.DateTimeType;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemAdjudicationComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemsComponent;
import org.hl7.fhir.dstu21.model.Identifier;
import org.hl7.fhir.dstu21.model.Medication;
import org.hl7.fhir.dstu21.model.MedicationOrder;
import org.hl7.fhir.dstu21.model.MedicationOrder.MedicationOrderDispenseRequestComponent;
import org.hl7.fhir.dstu21.model.Organization;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Practitioner;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.dstu21.model.ReferralRequest;
import org.hl7.fhir.dstu21.model.StringType;
import org.hl7.fhir.dstu21.model.TemporalPrecisionEnum;
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
		pdeRecord.partDEventId = "89";
		pdeRecord.beneficiaryId = "103";
		pdeRecord.prescriptionFillDate = LocalDate.of(2015, 6, 12);
		pdeRecord.paymentDate = Optional.of(LocalDate.of(2015, 6, 27));
		pdeRecord.serviceProviderIdQualiferCode = "01";
		pdeRecord.serviceProviderId = "1124137542";
		pdeRecord.prescriberIdQualifierCode = "01";
		pdeRecord.prescriberId = "1225061591";
		pdeRecord.prescriptionReferenceNumber = new Long(791569);
		pdeRecord.nationalDrugCode = "49884009902";
		pdeRecord.planContractId = "H8552";
		pdeRecord.planBenefitPackageId = "020";
		pdeRecord.compoundCode = CompoundCode.NOT_COMPOUNDED;
		pdeRecord.dispenseAsWrittenProductSelectionCode = "0";
		pdeRecord.quantityDispensed = new BigDecimal(60);
		pdeRecord.daysSupply = new Integer(30);
		pdeRecord.fillNumber = new Integer(3);
		pdeRecord.dispensingStatuscode = Optional.of(new Character('P'));
		pdeRecord.drugCoverageStatusCode = DrugCoverageStatus.COVERED;
		pdeRecord.adjustmentDeletionCode = Optional.of(new Character('A'));
		pdeRecord.nonstandardFormatCode = Optional.of(new Character('X'));
		pdeRecord.pricingExceptionCode = Optional.of(new Character('M'));
		pdeRecord.catastrophicCoverageCode = Optional.of(new Character('C'));
		pdeRecord.grossCostBelowOutOfPocketThreshold = new BigDecimal("362.84");
		pdeRecord.grossCostAboveOutOfPocketThreshold = new BigDecimal("15.25");
		pdeRecord.patientPaidAmount = new BigDecimal("235.85");
		pdeRecord.otherTrueOutOfPocketPaidAmount = new BigDecimal("17.30");
		pdeRecord.lowIncomeSubsidyPaidAmount = new BigDecimal("122.23");
		pdeRecord.patientLiabilityReductionOtherPaidAmount = new BigDecimal("42.42");
		pdeRecord.partDPlanCoveredPaidAmount = new BigDecimal("126.99");
		pdeRecord.partDPlanNonCoveredPaidAmount = new BigDecimal("17.98");
		pdeRecord.totalPrescriptionCost = new BigDecimal("362.84");
		pdeRecord.prescriptionOriginationCode = Optional.of(new Character('3'));
		pdeRecord.gapDiscountAmount = new BigDecimal("317.22");
		/*
		 * TODO re-enable once determined if optional or not.
		 */
		// partDEventRecord.brandGenericCode = new Character('G');
		pdeRecord.pharmacyTypeCode = "01";
		pdeRecord.patientResidenceCode = "02";
		pdeRecord.submissionClarificationCode = Optional.of("08");
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
		// Create the mock bene to test against.
		BeneficiaryRow record = new BeneficiaryRow();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.stateCode = "HI";
		record.countyCode = "Transylvania";
		record.postalCode = "12345";
		record.birthDate = LocalDate.of(1959, Month.MARCH, 17);
		record.sex = ('M');
		record.entitlementCodeOriginal = Optional.of(new Character('1'));
		record.entitlementCodeCurrent = Optional.of(new Character('1'));
		record.endStageRenalDiseaseCode = Optional.of(new Character('N'));
		record.medicareEnrollmentStatusCode = Optional.of(new String("20"));
		record.nameSurname = "Doe";
		record.nameGiven = "John";
		record.nameMiddleInitial = Optional.of(new Character('E'));

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
		 * Bundle should have: 1) Patient, 2) Organization, 3) Coverage (part
		 * A), 4) Coverage (part B), 5) Coverage (part D).
		 */
		Bundle beneBundle = beneBundleWrapper.getResult();
		Assert.assertEquals(5, beneBundle.getEntry().size());
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
		Assert.assertEquals(record.nameMiddleInitial.toString(), bene.getName().get(0).getGiven().get(1).toString());
		Assert.assertEquals(record.nameSurname, bene.getName().get(0).getFamilyAsSingleString().toString());

		// TODO Need to check the status code for partA
		// and partB (BENE_PTA_TRMNTN_CD & BENE_PTB_TRMNTN_CD) once STU3 is
		// available

		BundleEntryComponent[] coverageEntry = beneBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Coverage).toArray(BundleEntryComponent[]::new);

		Coverage partA = (Coverage) coverageEntry[0].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partA.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_A, partA.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.toString(),
				((StringType) partA.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD).get(0)
						.getValue()).getValue());

		Coverage partB = (Coverage) coverageEntry[1].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partB.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_B, partB.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.toString(),
				((StringType) partB.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_BENE_MDCR_STATUS_CD).get(0)
						.getValue()).getValue());

		Coverage partD = (Coverage) coverageEntry[2].getResource();
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN, partD.getPlan());
		Assert.assertEquals(DataTransformer.COVERAGE_PLAN_PART_D, partD.getSubPlan());
		Assert.assertEquals(record.medicareEnrollmentStatusCode.toString(),
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
		 * Bundle should have: 1) EOB, 2) Practitioner (prescriber), 3)
		 * Medication, 4) Organization (serviceProviderOrg), 5) Coverage.
		 */
		Assert.assertEquals(5, pdeBundle.getEntry().size());

		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());

		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_PDE_ID, pdeRecord.partDEventId, eob.getIdentifier());
		// TODO verify eob.type once STU3 available
		Assert.assertEquals("Patient/bene-" + pdeRecord.beneficiaryId, eob.getPatient().getReference());
		Assert.assertEquals(Date.valueOf(pdeRecord.paymentDate.get()), eob.getPaymentDate());

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();
		// Default case has compound code = not a compound
		Assert.assertEquals("RXDINV", rxItem.getType().getCode());
		Assert.assertEquals(Date.valueOf(pdeRecord.prescriptionFillDate), rxItem.getServicedDateType().getValue());

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

		BundleEntryComponent prescriberEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Practitioner).findAny().get();
		Practitioner prescriber = (Practitioner) prescriberEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, pdeRecord.prescriberId,
				prescriber.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, prescriberEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(pdeRecord.prescriberId).getReference(),
				prescriberEntry.getRequest().getUrl());

		BundleEntryComponent medicationEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Medication).findAny().get();
		Medication medication = (Medication) medicationEntry.getResource();
		assertCodingEquals(DataTransformer.CODING_SYSTEM_NDC, pdeRecord.nationalDrugCode,
				medication.getCode().getCoding().get(0));
		Assert.assertEquals(HTTPVerb.PUT, medicationEntry.getRequest().getMethod());
		Assert.assertEquals("Medication/ndc-" + pdeRecord.nationalDrugCode, medicationEntry.getRequest().getUrl());

		MedicationOrder medicationOrder = (MedicationOrder) eob.getPrescription().getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_RX_SRVC_RFRNC_NUM,
				String.valueOf(pdeRecord.prescriptionReferenceNumber), medicationOrder.getIdentifier());
		Assert.assertEquals("Patient/bene-" + pdeRecord.beneficiaryId, medicationOrder.getPatient().getReference());
		Assert.assertEquals(DataTransformer.referencePractitioner(pdeRecord.prescriberId).getReference(),
				medicationOrder.getPrescriber().getReference());
		Assert.assertEquals("Medication/ndc-" + pdeRecord.nationalDrugCode,
				medicationOrder.getMedicationReference().getReference());
		MedicationOrderDispenseRequestComponent dispenseRequest = medicationOrder.getDispenseRequest();
		Assert.assertEquals(pdeRecord.quantityDispensed, dispenseRequest.getQuantity().getValue());
		Assert.assertEquals("days", dispenseRequest.getExpectedSupplyDuration().getUnit());
		Assert.assertEquals(new BigDecimal(pdeRecord.daysSupply),
				dispenseRequest.getExpectedSupplyDuration().getValue());
		/*
		 * TODO verify substitution.allowed and substitution.reason once STU3
		 * structures are available
		 */

		BundleEntryComponent organizationEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, pdeRecord.serviceProviderId,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(pdeRecord.serviceProviderId).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_PHRMCY_SRVC_TYPE_CD, pdeRecord.pharmacyTypeCode,
				organization.getType().getCoding().get(0));

		BundleEntryComponent coverageEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Coverage).findAny().get();
		Coverage coverage = (Coverage) coverageEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID, pdeRecord.planContractId,
				coverage.getIdentifier());
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_PDE_PLAN_BENEFIT_PACKAGE_ID,
				pdeRecord.planBenefitPackageId, coverage.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, coverageEntry.getRequest().getMethod());
		Assert.assertEquals(
				new Reference(String.format("Coverage?identifier=%s|%s",
						DataTransformer.CODING_SYSTEM_PDE_PLAN_CONTRACT_ID, pdeRecord.planContractId)).getReference(),
				coverageEntry.getRequest().getUrl());
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

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();
		Assert.assertEquals("RXCINV", rxItem.getType().getCode());
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

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

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

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();

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
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertCarrierClaimEvent() {
		// Create the mock bene to test against.
		CarrierClaimGroup record = new CarrierClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.nearLineRecordIdCode = '1';
		record.claimDispositionCode = "01";
		record.carrierNumber = "06102";
		record.paymentDenialCode = "1";
		record.paymentAmount = new BigDecimal("130.32");
		record.referringPhysicianNpi = "1265415426";
		record.providerPaymentAmount = new BigDecimal("123.45");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3"));
		CarrierClaimLine recordLine1 = new CarrierClaimLine();
		record.lines.add(recordLine1);
		recordLine1.number = 1;
		recordLine1.organizationNpi = Optional.of("1487872263");
		recordLine1.cmsServiceTypeCode = "90853-HE";
		recordLine1.betosCode = "M5C";
		recordLine1.paymentAmount = new BigDecimal("123.45");
		recordLine1.beneficiaryPaymentAmount = new BigDecimal("0");
		recordLine1.providerPaymentAmount = new BigDecimal("120.20");
		recordLine1.beneficiaryPartBDeductAmount = new BigDecimal("18.00");
		recordLine1.primaryPayerPaidAmount = new BigDecimal("11.00");
		recordLine1.coinsuranceAmount = new BigDecimal("20.20");
		recordLine1.submittedChargeAmount = new BigDecimal("130.45");
		recordLine1.allowedChargeAmount = new BigDecimal("129.45");
		recordLine1.diagnosis = new IcdCode(IcdVersion.ICD_10, "F63.2");
		recordLine1.nationalDrugCode = Optional.of(new String("49884009902"));

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
		 * Bundle should have: 1) EOB, 2) Practitioner (referrer) 3) Medication.
		 */
		Assert.assertEquals(3, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (professional)

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		Assert.assertEquals(record.nearLineRecordIdCode.toString(), ((StringType) eob
				.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD).get(0).getValue()).getValue());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		Assert.assertEquals(record.carrierNumber.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentDenialCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, referral.getPatient().getReference());
		Assert.assertEquals(1, referral.getRecipient().size());
		Assert.assertEquals(claimBundle.getEntry().stream()
				.filter(entryIsPractitionerWithNpi(record.referringPhysicianNpi)).findAny().get().getFullUrl(),
				referral.getRecipient().get(0).getReference());
		BundleEntryComponent referrerEntry = claimBundle.getEntry().stream().filter(r -> {
			if (!(r.getResource() instanceof Practitioner))
				return false;
			Practitioner referrer = (Practitioner) r.getResource();
			return referrer.getIdentifier().stream()
					.filter(i -> DataTransformer.CODING_SYSTEM_NPI_US.equals(i.getSystem()))
					.filter(i -> record.referringPhysicianNpi.equals(i.getValue())).findAny().isPresent();
		}).findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, referrerEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.referringPhysicianNpi).getReference(),
				referrerEntry.getRequest().getUrl());
		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(2, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam
		 * entries.organizationNpi, performingPhysicianNpi,
		 * providerTypeCode,providerSpecialityCode,
		 * providerParticipatingIndCode, providerStateCode,providerZipCode
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		Assert.assertEquals(recordLine1.betosCode,
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

		BundleEntryComponent medicationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Medication).findAny().get();
		Medication medication = (Medication) medicationEntry.getResource();
		assertCodingEquals(DataTransformer.CODING_SYSTEM_NDC, recordLine1.nationalDrugCode.toString(),
				medication.getCode().getCoding().get(0));
		Assert.assertEquals(HTTPVerb.PUT, medicationEntry.getRequest().getMethod());
		Assert.assertEquals("Medication/ndc-" + recordLine1.nationalDrugCode, medicationEntry.getRequest().getUrl());

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link InpatientClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertInpatientClaimEvent() {
		// Create the mock bene to test against.
		InpatientClaimGroup record = new InpatientClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = "1487872263";
		record.attendingPhysicianNpi = "1265415426";
		record.operatingPhysicianNpi = "1265415999";
		record.otherPhysicianNpi = "1265415888";
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.passThruPerDiemAmount = new BigDecimal("10.00");
		record.deductibleAmount = new BigDecimal("112.00");
		record.partACoinsuranceLiabilityAmount = new BigDecimal("5.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.professionalComponentCharge = new BigDecimal("4.00");
		record.noncoveredCharge = new BigDecimal("33.00");
		record.totalDeductionAmount = new BigDecimal("14.00");
		record.diagnosisAdmitting = new IcdCode(IcdVersion.ICD_10, "F99.2");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3", "Y"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "F22.2"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "R11.3", "N"));
		InpatientClaimLine recordLine1 = new InpatientClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = "M5C";

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
		 * Bundle should have: 1) EOB, 2) Organization, 3) Practitioner
		 * (attending physician) 4) Practitioner (Operating Physician), 5)
		 * Practitioner (Other physician)
		 */
		Assert.assertEquals(5, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional)

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getClaimTotal().getValue());

		BundleEntryComponent organizationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(record.organizationNpi).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				organization.getType().getCoding().get(0));

		BundleEntryComponent[] physicianEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Practitioner).toArray(BundleEntryComponent[]::new);
		Practitioner attendingPhysician = (Practitioner) physicianEntry[0].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.attendingPhysicianNpi,
				attendingPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[0].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.attendingPhysicianNpi).getReference(),
				physicianEntry[0].getRequest().getUrl());

		Practitioner operatingPhysician = (Practitioner) physicianEntry[1].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.operatingPhysicianNpi,
				operatingPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[1].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.operatingPhysicianNpi).getReference(),
				physicianEntry[1].getRequest().getUrl());

		Practitioner otherPhysician = (Practitioner) physicianEntry[2].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.otherPhysicianNpi,
				otherPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[2].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.otherPhysicianNpi).getReference(),
				physicianEntry[2].getRequest().getUrl());
		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(5, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam for rendering
		 * physician npi
		 */
		 /*
		  * TODO Once STU3 is available, verify eob.item.category.
		  */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PRIMARY_PAYER_PAID_AMOUNT,
				record.primaryPayerPaidAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PASS_THRU_PER_DIEM_AMOUNT,
				record.passThruPerDiemAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_DEDUCTIBLE,
				record.deductibleAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT,
				record.partACoinsuranceLiabilityAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BLOOD_DEDUCTIBLE,
				record.bloodDeductibleLiabilityAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PROFESSIONAL_COMP_CHARGE,
				record.professionalComponentCharge, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE, record.noncoveredCharge,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_DEDUCTION_AMOUNT, record.totalDeductionAmount,
				eobItem0.getAdjudication());

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link OutpatientClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertOutpatientClaimEvent() {
		// Create the mock bene to test against.
		OutpatientClaimGroup record = new OutpatientClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = "1487872263";
		record.attendingPhysicianNpi = "1265415426";
		record.operatingPhysicianNpi = "1265415999";
		record.otherPhysicianNpi = Optional.of("1265415888");
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.deductibleAmount = new BigDecimal("112.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.professionalComponentCharge = new BigDecimal("4.00");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "F22.2"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "R11.3"));
		record.diagnosesReasonForVisit.add(new IcdCode(IcdVersion.ICD_10, "R079"));
		OutpatientClaimLine recordLine1 = new OutpatientClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = "M5C";
		recordLine1.bloodDeductibleAmount = new BigDecimal("33.00");
		recordLine1.cashDeductibleAmount = new BigDecimal("32.00");
		recordLine1.wageAdjustedCoinsuranceAmount = new BigDecimal("31.00");
		recordLine1.reducedCoinsuranceAmount = new BigDecimal("30.00");
		recordLine1.providerPaymentAmount = new BigDecimal("29.00");
		recordLine1.benficiaryPaymentAmount = new BigDecimal("28.00");
		recordLine1.patientResponsibilityAmount = new BigDecimal("27.00");
		recordLine1.paymentAmount = new BigDecimal("26.00");
		recordLine1.totalChargeAmount = new BigDecimal("25.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("24.00");

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
		 * Bundle should have: 1) EOB, 2) Organization, 3) Practitioner
		 * (attending physician) 4) Practitioner (Operating Physician), 5)
		 * Practitioner (Other physician)
		 */

		Assert.assertEquals(5, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional).

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getClaimTotal().getValue());

		BundleEntryComponent organizationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(record.organizationNpi).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				organization.getType().getCoding().get(0));

		BundleEntryComponent[] physicianEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Practitioner).toArray(BundleEntryComponent[]::new);
		Practitioner attendingPhysician = (Practitioner) physicianEntry[0].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.attendingPhysicianNpi,
				attendingPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[0].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.attendingPhysicianNpi).getReference(),
				physicianEntry[0].getRequest().getUrl());

		Practitioner operatingPhysician = (Practitioner) physicianEntry[1].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.operatingPhysicianNpi,
				operatingPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[1].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.operatingPhysicianNpi).getReference(),
				physicianEntry[1].getRequest().getUrl());

		Practitioner otherPhysician = (Practitioner) physicianEntry[2].getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.otherPhysicianNpi.toString(),
				otherPhysician.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, physicianEntry[2].getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.otherPhysicianNpi.toString()).getReference(),
				physicianEntry[2].getRequest().getUrl());
		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(5, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam for rendering
		 * physician npi
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */
		
		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_BLOOD_DEDUCTIBLE,
				recordLine1.bloodDeductibleAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_CASH_DEDUCTIBLE,
				recordLine1.cashDeductibleAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_WAGE_ADJ_COINSURANCE_AMOUNT,
				recordLine1.wageAdjustedCoinsuranceAmount, eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_REDUCED_COINSURANCE_AMOUNT,
				recordLine1.reducedCoinsuranceAmount, eobItem0.getAdjudication());
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

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link SNFClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertSNFClaimEvent() {
		// Create the mock bene to test against.
		SNFClaimGroup record = new SNFClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "9302293110";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = "1487872263";
		record.attendingPhysicianNpi = "1265415426";
		record.operatingPhysicianNpi = "1265415999";
		record.otherPhysicianNpi = "1265415888";
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.deductibleAmount = new BigDecimal("112.00");
		record.partACoinsuranceLiabilityAmount = new BigDecimal("5.00");
		record.bloodDeductibleLiabilityAmount = new BigDecimal("6.00");
		record.noncoveredCharge = new BigDecimal("33.00");
		record.totalDeductionAmount = new BigDecimal("14.00");
		record.diagnosisAdmitting = new IcdCode(IcdVersion.ICD_10, "F99.2");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3", "Y"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "F22.2"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "R11.3", "N"));
		SNFClaimLine recordLine1 = new SNFClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = "M5C";
		recordLine1.totalChargeAmount = new BigDecimal("95.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("88.00");

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
		 * Bundle should have: 1) EOB, 2) Organization
		 */
		Assert.assertEquals(2, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional)

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getClaimTotal().getValue());

		BundleEntryComponent organizationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(record.organizationNpi).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				organization.getType().getCoding().get(0));

		Assert.assertEquals(record.attendingPhysicianNpi,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.operatingPhysicianNpi,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OPERATING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(record.otherPhysicianNpi,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_OTHER_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(5, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam for rendering
		 * physician npi
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link HospiceClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertHospiceClaimEvent() {
		// Create the mock bene to test against.
		HospiceClaimGroup record = new HospiceClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = "1487872263";
		record.attendingPhysicianNpi = "1265415426";
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "F22.2"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "R11.3"));
		HospiceClaimLine recordLine1 = new HospiceClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = "M5C";
		recordLine1.providerPaymentAmount = new BigDecimal("29.00");
		recordLine1.benficiaryPaymentAmount = new BigDecimal("28.00");
		recordLine1.paymentAmount = new BigDecimal("26.00");
		recordLine1.totalChargeAmount = new BigDecimal("25.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("24.00");

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
		 * Bundle should have: 1) EOB, 2) Organization
		 */
		Assert.assertEquals(2, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional).

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getClaimTotal().getValue());

		BundleEntryComponent organizationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(record.organizationNpi).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				organization.getType().getCoding().get(0));

		Assert.assertEquals(record.attendingPhysicianNpi,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam for rendering
		 * physician npi
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
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

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link HHAClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertHHAClaimEvent() {
		// Create the mock bene to test against.
		HHAClaimGroup record = new HHAClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1988, 01, 24);
		record.dateThrough = LocalDate.of(1990, 01, 01);
		record.patientDischargeStatusCode = "01";
		record.nearLineRecordIdCode = '1';
		record.claimNonPaymentReasonCode = Optional.of("1");
		record.providerNumber = "45645";
		record.paymentAmount = new BigDecimal("130.32");
		record.totalChargeAmount = new BigDecimal("199.99");
		record.organizationNpi = "1487872263";
		record.attendingPhysicianNpi = "1265415426";
		record.claimFacilityTypeCode = '2';
		record.primaryPayerPaidAmount = new BigDecimal("11.00");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3"));
		record.diagnosisFirstClaimExternal = Optional.of(new IcdCode(IcdVersion.ICD_10, "F22.2"));
		record.diagnosesExternal.add(new IcdCode(IcdVersion.ICD_10, "R11.3"));
		HHAClaimLine recordLine1 = new HHAClaimLine();
		record.lines.add(recordLine1);
		recordLine1.lineNumber = 1;
		recordLine1.hcpcsCode = "M5C";
		recordLine1.paymentAmount = new BigDecimal("26.00");
		recordLine1.totalChargeAmount = new BigDecimal("25.00");
		recordLine1.nonCoveredChargeAmount = new BigDecimal("24.00");

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
		 * Bundle should have: 1) EOB, 2) Organization
		 */
		Assert.assertEquals(2, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (institutional).

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(record.claimNonPaymentReasonCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_INP_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());
		Assert.assertEquals(record.totalChargeAmount, eob.getClaimTotal().getValue());

		BundleEntryComponent organizationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Organization).findAny().get();
		Organization organization = (Organization) organizationEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_NPI_US, record.organizationNpi,
				organization.getIdentifier());
		Assert.assertEquals(HTTPVerb.PUT, organizationEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referenceOrganizationByNpi(record.organizationNpi).getReference(),
				organizationEntry.getRequest().getUrl());
		assertCodingEquals(DataTransformer.CODING_SYSTEM_CCW_FACILITY_TYPE_CD, record.claimFacilityTypeCode.toString(),
				organization.getType().getCoding().get(0));

		Assert.assertEquals(record.attendingPhysicianNpi,
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_ATTENDING_PHYSICIAN_NPI).get(0)
						.getValue()).getValue());

		Assert.assertEquals(4, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.lineNumber), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam for rendering
		 * physician npi
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */

		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_PAYMENT, recordLine1.paymentAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_TOTAL_CHARGE_AMOUNT, recordLine1.totalChargeAmount,
				eobItem0.getAdjudication());
		assertAdjudicationEquals(DataTransformer.CODED_ADJUDICATION_NONCOVERED_CHARGE,
				recordLine1.nonCoveredChargeAmount, eobItem0.getAdjudication());

	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * single {@link DMEClaimGroup} {@link RecordAction#INSERT}
	 * {@link RifRecordEvent}.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertDMEClaimEvent() {
		// Create the mock bene to test against.
		DMEClaimGroup record = new DMEClaimGroup();
		record.version = RifFilesProcessor.RECORD_FORMAT_VERSION;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.claimId = "2929923122";
		record.dateFrom = LocalDate.of(1848, 01, 24);
		record.dateThrough = LocalDate.of(1850, 01, 01);
		record.nearLineRecordIdCode = '1';
		record.claimDispositionCode = "01";
		record.carrierNumber = "06102";
		record.paymentDenialCode = "1";
		record.paymentAmount = new BigDecimal("130.32");
		record.referringPhysicianNpi = "1265415426";
		record.providerPaymentAmount = new BigDecimal("123.45");
		record.diagnosisPrincipal = new IcdCode(IcdVersion.ICD_10, "F63.2");
		record.diagnosesAdditional.add(new IcdCode(IcdVersion.ICD_10, "R44.3"));
		DMEClaimLine recordLine1 = new DMEClaimLine();
		record.lines.add(recordLine1);
		recordLine1.number = 1;
		recordLine1.cmsServiceTypeCode = "90853-HE";
		recordLine1.betosCode = "M5C";
		recordLine1.paymentAmount = new BigDecimal("123.45");
		recordLine1.beneficiaryPaymentAmount = new BigDecimal("0");
		recordLine1.providerPaymentAmount = new BigDecimal("120.20");
		recordLine1.beneficiaryPartBDeductAmount = new BigDecimal("18.00");
		recordLine1.primaryPayerPaidAmount = new BigDecimal("11.00");
		recordLine1.coinsuranceAmount = new BigDecimal("20.20");
		recordLine1.primaryPayerAllowedChargeAmount = new BigDecimal("20.29");
		recordLine1.submittedChargeAmount = new BigDecimal("130.45");
		recordLine1.allowedChargeAmount = new BigDecimal("129.45");
		recordLine1.diagnosis = new IcdCode(IcdVersion.ICD_10, "F63.2");
		recordLine1.purchasePriceAmount = new BigDecimal("82.29");
		recordLine1.nationalDrugCode = Optional.of(new String("49884009902"));

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
		 * Bundle should have: 1) EOB, 2) Practitioner (referrer) 3) Medication.
		 */
		Assert.assertEquals(3, claimBundle.getEntry().size());
		BundleEntryComponent eobEntry = claimBundle.getEntry().stream()
				.filter(e -> e.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.POST, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		assertIdentifierExists(DataTransformer.CODING_SYSTEM_CCW_CLAIM_ID, record.claimId, eob.getIdentifier());
		// TODO Verify eob.type once STU3 is available (professional)

		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, eob.getPatient().getReference());
		Assert.assertEquals(record.nearLineRecordIdCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_RECORD_ID_CD).get(0).getValue())
						.getValue());
		assertDateEquals(record.dateFrom, eob.getBillablePeriod().getStartElement());
		assertDateEquals(record.dateThrough, eob.getBillablePeriod().getEndElement());
		Assert.assertEquals(DataTransformer.CODING_SYSTEM_CCW_CARR_CLAIM_DISPOSITION, eob.getDisposition());
		Assert.assertEquals(record.carrierNumber.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_CARRIER_NUMBER).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentDenialCode.toString(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_PAYMENT_DENIAL_CD).get(0)
						.getValue()).getValue());
		Assert.assertEquals(record.paymentAmount, eob.getPaymentAmount().getValue());

		ReferralRequest referral = (ReferralRequest) eob.getReferral().getResource();
		Assert.assertEquals("Patient/bene-" + record.beneficiaryId, referral.getPatient().getReference());
		Assert.assertEquals(1, referral.getRecipient().size());
		Assert.assertEquals(claimBundle.getEntry().stream()
				.filter(entryIsPractitionerWithNpi(record.referringPhysicianNpi)).findAny().get().getFullUrl(),
				referral.getRecipient().get(0).getReference());
		BundleEntryComponent referrerEntry = claimBundle.getEntry().stream().filter(r -> {
			if (!(r.getResource() instanceof Practitioner))
				return false;
			Practitioner referrer = (Practitioner) r.getResource();
			return referrer.getIdentifier().stream()
					.filter(i -> DataTransformer.CODING_SYSTEM_NPI_US.equals(i.getSystem()))
					.filter(i -> record.referringPhysicianNpi.equals(i.getValue())).findAny().isPresent();
		}).findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, referrerEntry.getRequest().getMethod());
		Assert.assertEquals(DataTransformer.referencePractitioner(record.referringPhysicianNpi).getReference(),
				referrerEntry.getRequest().getUrl());
		/*
		 * TODO once STU3 is available, verify amounts in eob.information
		 * entries
		 */
		Assert.assertEquals(2, eob.getDiagnosis().size());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobItem0 = eob.getItem().get(0);
		Assert.assertEquals(new Integer(recordLine1.number), new Integer(eobItem0.getSequence()));
		Assert.assertEquals("CSPINV", eobItem0.getType().getCode());

		/*
		 * TODO Once STU3 is available, verify eob.item.careTeam
		 * entries.providerTypeCode,providerSpecialityCode,
		 * providerParticipatingIndCode, providerStateCode
		 */
		/*
		 * TODO Once STU3 is available, verify eob.item.category.
		 */
		/*
		 * TODO once STU3 is available, verify eob.line.location
		 */
		assertCodingEquals(DataTransformer.CODING_SYSTEM_HCPCS, recordLine1.hcpcsCode, eobItem0.getService());
		Assert.assertEquals(recordLine1.betosCode,
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

		BundleEntryComponent medicationEntry = claimBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof Medication).findAny().get();
		Medication medication = (Medication) medicationEntry.getResource();
		assertCodingEquals(DataTransformer.CODING_SYSTEM_NDC, recordLine1.nationalDrugCode.toString(),
				medication.getCode().getCoding().get(0));
		Assert.assertEquals(HTTPVerb.PUT, medicationEntry.getRequest().getMethod());
		Assert.assertEquals("Medication/ndc-" + recordLine1.nationalDrugCode, medicationEntry.getRequest().getUrl());
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
	 *            {@link ItemAdjudicationComponent#getCategory()} to find and
	 *            verify
	 * @param expectedAmount
	 *            the expected {@link ItemAdjudicationComponent#getAmount()}
	 * @param actuals
	 *            the actual {@link ItemAdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationEquals(String expectedCategoryCode, BigDecimal expectedAmount,
			List<ItemAdjudicationComponent> actuals) {
		Optional<ItemAdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
		Assert.assertTrue(adjudication.isPresent());
		Assert.assertEquals(expectedAmount, adjudication.get().getAmount().getValue());
	}

	/**
	 * @param expectedCategoryCode
	 *            the expected {@link Coding#getCode()} of the
	 *            {@link ItemAdjudicationComponent#getCategory()} to verify is
	 *            not present
	 * @param actuals
	 *            the actual {@link ItemAdjudicationComponent}s to verify
	 */
	private static void assertAdjudicationNotPresent(String expectedCategoryCode,
			List<ItemAdjudicationComponent> actuals) {
		Optional<ItemAdjudicationComponent> adjudication = actuals.stream()
				.filter(a -> DataTransformer.CODING_SYSTEM_ADJUDICATION_CMS.equals(a.getCategory().getSystem()))
				.filter(a -> expectedCategoryCode.equals(a.getCategory().getCode())).findAny();
		Assert.assertFalse(adjudication.isPresent());
	}

	/**
	 * @param expectedDiagnosis
	 *            the expected {@link IcdCode} to verify the presence of in the
	 *            {@link ItemsComponent}
	 * @param eob
	 *            the {@link ExplanationOfBenefit} to verify
	 * @param eobItem
	 *            the {@link ItemsComponent} to verify
	 */
	private static void assertDiagnosisLinkPresent(IcdCode expectedDiagnosis, ExplanationOfBenefit eob,
			ItemsComponent eobItem) {
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
