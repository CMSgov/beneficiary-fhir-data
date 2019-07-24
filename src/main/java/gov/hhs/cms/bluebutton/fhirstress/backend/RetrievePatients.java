package gov.hhs.cms.bluebutton.fhirstress.backend;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.hl7.fhir.dstu3.model.Patient;

//import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.fhirstress.utils.BenefitIdMgr;

/**
 * This JMeter sampler will run query for a FHIR {@link Patient} using the
 * specified benefit id.
 */
public final class RetrievePatients extends CustomSamplerClient {
	private BenefitIdMgr bim;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		bim = new BenefitIdMgr(1, 1, 10000, "199900000", "%05d");
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
		client.search().forResource(Patient.class).withIdAndCompartment(bim.nextId(), "").execute();
		// }
	}
}
