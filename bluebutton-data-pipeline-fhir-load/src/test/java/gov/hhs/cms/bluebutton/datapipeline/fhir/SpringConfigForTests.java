package gov.hhs.cms.bluebutton.datapipeline.fhir;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import gov.hhs.cms.bluebutton.datapipeline.ccw.extract.SpringConfigForBlueButtonPipelineCcwExtract;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.SpringConfigForBlueButtonPipelineCcwTest;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.SpringConfigForBlueButtonPipelineSampleData;

/**
 * Spring {@link Configuration} for this project's unit tests.
 */
@Configuration
@Import(value = { SpringConfigForBlueButtonPipelineFhirLoad.class, SpringConfigForBlueButtonPipelineCcwExtract.class,
		SpringConfigForBlueButtonPipelineCcwTest.class, SpringConfigForBlueButtonPipelineSampleData.class })
public class SpringConfigForTests {
}
