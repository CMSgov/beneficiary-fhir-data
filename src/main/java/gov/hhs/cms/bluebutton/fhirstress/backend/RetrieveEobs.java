package gov.hhs.cms.bluebutton.fhirstress.backend;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Coverage;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerUtils;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.MedicareSegment;

import gov.hhs.cms.bluebutton.fhirstress.utils.BenefitIdMgr;

/**
 * This JMeter sampler will run a search for a random FHIR {@link Patient} and
 * then retrieve that {@link Patient} and all of their
 * {@link ExplanationOfBenefit}s.
 */
public final class RetrieveEobs extends CustomSamplerClient {
	private Random rng = new Random();
	private List<String> patientIds;
  private BenefitIdMgr bim; 

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
    bim = new BenefitIdMgr(1,1,10000,"201400000","%05d"); 
	}

	/**
	 * Test Implementation 
	 */
  @Override
	protected void executeTest() {
    // Removed the Rif parser to speed things up
    //if (this.parser.hasNext()) 
    //{  
      //RifEntry entry = this.parser.next();

      // query all EOBs for a patient
		  Bundle searchResults = client.search()
        .forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(bim.nextId())))
				.returnBundle(Bundle.class)
        .execute();
    //}
	}
}
