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
import org.hl7.fhir.dstu21.model.CodeableConcept;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.DateTimeType;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.DetailComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.DiagnosisComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemAdjudicationComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemsComponent;
import org.hl7.fhir.dstu21.model.Identifier;
import org.hl7.fhir.dstu21.model.IntegerType;
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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.AllClaimsProfile;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.DiagnosisRelatedGroup;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.Procedure;
import gov.hhs.cms.bluebutton.datapipeline.fhir.SpringConfigForTests;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.RifFilesProcessor;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup.CarrierClaimLine;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CompoundCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.DrugCoverageStatus;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.IcdCode.IcdVersion;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.PartDEventRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RecordAction;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFilesEvent;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifRecordEvent;

/**
 * Unit tests for {@link DataTransformer}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
public final class DataTransformerTest {
	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

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
	 * empty stream.
	 */
	@Test
	public void transformEmptyStream() {
		DataTransformer transformer = new DataTransformer();

		Stream<CurrentBeneficiary> emptySourceStream = new ArrayList<CurrentBeneficiary>().stream();
		Stream<BeneficiaryBundle> transformedFhirStream = transformer.transformSourceData(emptySourceStream);
		Assert.assertNotNull(transformedFhirStream);
		Assert.assertEquals(0, transformedFhirStream.count());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * small hand-crafted data set with an inpatient claim.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformInpatientClaim() throws FHIRException {
		// Create some mock data.
		CurrentBeneficiary bene = new CurrentBeneficiary().setId(0);
		PartAClaimFact sourceClaim = new PartAClaimFact().setId(0L).setBeneficiary(bene)
				.setClaimProfile(new AllClaimsProfile().setId(1L).setClaimType(ClaimType.INPATIENT_CLAIM))
				.setDiagnosisGroup(new DiagnosisRelatedGroup().setId(1L).setCode("foo"))
				.setDateAdmission(LocalDate.now()).setDateFrom(LocalDate.now()).setDateThrough(LocalDate.now())
				.setDateDischarge(LocalDate.now()).setProviderAtTimeOfClaimNpi(42L).setUtilizationDayCount(3L)
				.setPayment(new BigDecimal("1.00")).setPassThroughPerDiemAmount(new BigDecimal("1.50"))
				.setNchBeneficiaryBloodDeductibleLiability(new BigDecimal("2.00"))
				.setNchBeneficiaryInpatientDeductible(new BigDecimal("3.00"))
				.setNchBeneficiaryPartACoinsuranceLiability(new BigDecimal("4.00"))
				.setNchPrimaryPayerPaid(new BigDecimal("5.00")).setAttendingPhysicianNpi(43L)
				.setOperatingPhysicianNpi(44L).setOtherPhysicianNpi(45L).setAdmittingDiagnosisCode("foo");
		bene.getPartAClaimFacts().add(sourceClaim);
		PartAClaimRevLineFact sourceClaimLine = new PartAClaimRevLineFact().setClaim(sourceClaim).setLineNumber(1)
				.setDiagnosisCode1("bar").setProcedureCode1("fizz");
		sourceClaim.getClaimLines().add(sourceClaimLine);

		// Run the transformer against the mock data.
		DataTransformer transformer = new DataTransformer();
		Stream<CurrentBeneficiary> emptySourceStream = Arrays.asList(bene).stream();
		Stream<BeneficiaryBundle> transformedFhirStream = transformer.transformSourceData(emptySourceStream);
		List<BeneficiaryBundle> transformedBundles = transformedFhirStream.collect(Collectors.toList());

		/*
		 * Verify the transformation results.
		 */
		Assert.assertEquals(1, transformedBundles.size());
		BeneficiaryBundle bundle = transformedBundles.get(0);
		Patient patient = bundle.getPatient();

		// Verify the transformed inpatient EOB and its sole item
		Assert.assertEquals(1, bundle.getExplanationOfBenefitsForInpatient().size());
		ExplanationOfBenefit eob = bundle.getExplanationOfBenefitsForInpatient().get(0);
		Assert.assertEquals("" + sourceClaim.getId(), eob.getIdentifier().get(0).getValue());
		Assert.assertEquals(patient.getId(), eob.getPatient().getReference());
		Assert.assertEquals(sourceClaim.getDiagnosisGroup().getCode(),
				((StringType) eob.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_DIAGNOSIS_GROUP).get(0).getValue())
						.getValue());
		Assert.assertEquals(Date.valueOf(sourceClaim.getDateFrom()), eob.getBillablePeriod().getStart());
		Assert.assertEquals(Date.valueOf(sourceClaim.getDateThrough()), eob.getBillablePeriod().getEnd());
		Assert.assertEquals(1, eob.getItem().size());
		ItemsComponent eobSoleItem = eob.getItem().get(0);
		Assert.assertEquals(Date.valueOf(sourceClaim.getDateAdmission()), eobSoleItem.getServicedPeriod().getStart());
		Assert.assertEquals(Date.valueOf(sourceClaim.getDateDischarge()), eobSoleItem.getServicedPeriod().getEnd());
		Assert.assertEquals(eob.getProvider().getReference(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getProviderAtTimeOfClaimNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals((long) sourceClaim.getUtilizationDayCount(),
				eobSoleItem.getQuantity().getValue().longValue());
		Assert.assertEquals(sourceClaim.getPayment(),
				eobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PAYMENT.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(sourceClaim.getPassThroughPerDiemAmount(), eobSoleItem.getAdjudication().stream().filter(
				a -> DataTransformer.CODED_ADJUDICATION_PASS_THROUGH_PER_DIEM_AMOUNT.equals(a.getCategory().getCode()))
				.findAny().get().getAmount().getValue());
		Assert.assertEquals(sourceClaim.getNchBeneficiaryBloodDeductibleLiability(),
				eobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(sourceClaim.getNchBeneficiaryInpatientDeductible(),
				eobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_INPATIENT_DEDUCTIBLE
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(sourceClaim.getNchBeneficiaryPartACoinsuranceLiability(),
				eobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_A_COINSURANCE_LIABILITY
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(sourceClaim.getNchPrimaryPayerPaid(),
				eobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_PRIMARY_PAYER_CLAIM_PAID_AMOUNT
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(
				eob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_ATTENDING_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue()).getReference()).findAny().get(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getAttendingPhysicianNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				eob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OPERATING_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue()).getReference()).findAny().get(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getOperatingPhysicianNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				eob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OTHER_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue()).getReference()).findAny().get(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getOtherPhysicianNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(sourceClaim.getAdmittingDiagnosisCode(),
				((Coding) eob.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_ADMITTING_DIAGNOSIS).get(0).getValue())
						.getCode());

		// Verify EOB's sole item's detail and subdetail components.
		Assert.assertEquals(sourceClaim.getClaimLines().size(), eobSoleItem.getDetail().size());
		DetailComponent outpatientEobDetail = eobSoleItem.getDetail().get(0);
		Assert.assertEquals(sourceClaimLine.getLineNumber(), outpatientEobDetail.getSequence());
		Assert.assertEquals(1, eob.getDiagnosis().size());
		Assert.assertEquals(0,
				((IntegerType) outpatientEobDetail.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_DIAGNOSIS_LINK_ID)
						.get(0).getValue()).getValue().intValue());
		Assert.assertEquals(1, outpatientEobDetail.getSubDetail().size());
		Assert.assertEquals(sourceClaimLine.getProcedureCode1(),
				outpatientEobDetail.getSubDetail().get(0).getService().getCode());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * small hand-crafted data set.
	 * 
	 * @throws FHIRException
	 *             (indicates test failure)
	 */
	@Test
	public void transformSmallDataset() throws FHIRException {
		// Create some mock data.
		CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now()).setGivenName("John")
				.setSurname("Doe").setContactAddress("123 Main St, Anytown, MD").setContactAddressZip("123456789");
		PartAClaimFact outpatientClaimForBeneA = new PartAClaimFact().setId(0L).setBeneficiary(beneA)
				.setClaimProfile(new AllClaimsProfile().setId(1L).setClaimType(ClaimType.OUTPATIENT_CLAIM))
				.setDateFrom(LocalDate.now()).setDateThrough(LocalDate.now()).setProviderAtTimeOfClaimNpi(42L)
				.setPayment(new BigDecimal("1.00")).setNchBeneficiaryBloodDeductibleLiability(new BigDecimal("2.00"))
				.setNchBeneficiaryPartBDeductible(new BigDecimal("3.00"))
				.setNchBeneficiaryPartBCoinsurance(new BigDecimal("4.00"))
				.setNchPrimaryPayerPaid(new BigDecimal("5.00")).setAttendingPhysicianNpi(43L)
				.setOperatingPhysicianNpi(44L).setOtherPhysicianNpi(45L).setAdmittingDiagnosisCode("foo");
		beneA.getPartAClaimFacts().add(outpatientClaimForBeneA);
		PartAClaimRevLineFact outpatientClaimLineForBeneA = new PartAClaimRevLineFact()
				.setClaim(outpatientClaimForBeneA).setLineNumber(1).setRevenueCenter(new Procedure().setCode("foo"))
				.setDiagnosisCode1("bar").setProcedureCode1("fizz");
		outpatientClaimForBeneA.getClaimLines().add(outpatientClaimLineForBeneA);
		PartBClaimFact carrierClaimForBeneA = new PartBClaimFact().setId(0L).setBeneficiary(beneA)
				.setClaimProfile(new AllClaimsProfile().setId(1L).setClaimType(ClaimType.CARRIER_NON_DME_CLAIM))
				.setCarrierControlNumber(0L).setDiagnosisCode1("foo").setDiagnosisCode2("bar").setProviderNpi(12345L);
		beneA.getPartBClaimFacts().add(carrierClaimForBeneA);
		PartBClaimLineFact carrierClaimLineForBeneA = new PartBClaimLineFact().setClaim(carrierClaimForBeneA)
				.setLineNumber(1).setBeneficiary(beneA).setProcedure(new Procedure().setId(0L).setCode("foo"))
				.setDateFrom(LocalDate.now()).setDateThrough(LocalDate.now()).setAllowedAmount(42.0)
				.setDeductibleAmount(43.0).setBeneficiaryPrimaryPayerPaidAmount(44.0).setCoinsuranceAmount(45.0)
				.setNchPaymentAmount(46.0).setLineDiagnosisCode("bar").setProcessingIndicationCode("foo");
		carrierClaimForBeneA.getClaimLines().add(carrierClaimLineForBeneA);
		PartDEventFact partDEventForBeneA = new PartDEventFact().setId(0L).setBeneficiary(beneA).setPrescriberNpi(1234L)
				.setServiceProviderNpi(2345L).setProductNdc(3456L).setServiceDate(LocalDate.now())
				.setQuantityDispensed(12L).setNumberDaysSupply(43L).setPatientPayAmount(42.0)
				.setTotalPrescriptionCost(142.0);
		beneA.getPartDEventFacts().add(partDEventForBeneA);
		CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
		PartAClaimFact outpatientClaimForBeneB = new PartAClaimFact().setId(1L).setBeneficiary(beneB)
				.setClaimProfile(outpatientClaimForBeneA.getClaimProfile()).setAdmittingDiagnosisCode("foo");
		beneB.getPartAClaimFacts().add(outpatientClaimForBeneB);

		// Run the transformer against the mock data.
		DataTransformer transformer = new DataTransformer();
		Stream<CurrentBeneficiary> emptySourceStream = Arrays.asList(beneA, beneB).stream();
		Stream<BeneficiaryBundle> transformedFhirStream = transformer.transformSourceData(emptySourceStream);
		List<BeneficiaryBundle> transformedBundles = transformedFhirStream.collect(Collectors.toList());

		/*
		 * Verify the transformation results.
		 */
		Assert.assertEquals(2, transformedBundles.size());

		BeneficiaryBundle bundle = transformedBundles.get(0);
		Patient patientA = bundle.getPatient();
		Assert.assertEquals(1, patientA.getIdentifier().size());
		Assert.assertEquals("" + beneA.getId(), patientA.getIdentifier().get(0).getValue());

		Assert.assertEquals(1, patientA.getName().size());
		Assert.assertEquals(beneA.getGivenName(), patientA.getName().get(0).getGivenAsSingleString());
		Assert.assertEquals(beneA.getSurname(), patientA.getName().get(0).getFamilyAsSingleString());

		Assert.assertEquals(Date.valueOf(beneA.getBirthDate()), patientA.getBirthDate());

		Assert.assertEquals(1, patientA.getAddress().size());
		Assert.assertEquals(2, patientA.getAddress().get(0).getLine().size());
		Assert.assertEquals(beneA.getContactAddress(), patientA.getAddress().get(0).getLine().get(0).getValue());
		Assert.assertEquals(beneA.getContactAddressZip(), patientA.getAddress().get(0).getLine().get(1).getValue());

		// Verify a transformed outpatient EOB and its sole item
		Assert.assertEquals(1, bundle.getExplanationOfBenefitsForOutpatient().size());
		ExplanationOfBenefit outpatientEob = bundle.getExplanationOfBenefitsForOutpatient().get(0);
		Assert.assertEquals("" + outpatientClaimForBeneA.getId(), outpatientEob.getIdentifier().get(0).getValue());
		Assert.assertEquals(patientA.getId(), outpatientEob.getPatient().getReference());
		Assert.assertEquals(Date.valueOf(outpatientClaimForBeneA.getDateFrom()),
				outpatientEob.getBillablePeriod().getStart());
		Assert.assertEquals(Date.valueOf(outpatientClaimForBeneA.getDateThrough()),
				outpatientEob.getBillablePeriod().getEnd());
		Assert.assertEquals(1, outpatientEob.getItem().size());
		ItemsComponent outpatientEobSoleItem = outpatientEob.getItem().get(0);
		Assert.assertEquals(
				outpatientEob.getProvider()
						.getReference(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> outpatientClaimForBeneA.getProviderAtTimeOfClaimNpi().toString()
										.equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(outpatientClaimForBeneA.getPayment(),
				outpatientEobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PAYMENT.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(outpatientClaimForBeneA.getNchBeneficiaryBloodDeductibleLiability(),
				outpatientEobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_BLOOD_DEDUCTIBLE_LIABILITY_AMOUNT
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(outpatientClaimForBeneA.getNchBeneficiaryPartBDeductible(),
				outpatientEobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_DEDUCTIBLE
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(outpatientClaimForBeneA.getNchBeneficiaryPartBCoinsurance(),
				outpatientEobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_BENEFICIARY_PART_B_COINSURANCE
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(outpatientClaimForBeneA.getNchPrimaryPayerPaid(),
				outpatientEobSoleItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_NCH_PRIMARY_PAYER_CLAIM_PAID_AMOUNT
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue());
		Assert.assertEquals(
				outpatientEob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_ATTENDING_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> outpatientClaimForBeneA.getAttendingPhysicianNpi().toString()
										.equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				outpatientEob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OPERATING_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> outpatientClaimForBeneA.getOperatingPhysicianNpi().toString()
										.equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				outpatientEob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OTHER_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> outpatientClaimForBeneA.getOtherPhysicianNpi().toString()
										.equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(outpatientClaimForBeneA.getAdmittingDiagnosisCode(), ((Coding) outpatientEob
				.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_ADMITTING_DIAGNOSIS).get(0).getValue()).getCode());

		// Verify a transformed outpatient EOB and its sole item
		Assert.assertEquals(outpatientClaimForBeneA.getClaimLines().size(), outpatientEobSoleItem.getDetail().size());
		DetailComponent outpatientEobDetail = outpatientEobSoleItem.getDetail().get(0);
		Assert.assertEquals(outpatientClaimLineForBeneA.getLineNumber(), outpatientEobDetail.getSequence());
		Assert.assertEquals(outpatientClaimLineForBeneA.getRevenueCenter().getCode(),
				outpatientEobDetail.getService().getCode());
		Assert.assertEquals(1, outpatientEob.getDiagnosis().size());
		Assert.assertEquals(0,
				((IntegerType) outpatientEobDetail.getExtensionsByUrl(DataTransformer.EXTENSION_CMS_DIAGNOSIS_LINK_ID)
						.get(0).getValue()).getValue().intValue());
		Assert.assertEquals(1, outpatientEobDetail.getSubDetail().size());
		Assert.assertEquals(outpatientClaimLineForBeneA.getProcedureCode1(),
				outpatientEobDetail.getSubDetail().get(0).getService().getCode());

		Assert.assertEquals(1, bundle.getExplanationOfBenefitsForCarrier().size());
		ExplanationOfBenefit carrierEob = bundle.getExplanationOfBenefitsForCarrier().get(0);
		Assert.assertEquals(patientA.getId(), carrierEob.getPatient().getReference());
		Assert.assertEquals("" + carrierClaimForBeneA.getCarrierControlNumber(),
				carrierEob.getIdentifier().get(0).getValue());
		Assert.assertEquals(carrierClaimForBeneA.getDiagnosisCode1(),
				carrierEob.getDiagnosis().get(0).getDiagnosis().getCode());
		Assert.assertEquals(carrierClaimForBeneA.getDiagnosisCode2(),
				carrierEob.getDiagnosis().get(1).getDiagnosis().getCode());
		Assert.assertNotNull(carrierEob.getProvider().getReference());
		Assert.assertEquals(carrierEob.getProvider().getReference(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> carrierClaimForBeneA.getProviderNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(1, carrierEob.getItem().size());

		ItemsComponent carrierEobItem = carrierEob.getItem().get(0);
		Assert.assertEquals(1, carrierEobItem.getSequence());
		Assert.assertEquals(patientA.getId(), carrierEob.getPatient().getReference());
		Assert.assertEquals(carrierClaimLineForBeneA.getProcedure().getCode(), carrierEobItem.getService().getCode());
		Assert.assertEquals(Date.valueOf(carrierClaimLineForBeneA.getDateFrom()),
				carrierEobItem.getServicedPeriod().getStart());
		Assert.assertEquals(Date.valueOf(carrierClaimLineForBeneA.getDateThrough()),
				carrierEobItem.getServicedPeriod().getEnd());
		Assert.assertEquals(carrierClaimLineForBeneA.getAllowedAmount(), carrierEobItem.getAdjudication().stream()
				.filter(a -> DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE.equals(a.getCategory().getCode()))
				.findAny().get().getAmount().getValue().doubleValue(), 0.0);
		Assert.assertEquals(carrierClaimLineForBeneA.getDeductibleAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_DEDUCTIBLE.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(carrierClaimLineForBeneA.getBeneficiaryPrimaryPayerPaidAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_BENEFICIARY_PRIMARY_PAYER_PAID
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(carrierClaimLineForBeneA.getCoinsuranceAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_LINE_COINSURANCE_AMOUNT
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(carrierClaimLineForBeneA.getNchPaymentAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PAYMENT.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(0, (int) carrierEobItem.getDiagnosisLinkId().get(0).getValue());

		Assert.assertEquals(1, bundle.getExplanationOfBenefitsForPartD().size());
		ExplanationOfBenefit partDEob = bundle.getExplanationOfBenefitsForPartD().get(0);
		Assert.assertEquals(patientA.getId(), partDEob.getPatient().getReference());
		Assert.assertEquals("" + partDEventForBeneA.getId(), partDEob.getIdentifier().get(0).getValue());
		Assert.assertEquals(partDEob.getProvider().getReference(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> partDEventForBeneA.getServiceProviderNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(Date.valueOf(partDEventForBeneA.getServiceDate()),
				partDEob.getItem().get(0).getServicedDateType().getValue());
		Assert.assertEquals(partDEventForBeneA.getPatientPayAmount(),
				partDEob.getItem().get(0).getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PATIENT_PAY.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(partDEventForBeneA.getTotalPrescriptionCost(),
				partDEob.getItem().get(0).getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_TOTAL_COST.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);

		Assert.assertEquals(1, bundle.getFhirResources().stream().filter(r -> r instanceof MedicationOrder).count());
		MedicationOrder partDOrder = bundle.getFhirResources().stream().filter(r -> r instanceof MedicationOrder)
				.map(r -> (MedicationOrder) r).findAny().get();
		Assert.assertEquals(partDOrder.getId(), partDEob.getPrescription().getReference());
		Assert.assertEquals(partDOrder.getPrescriber().getReference(),
				bundle.getFhirResources().stream().filter(r -> r instanceof Practitioner).map(r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> partDEventForBeneA.getPrescriberNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals("" + partDEventForBeneA.getProductNdc(),
				((CodeableConcept) partDOrder.getMedication()).getCoding().get(0).getCode());
		Assert.assertEquals(partDEventForBeneA.getQuantityDispensed().longValue(),
				partDOrder.getDispenseRequest().getQuantity().getValue().longValue());
		Assert.assertEquals(partDEventForBeneA.getNumberDaysSupply().longValue(),
				partDOrder.getDispenseRequest().getExpectedSupplyDuration().getValue().longValue());
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
		record.claimId = "SuttersMill";
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
		RifRecordEvent beneRecordEvent = new RifRecordEvent<CarrierClaimGroup>(filesEvent, file, record);

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

		Bundle claimBundle = beneBundleWrapper.getResult();
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
				.getExtensionsByUrl(DataTransformer.CODING_SYSTEM_CCW_CARR_RECORD_ID_CD).get(0).getValue()).getValue());
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
