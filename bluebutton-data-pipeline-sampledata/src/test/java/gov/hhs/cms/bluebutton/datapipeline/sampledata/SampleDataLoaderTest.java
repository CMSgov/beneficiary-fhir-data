package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
public final class SampleDataLoaderTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataLoaderTest.class);

	/**
	 * Verifies that {@link SampleDataLoader} loads the expected number of
	 * records when run against {@link SynpufArchive#SAMPLE_TEST_A}.
	 */
	@Test
	public void verifyCountsForSampleTestA() {
		// Run the loader and verify the results.
		SampleDataLoader loader = new SampleDataLoader(new MetricRegistry());
		SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
		List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);

		Assert.assertEquals(archive.getBeneficiaryCount(), beneficiaries.size());

		int partAFactCount = beneficiaries.stream().mapToInt(b -> b.getPartAClaimFacts().size()).sum();
		Assert.assertTrue(partAFactCount > 0);
		int partARevLineFactCount = beneficiaries.stream()
				.mapToInt(b -> b.getPartAClaimFacts().stream().mapToInt(c -> c.getClaimLines().size()).sum()).sum();
		Assert.assertTrue(partARevLineFactCount > partAFactCount);

		int partBFactCount = beneficiaries.stream().mapToInt(b -> b.getPartBClaimFacts().size()).sum();
		Assert.assertTrue(partBFactCount > 0);
		int partBFactLineCount = beneficiaries.stream()
				.mapToInt(b -> b.getPartBClaimFacts().stream().mapToInt(c -> c.getClaimLines().size()).sum()).sum();
		Assert.assertTrue(partBFactLineCount > partBFactCount);

		int partDFactCount = beneficiaries.stream().mapToInt(b -> b.getPartDEventFacts().size()).sum();
		Assert.assertTrue(partDFactCount > 0);
	}

	/**
	 * Spot-checks a single loaded beneficiary from
	 * {@link SynpufArchive#SAMPLE_TEST_A} to verify that
	 * {@link SampleDataLoader} is handling fields as expected.
	 */
	@Test
	public void spotCheckBeneficiaryFromSampleTestA() {
		// Run the loader and verify the results.
		SampleDataLoader loader = new SampleDataLoader(new MetricRegistry());
		SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
		List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);

		// Grab the beneficiary to spot-check.
		CurrentBeneficiary beneficiary = beneficiaries.stream().filter(b -> 0 == b.getId()).findAny().get();

		// Spot check the CurrentBeneficiary itself.
		LOGGER.info("Checking against beneficiary: {}", beneficiary);
		Assert.assertEquals(1923, beneficiary.getBirthDate().getYear());
		Assert.assertTrue(beneficiary.getGivenName() != null && beneficiary.getGivenName().length() > 0);
		Assert.assertEquals("00013D2EFD8E45D1", beneficiary.getSurname());
		Assert.assertTrue(beneficiary.getContactAddress() != null && beneficiary.getContactAddress().length() > 0);
		Assert.assertTrue(
				beneficiary.getContactAddressZip() != null && beneficiary.getContactAddressZip().length() > 0);
		Assert.assertEquals(2, beneficiary.getPartAClaimFacts().size());

		// Spot check one of the beneficiary's inpatient claims.
		PartAClaimFact inpatientClaim = beneficiary.getPartAClaimFacts().stream()
				.filter(c -> 196661176988405L == c.getId()).findAny().get();
		LOGGER.info("Checking against inpatient claim: {}", inpatientClaim);
		Assert.assertEquals(ClaimType.INPATIENT_CLAIM, inpatientClaim.getClaimProfile().getClaimType());
		Assert.assertEquals("217", inpatientClaim.getDiagnosisGroup().getCode());
		Assert.assertEquals(2010, inpatientClaim.getDateAdmission().getYear());
		Assert.assertEquals(2010, inpatientClaim.getDateFrom().getYear());
		Assert.assertEquals(2010, inpatientClaim.getDateThrough().getYear());
		Assert.assertEquals(2010, inpatientClaim.getDateDischarge().getYear());
		Assert.assertNotNull(inpatientClaim.getProviderAtTimeOfClaimNpi());
		Assert.assertEquals(1L, (long) inpatientClaim.getUtilizationDayCount());
		Assert.assertEquals(new BigDecimal("4000.00"), inpatientClaim.getPayment());
		Assert.assertEquals(new BigDecimal("0.00"), inpatientClaim.getPassThroughPerDiemAmount());
		Assert.assertEquals(new BigDecimal("0.00"), inpatientClaim.getNchBeneficiaryBloodDeductibleLiability());
		Assert.assertEquals(new BigDecimal("1100.00"), inpatientClaim.getNchBeneficiaryInpatientDeductible());
		Assert.assertEquals(new BigDecimal("0.00"), inpatientClaim.getNchBeneficiaryPartACoinsuranceLiability());
		Assert.assertNull(inpatientClaim.getNchBeneficiaryPartBDeductible());
		Assert.assertNull(inpatientClaim.getNchBeneficiaryPartBCoinsurance());
		Assert.assertEquals(new BigDecimal("0.00"), inpatientClaim.getNchPrimaryPayerPaid());
		Assert.assertNotNull(inpatientClaim.getAttendingPhysicianNpi());
		Assert.assertNotNull(inpatientClaim.getOperatingPhysicianNpi());
		Assert.assertNotNull(inpatientClaim.getOtherPhysicianNpi());
		Assert.assertEquals("4580", inpatientClaim.getAdmittingDiagnosisCode());

		// Spot check one of the beneficiary's inpatient claim lines.
		Assert.assertEquals(1, inpatientClaim.getClaimLines().size());
		PartAClaimRevLineFact inpatientClaimRevLine = inpatientClaim.getClaimLines().get(0);
		LOGGER.info("Checking against inpatient claim rev line: {}", inpatientClaimRevLine);
		Assert.assertSame(inpatientClaim, inpatientClaimRevLine.getClaim());
		Assert.assertEquals(1, inpatientClaimRevLine.getLineNumber());
		Assert.assertNull(inpatientClaimRevLine.getRevenueCenter());
		Assert.assertEquals("7802", inpatientClaimRevLine.getDiagnosisCode1());
		Assert.assertEquals("78820", inpatientClaimRevLine.getDiagnosisCode2());
		Assert.assertEquals("V4501", inpatientClaimRevLine.getDiagnosisCode3());
		Assert.assertEquals("4280", inpatientClaimRevLine.getDiagnosisCode4());
		Assert.assertEquals("2720", inpatientClaimRevLine.getDiagnosisCode5());
		Assert.assertEquals("4019", inpatientClaimRevLine.getDiagnosisCode6());
		Assert.assertEquals("V4502", inpatientClaimRevLine.getDiagnosisCode7());
		Assert.assertEquals("73300", inpatientClaimRevLine.getDiagnosisCode8());
		Assert.assertEquals("E9330", inpatientClaimRevLine.getDiagnosisCode9());
		Assert.assertEquals("", inpatientClaimRevLine.getDiagnosisCode10());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode1());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode2());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode3());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode4());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode5());
		Assert.assertEquals("", inpatientClaimRevLine.getProcedureCode6());

		// Spot check one of the beneficiary's outpatient claims.
		PartAClaimFact outpatientClaim = beneficiary.getPartAClaimFacts().stream()
				.filter(c -> 542192281063886L == c.getId()).findAny().get();
		LOGGER.info("Checking against outpatient claim: {}", outpatientClaim);
		Assert.assertEquals(ClaimType.OUTPATIENT_CLAIM, outpatientClaim.getClaimProfile().getClaimType());
		Assert.assertEquals(2008, outpatientClaim.getDateFrom().getYear());
		Assert.assertEquals(2008, outpatientClaim.getDateThrough().getYear());
		Assert.assertNotNull(outpatientClaim.getProviderAtTimeOfClaimNpi());
		Assert.assertEquals(new BigDecimal("50.00"), outpatientClaim.getPayment());
		Assert.assertEquals(new BigDecimal("0.00"), outpatientClaim.getNchBeneficiaryBloodDeductibleLiability());
		Assert.assertEquals(new BigDecimal("0.00"), outpatientClaim.getNchBeneficiaryPartBDeductible());
		Assert.assertEquals(new BigDecimal("10.00"), outpatientClaim.getNchBeneficiaryPartBCoinsurance());
		Assert.assertEquals(new BigDecimal("0.00"), outpatientClaim.getNchPrimaryPayerPaid());
		Assert.assertNotNull(outpatientClaim.getAttendingPhysicianNpi());
		Assert.assertNotNull(outpatientClaim.getOperatingPhysicianNpi());
		Assert.assertNotNull(outpatientClaim.getOtherPhysicianNpi());
		Assert.assertEquals("V5883", outpatientClaim.getAdmittingDiagnosisCode());

		// Spot check one of the beneficiary's outpatient claim lines.
		Assert.assertEquals(1, outpatientClaim.getClaimLines().size());
		PartAClaimRevLineFact outpatientClaimRevLine = outpatientClaim.getClaimLines().get(0);
		LOGGER.info("Checking against outpatient claim rev line: {}", outpatientClaimRevLine);
		Assert.assertSame(outpatientClaim, outpatientClaimRevLine.getClaim());
		Assert.assertEquals(1, outpatientClaimRevLine.getLineNumber());
		Assert.assertEquals("85610", outpatientClaimRevLine.getRevenueCenter().getCode());
		Assert.assertEquals("V5841", outpatientClaimRevLine.getDiagnosisCode1());

		// Spot check one of the beneficiary's PartBClaimFacts.
		Assert.assertEquals(5, beneficiary.getPartBClaimFacts().size());
		PartBClaimFact carrierClaim = beneficiary.getPartBClaimFacts().stream()
				.filter(c -> 887213386947664L == c.getId()).findAny().get();
		LOGGER.info("Checking against carrier claim: {}", carrierClaim);
		Assert.assertSame(beneficiary, carrierClaim.getBeneficiary());
		Assert.assertEquals(ClaimType.CARRIER_NON_DME_CLAIM, carrierClaim.getClaimProfile().getClaimType());
		Assert.assertEquals(carrierClaim.getId(), carrierClaim.getCarrierControlNumber());
		Assert.assertEquals("3598", carrierClaim.getDiagnosisCode1());
		Assert.assertEquals("27541", carrierClaim.getDiagnosisCode2());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode3());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode4());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode5());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode6());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode7());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode8());
		Assert.assertEquals("", carrierClaim.getDiagnosisCode8());
		Assert.assertNotNull(carrierClaim.getProviderNpi());

		// Spot check one of the beneficiary's PartBClaimLineFacts.
		Assert.assertEquals(1, carrierClaim.getClaimLines().size());
		PartBClaimLineFact carrierClaimLine = carrierClaim.getClaimLines().get(0);
		LOGGER.info("Checking against carrier claim line: {}", carrierClaimLine);
		Assert.assertEquals(887213386947664L, (long) carrierClaim.getId());
		Assert.assertSame(carrierClaim, carrierClaimLine.getClaim());
		Assert.assertEquals(1L, carrierClaimLine.getLineNumber());
		Assert.assertSame(beneficiary, carrierClaimLine.getBeneficiary());
		Assert.assertTrue(carrierClaimLine.getProcedure().getId() >= 0);
		Assert.assertEquals("01996", carrierClaimLine.getProcedure().getCode());
		Assert.assertEquals(2009, carrierClaimLine.getDateFrom().getYear());
		Assert.assertEquals(2009, carrierClaimLine.getDateThrough().getYear());
		Assert.assertEquals(Double.valueOf(50.0), carrierClaimLine.getNchPaymentAmount());
		Assert.assertEquals(Double.valueOf(0.0), carrierClaimLine.getDeductibleAmount());
		Assert.assertEquals(Double.valueOf(0.0), carrierClaimLine.getBeneficiaryPrimaryPayerPaidAmount());
		Assert.assertEquals(Double.valueOf(10.0), carrierClaimLine.getCoinsuranceAmount());
		Assert.assertEquals(Double.valueOf(60.0), carrierClaimLine.getAllowedAmount());
		Assert.assertEquals("33818", carrierClaimLine.getLineDiagnosisCode());
		// TODO
		// Assert.assertEquals("???", partBClaimLine.getMiscCode().getId());
		// Assert.assertEquals("???",
		// partBClaimLine.getMiscCode().getCode());
		Assert.assertEquals("A", carrierClaimLine.getProcessingIndicationCode());

		// Spot check one of the beneficiary's PartDEventFacts.
		Assert.assertEquals(174, beneficiary.getPartDEventFacts().size());
		PartDEventFact partDEvent = beneficiary.getPartDEventFacts().stream().filter(e -> 233024488623927L == e.getId())
				.findAny().get();
		LOGGER.info("Checking against Part D event: {}", partDEvent);
		Assert.assertNotNull(partDEvent.getPrescriberNpi());
		Assert.assertNotNull(partDEvent.getServiceProviderNpi());
		Assert.assertEquals(54868407904L, (long) partDEvent.getProductNdc());
		Assert.assertEquals(2010, partDEvent.getServiceDate().getYear());
		Assert.assertEquals(30L, (long) partDEvent.getQuantityDispensed());
		Assert.assertEquals(30L, (long) partDEvent.getNumberDaysSupply());
		Assert.assertEquals(0.0, (double) partDEvent.getPatientPayAmount(), 0.0);
		Assert.assertEquals(180.0, (double) partDEvent.getTotalPrescriptionCost(), 0.0);
	}

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_TEST_B}
	 * to verify that no errors are thrown and the correct amount of data is
	 * produced.
	 */
	@Test
	public void verifyCountsFromSampleTestB() {
		MetricRegistry metrics = new MetricRegistry();
		metrics.registerAll(new MemoryUsageGaugeSet());
		metrics.registerAll(new GarbageCollectorMetricSet());

		SampleDataLoader loader = new SampleDataLoader(new MetricRegistry());
		SynpufArchive archive = SynpufArchive.SAMPLE_TEST_B;
		List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);
		Assert.assertEquals(archive.getBeneficiaryCount(), beneficiaries.size());

		// Collect and print out the metrics from the run, just cause.
		Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
		metricsReporter.report();
	}

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_TEST_B}
	 * to verify that no errors are thrown and the correct amount of data is
	 * produced.
	 */
	@Test
	@Ignore("slow (takes 60sec), so skipped by default")
	public void verifyCountsFromSampleTestD() {
		/*
		 * FIXME If I run this test by itself using Eclipse Mar's new feature
		 * that allows that, this seems to complete much faster. WTF?! Why?
		 */
		MetricRegistry metrics = new MetricRegistry();
		metrics.registerAll(new MemoryUsageGaugeSet());
		metrics.registerAll(new GarbageCollectorMetricSet());

		SampleDataLoader loader = new SampleDataLoader(new MetricRegistry());
		SynpufArchive archive = SynpufArchive.SAMPLE_TEST_D;
		List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);
		Assert.assertEquals(archive.getBeneficiaryCount(), beneficiaries.size());

		// Collect and print out the metrics from the run, just cause.
		Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
		metricsReporter.report();
	}

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_1} to
	 * verify that no errors are thrown and the correct amount of data is
	 * produced.
	 */
	@Test
	@Ignore("slow (takes 10min), so skipped by default")
	public void verifyCountsFromSample1() {
		/*
		 * FIXME can't I move these into categories or ITs, instead of disabling
		 * the slow test cases?
		 */

		MetricRegistry metrics = new MetricRegistry();
		metrics.registerAll(new MemoryUsageGaugeSet());
		metrics.registerAll(new GarbageCollectorMetricSet());

		SampleDataLoader loader = new SampleDataLoader(new MetricRegistry());
		SynpufArchive archive = SynpufArchive.SAMPLE_1;
		List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);
		Assert.assertEquals(archive.getBeneficiaryCount(), beneficiaries.size());

		// Collect and print out the metrics from the run, just cause.
		Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
		metricsReporter.report();
	}
}
