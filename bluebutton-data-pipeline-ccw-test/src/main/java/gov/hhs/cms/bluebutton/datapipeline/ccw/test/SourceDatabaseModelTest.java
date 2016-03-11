package gov.hhs.cms.bluebutton.datapipeline.ccw.test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForBlueButtonPipelineCcwTest.class })
@RunWith(Parameterized.class)
public final class SourceDatabaseModelTest {
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
	 * Verifies that the relationship between {@link CurrentBeneficiary} and
	 * {@link PartAClaimFact} works as expected.
	 */
	@Test
	public void beneficiaryClaimsPartA() {
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
				PartAClaimFact factB = new PartAClaimFact().setId(1L).setBeneficiary(beneA)
						.setAdmittingDiagnosisCode("foo");
				beneA.getPartAClaimFacts().add(factB);
				pm.makePersistent(beneA);
				tx.commit();
			} finally {
				if (tx != null && tx.isActive())
					tx.rollback();
			}

			// Now, retrieve those objects and verify them.
			List<CurrentBeneficiary> benes = pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.filter(QCurrentBeneficiary.candidate().id.eq(0)).executeList();
			Assert.assertEquals(1, benes.size());
			Assert.assertEquals(2, benes.get(0).getPartAClaimFacts().size());
			Assert.assertSame(benes.get(0), benes.get(0).getPartAClaimFacts().get(0).getBeneficiary());
			Assert.assertSame(benes.get(0), benes.get(0).getPartAClaimFacts().get(1).getBeneficiary());
			List<PartAClaimFact> partAClaims = pm.newJDOQLTypedQuery(PartAClaimFact.class).executeList();
			Assert.assertEquals(2, partAClaims.size());
		}
	}
}
