package gov.hhs.cms.bluebutton.fhirstress.backend;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.hl7.fhir.dstu3.model.Patient;

import gov.hhs.cms.bluebutton.fhirstress.utils.BenefitIdManager;
import gov.hhs.cms.bluebutton.fhirstress.utils.CsvBenefitIdManager;

/**
 * This JMeter sampler will run query for a FHIR {@link Patient} using the
 * specified benefit id.
 */
public final class RetrievePatients extends CustomSamplerClient {
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

		// query a patient record
		client.read().resource(Patient.class).withId(bim.nextId()).execute();
		// }
	}
}
