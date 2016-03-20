package gov.hhs.cms.bluebutton.datapipeline.sampledata;

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

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimLineFact;
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

			long partBFactCount = (long) pm.newJDOQLTypedQuery(PartBClaimFact.class)
					.result(false, QPartBClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactCount > 0L);
			long partBFactLineCount = (long) pm.newJDOQLTypedQuery(PartBClaimLineFact.class)
					.result(false, QPartBClaimLineFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactLineCount > partBFactCount);
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
			Assert.assertTrue(beneficiary.getSurname() != null && beneficiary.getSurname().length() > 0);
			Assert.assertTrue(beneficiary.getContactAddress() != null && beneficiary.getContactAddress().length() > 0);
			Assert.assertTrue(
					beneficiary.getContactAddressZip() != null && beneficiary.getContactAddressZip().length() > 0);

			// Spot check one of the beneficiary's PartAClaimFacts.
			Assert.assertEquals(1, beneficiary.getPartAClaimFacts().size());
			PartAClaimFact partAClaim = beneficiary.getPartAClaimFacts().get(0);
			LOGGER.info("Checking against Part A claim: {}", partAClaim);
			Assert.assertEquals(542192281063886L, (long) partAClaim.getId());
			Assert.assertEquals("V5883", partAClaim.getAdmittingDiagnosisCode());

			// Spot check one of the beneficiary's PartBClaimFacts.
			Assert.assertEquals(5, beneficiary.getPartBClaimFacts().size());
			PartBClaimFact partBClaim = beneficiary.getPartBClaimFacts().get(0);
			LOGGER.info("Checking against Part B claim: {}", partBClaim);
			Assert.assertEquals(887213386947664L, (long) partBClaim.getId());
			Assert.assertSame(beneficiary, partBClaim.getBeneficiary());
			Assert.assertEquals(partBClaim.getId(), partBClaim.getCarrierControlNumber());
			Assert.assertEquals("3598", partBClaim.getDiagnosisCode1());
			Assert.assertEquals("27541", partBClaim.getDiagnosisCode2());
			Assert.assertEquals("", partBClaim.getDiagnosisCode3());
			Assert.assertEquals("", partBClaim.getDiagnosisCode4());
			Assert.assertEquals("", partBClaim.getDiagnosisCode5());
			Assert.assertEquals("", partBClaim.getDiagnosisCode6());
			Assert.assertEquals("", partBClaim.getDiagnosisCode7());
			Assert.assertEquals("", partBClaim.getDiagnosisCode8());
			Assert.assertEquals("", partBClaim.getDiagnosisCode8());
			Assert.assertEquals(1689746125L, (long) partBClaim.getProviderNpi());

			// Spot check one of the beneficiary's PartBClaimLineFacts.
			Assert.assertEquals(1, partBClaim.getClaimLines().size());
			PartBClaimLineFact partBClaimLine = partBClaim.getClaimLines().get(0);
			LOGGER.info("Checking against Part B claim line: {}", partBClaimLine);
			Assert.assertEquals(887213386947664L, (long) partBClaim.getId());
			Assert.assertSame(partBClaim, partBClaimLine.getClaim());
			Assert.assertEquals(1L, partBClaimLine.getLineNumber());
			Assert.assertSame(beneficiary, partBClaimLine.getBeneficiary());
			Assert.assertTrue(partBClaimLine.getProcedure().getId() >= 0);
			Assert.assertEquals("01996", partBClaimLine.getProcedure().getCode());
			Assert.assertEquals(2009, partBClaimLine.getDateFrom().getYear());
			Assert.assertEquals(2009, partBClaimLine.getDateThrough().getYear());
			Assert.assertEquals(Double.valueOf(60.0), partBClaimLine.getAllowedAmount());
			// TODO
			// Assert.assertEquals(null, partBClaimLine.getSubmittedAmount());
			Assert.assertEquals("33818", partBClaimLine.getLineDiagnosisCode());
			// TODO
			// Assert.assertEquals("???", partBClaimLine.getMiscCode().getId());
			// Assert.assertEquals("???",
			// partBClaimLine.getMiscCode().getCode());
			Assert.assertEquals(Double.valueOf(50.0), partBClaimLine.getNchPaymentAmount());
			Assert.assertEquals(Double.valueOf(0.0), partBClaimLine.getBeneficiaryPrimaryPayerPaidAmount());
			Assert.assertEquals(Double.valueOf(10.0), partBClaimLine.getCoinsuranceAmount());
			Assert.assertEquals("A", partBClaimLine.getProcessingIndicationCode());
		}
	}
}
