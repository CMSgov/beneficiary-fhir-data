package gov.hhs.cms.bluebutton.datapipeline.ccw.extract;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;

/**
 * Unit tests for {@link CcwExtractor}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class CcwExtractorTest {
	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	@Inject
	public CcwTestHelper ccwHelper;

	@Parameters
	public static Iterable<Object> createTestParameters() {
		return Arrays.asList(new HsqlProvisioningRequest("tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	/**
	 * Verifies that {@link CcwExtractor} works correctly under normal
	 * circumstances.
	 */
	@Test
	public void normalUsage() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Create some model objects and persist them.
			Transaction tx = pm.currentTransaction();
			try {
				tx.begin();
				CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now());
				PartAClaimFact factA = new PartAClaimFact().setId(0L).setBeneficiary(beneA)
						.setAdmittingDiagnosisCode("foo");
				beneA.getPartAClaimFacts().add(factA);
				pm.makePersistent(beneA);
				CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
				PartAClaimFact factB = new PartAClaimFact().setId(1L).setBeneficiary(beneB)
						.setAdmittingDiagnosisCode("foo");
				beneB.getPartAClaimFacts().add(factB);
				pm.makePersistent(beneB);
				tx.commit();
			} finally {
				if (tx != null && tx.isActive())
					tx.rollback();
			}

			/*
			 * Run the extractor and verify the results. Streams are one-shot so
			 * we'll run it each time.
			 */
			CcwExtractor extractor = new CcwExtractor(pm);
			Stream<CurrentBeneficiary> beneficiariesStream = extractor.extractAllBeneficiaries();

			Assert.assertNotNull(beneficiariesStream);
			Assert.assertFalse(beneficiariesStream.isParallel());
			List<CurrentBeneficiary> beneficiariesList = beneficiariesStream.collect(Collectors.toList());
			Assert.assertEquals(2, beneficiariesList.size());
		}
	}
}
