package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.exceptions.FHIRException;
import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu21.model.CodeableConcept;
import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.DetailComponent;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit.ItemsComponent;
import org.hl7.fhir.dstu21.model.IntegerType;
import org.hl7.fhir.dstu21.model.MedicationOrder;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Practitioner;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.dstu21.model.StringType;
import org.junit.Assert;
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
import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
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
		Assert.assertEquals(
				eob.getProvider()
						.getReference(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
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
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getAttendingPhysicianNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				eob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OPERATING_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> sourceClaim.getOperatingPhysicianNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(
				eob.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_OTHER_PHYSICIAN.equals(x.getUrl()))
						.map(x -> ((Reference) x.getValue())
								.getReference())
						.findAny().get(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
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
		Assert.assertEquals(
				carrierEob.getProvider()
						.getReference(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
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
		Assert.assertEquals(carrierClaimLineForBeneA.getAllowedAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_ALLOWED_CHARGE
								.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(
				carrierClaimLineForBeneA
						.getDeductibleAmount(),
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
		Assert.assertEquals(
				carrierClaimLineForBeneA
						.getNchPaymentAmount(),
				carrierEobItem.getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PAYMENT.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(0, (int) carrierEobItem.getDiagnosisLinkId().get(0).getValue());

		Assert.assertEquals(1, bundle.getExplanationOfBenefitsForPartD().size());
		ExplanationOfBenefit partDEob = bundle.getExplanationOfBenefitsForPartD().get(0);
		Assert.assertEquals(patientA.getId(), partDEob.getPatient().getReference());
		Assert.assertEquals("" + partDEventForBeneA.getId(), partDEob.getIdentifier().get(0).getValue());
		Assert.assertEquals(
				partDEob.getProvider()
						.getReference(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
						.filter(p -> p.getIdentifier().stream()
								.filter(i -> i.getSystem() == DataTransformer.CODING_SYSTEM_NPI_US)
								.filter(i -> partDEventForBeneA.getServiceProviderNpi().toString().equals(i.getValue()))
								.findAny().isPresent())
						.findAny().get().getId());
		Assert.assertEquals(Date.valueOf(partDEventForBeneA.getServiceDate()),
				partDEob.getItem().get(0).getServicedDateType().getValue());
		Assert.assertEquals(
				partDEventForBeneA
						.getPatientPayAmount(),
				partDEob.getItem().get(0).getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_PATIENT_PAY.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);
		Assert.assertEquals(
				partDEventForBeneA
						.getTotalPrescriptionCost(),
				partDEob.getItem().get(0).getAdjudication().stream()
						.filter(a -> DataTransformer.CODED_ADJUDICATION_TOTAL_COST.equals(a.getCategory().getCode()))
						.findAny().get().getAmount().getValue().doubleValue(),
				0.0);

		Assert.assertEquals(1, bundle.getFhirResources().stream().filter(r -> r instanceof MedicationOrder).count());
		MedicationOrder partDOrder = bundle.getFhirResources().stream().filter(r -> r instanceof MedicationOrder)
				.map(r -> (MedicationOrder) r).findAny().get();
		Assert.assertEquals(partDOrder.getId(), partDEob.getPrescription().getReference());
		Assert.assertEquals(
				partDOrder.getPrescriber()
						.getReference(),
				bundle.getFhirResources().stream()
						.filter(r -> r instanceof Practitioner).map(
								r -> (Practitioner) r)
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
		record.version = 1;
		record.recordAction = RecordAction.INSERT;
		record.beneficiaryId = "42";
		record.stateCode = "HI";
		record.countyCode = "Transylvania";
		record.postalCode = "12345";

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

		Bundle beneBundle = beneBundleWrapper.getResult();
		Assert.assertEquals(5, beneBundle.getEntry().size());
		BundleEntryComponent beneEntry = beneBundle.getEntry().stream().filter(r -> r.getResource() instanceof Patient)
				.findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, beneEntry.getRequest().getMethod());
		Assert.assertEquals("Patient/" + record.beneficiaryId, beneEntry.getRequest().getUrl());
		Patient bene = (Patient) beneEntry.getResource();
		Assert.assertEquals(bene.getId(), "Patient/" + record.beneficiaryId);
		Assert.assertEquals(1, bene.getAddress().size());
		Assert.assertEquals(record.stateCode, bene.getAddress().get(0).getState());
		Assert.assertEquals(record.countyCode, bene.getAddress().get(0).getDistrict());
		Assert.assertEquals(record.postalCode, bene.getAddress().get(0).getPostalCode());
		// TODO test the rest of the columns/resource once they're all ready
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
		PartDEventRow record = new PartDEventRow();
		record.version = 1;
		record.recordAction = RecordAction.INSERT;
		record.partDEventId = "5";
		record.beneficiaryId = "17";
		record.compoundCode = Optional.of(PartDEventRow.COMPOUND_CODE_COMPOUNDED);
		record.prescriptionFillDate = LocalDate.of(2015, 6, 14);

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent pdeRecordEvent = new RifRecordEvent<PartDEventRow>(filesEvent, file, record);

		Stream source = Arrays.asList(pdeRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		Assert.assertNotNull(resultStream);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());
		Assert.assertEquals(1, resultList.size());

		TransformedBundle pdeBundleWrapper = resultList.get(0);
		Assert.assertNotNull(pdeBundleWrapper);
		Assert.assertSame(pdeRecordEvent, pdeBundleWrapper.getSource());
		Assert.assertNotNull(pdeBundleWrapper.getResult());

		Bundle pdeBundle = pdeBundleWrapper.getResult();
		Assert.assertEquals(1, pdeBundle.getEntry().size());
		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		Assert.assertEquals(HTTPVerb.PUT, eobEntry.getRequest().getMethod());
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();
		Assert.assertEquals("Patient/" + record.beneficiaryId, eob.getPatient().getReference());

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();
		Assert.assertEquals("RXCINV", rxItem.getType().getCode());
		Assert.assertEquals(Date.valueOf(record.prescriptionFillDate), rxItem.getServicedDateType().getValue());
		// TODO rest of values
	}

	/**
	 * Verifies that {@link DataTransformer} correctly sets the code system
	 * value when the compound code equals not compounded.
	 * 
	 * @throws FHIRException
	 *             indicates test failure
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	public void transformInsertPartDEventNotCompound() throws FHIRException {
		PartDEventRow record = new PartDEventRow();
		record.version = 1;
		record.recordAction = RecordAction.INSERT;
		record.partDEventId = "5";
		record.beneficiaryId = "17";
		record.compoundCode = Optional.of(PartDEventRow.COMPOUND_CODE_NOT_COMPOUNDED);
		record.prescriptionFillDate = LocalDate.of(2015, 6, 14);

		RifFile file = new MockRifFile();
		RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), file);
		RifRecordEvent pdeRecordEvent = new RifRecordEvent<PartDEventRow>(filesEvent, file, record);

		Stream source = Arrays.asList(pdeRecordEvent).stream();
		DataTransformer transformer = new DataTransformer();
		Stream<TransformedBundle> resultStream = transformer.transform(source);
		List<TransformedBundle> resultList = resultStream.collect(Collectors.toList());

		TransformedBundle pdeBundleWrapper = resultList.get(0);
		Bundle pdeBundle = pdeBundleWrapper.getResult();
		BundleEntryComponent eobEntry = pdeBundle.getEntry().stream()
				.filter(r -> r.getResource() instanceof ExplanationOfBenefit).findAny().get();
		ExplanationOfBenefit eob = (ExplanationOfBenefit) eobEntry.getResource();

		ItemsComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();
		Assert.assertEquals("RXDINV", rxItem.getType().getCode());
	}
}
