package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.TearDownAcceptor;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class SampleDataLoaderTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataLoaderTest.class);

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public final TearDownAcceptor tearDown = new TearDownAcceptor();

	@Inject
	public CcwTestHelper ccwHelper;

	@Parameters
	public static Iterable<Object> createTestParameters() {
		return Arrays.asList(new HsqlProvisioningRequest("tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	/**
	 * Verifies that {@link SampleDataLoader} loads the expected number of
	 * records when run against {@link SynpufArchive#SAMPLE_TEST_A}.
	 */
	@Test
	public void verifyCountsForSampleTestA() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			loader.loadSampleData(Paths.get(".", "target"), archive);

			Assert.assertEquals(archive.getBeneficiaryCount(), pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.result(false, QCurrentBeneficiary.candidate().count()).executeResultUnique());

			long partAFactCount = (long) pm.newJDOQLTypedQuery(PartAClaimFact.class)
					.result(false, QPartAClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partAFactCount > 0L);
			long partARevLineFactCount = (long) pm.newJDOQLTypedQuery(PartAClaimRevLineFact.class)
					.result(false, QPartAClaimRevLineFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partARevLineFactCount > 0L);

			long partBFactCount = (long) pm.newJDOQLTypedQuery(PartBClaimFact.class)
					.result(false, QPartBClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactCount > 0L);
			long partBFactLineCount = (long) pm.newJDOQLTypedQuery(PartBClaimLineFact.class)
					.result(false, QPartBClaimLineFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactLineCount > partBFactCount);

			long partDFactCount = (long) pm.newJDOQLTypedQuery(PartDEventFact.class)
					.result(false, QPartDEventFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partDFactCount > 0L);
		}
	}

	/**
	 * Spot-checks a single loaded beneficiary from
	 * {@link SynpufArchive#SAMPLE_TEST_A} to verify that
	 * {@link SampleDataLoader} is handling fields as expected.
	 */
	@Test
	public void spotCheckBeneficiaryFromSampleTestA() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			loader.loadSampleData(Paths.get(".", "target"), archive);

			// Grab the beneficiary to spot-check.
			CurrentBeneficiary beneficiary = pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.filter(QCurrentBeneficiary.candidate().id.eq(0)).executeUnique();

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
			PartAClaimFact inpatientClaim = beneficiary.getPartAClaimFacts().get(0);
			LOGGER.info("Checking against inpatient claim: {}", inpatientClaim);
			Assert.assertEquals(196661176988405L, (long) inpatientClaim.getId());
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
			PartAClaimFact outpatientClaim = beneficiary.getPartAClaimFacts().get(1);
			LOGGER.info("Checking against outpatient claim: {}", outpatientClaim);
			Assert.assertEquals(542192281063886L, (long) outpatientClaim.getId());
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
			PartBClaimFact carrierClaim = beneficiary.getPartBClaimFacts().get(0);
			LOGGER.info("Checking against carrier claim: {}", carrierClaim);
			Assert.assertEquals(887213386947664L, (long) carrierClaim.getId());
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
			Assert.assertEquals(188, beneficiary.getPartDEventFacts().size());
			PartDEventFact partDEvent = beneficiary.getPartDEventFacts().get(0);
			LOGGER.info("Checking against Part D event: {}", partDEvent);
			Assert.assertEquals(233024488623927L, (long) partDEvent.getId());
			Assert.assertEquals(1487603916L, (long) partDEvent.getPrescriberNpi());
			Assert.assertEquals(1700826658L, (long) partDEvent.getServiceProviderNpi());
			Assert.assertEquals(54868407904L, (long) partDEvent.getProductNdc());
			Assert.assertEquals(2010, partDEvent.getServiceDate().getYear());
			Assert.assertEquals(30L, (long) partDEvent.getQuantityDispensed());
			Assert.assertEquals(30L, (long) partDEvent.getNumberDaysSupply());
			Assert.assertEquals(0.0, (double) partDEvent.getPatientPayAmount(), 0.0);
			Assert.assertEquals(180.0, (double) partDEvent.getTotalPrescriptionCost(), 0.0);
		}
	}
}
