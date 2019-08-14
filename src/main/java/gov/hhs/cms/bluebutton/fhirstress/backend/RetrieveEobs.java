package gov.hhs.cms.bluebutton.fhirstress.backend;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import gov.hhs.cms.bluebutton.fhirstress.utils.BenefitIdManager;
import gov.hhs.cms.bluebutton.fhirstress.utils.CsvBenefitIdManager;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerUtils;

/**
 * This JMeter sampler will run query for a FHIR {@link ExplanationOfBenefit}
 * using the specified benefit id.
 */
public final class RetrieveEobs extends CustomSamplerClient {
	private BenefitIdManager bim;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		bim = new CsvBenefitIdManager();
	}

	/**
	 * Test Implementation
	 */
	@Override
	protected void executeTest() {
		// Removed the Rif parser to speed things up
//		RifRecordEvent<?> rifRecordEvent = this.parser.next();
//		if (rifRecordEvent != null)
//		{
//		}

		client.search().forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(bim.nextId())))
				.returnBundle(Bundle.class).execute();
	}
}
