package gov.hhs.cms.bluebutton.datapipeline.ccw;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import gov.hhs.cms.bluebutton.datapipeline.ccw.test.SpringConfigForBlueButtonPipelineCcwTest;

/**
 * Spring {@link Configuration} for this project's unit tests.
 */
@Configuration
@Import(value = { SpringConfigForBlueButtonPipelineCcwTest.class })
public class SpringConfigForTests {
}
