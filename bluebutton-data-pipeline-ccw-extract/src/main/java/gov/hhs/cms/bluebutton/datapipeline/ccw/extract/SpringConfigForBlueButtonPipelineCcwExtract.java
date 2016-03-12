package gov.hhs.cms.bluebutton.datapipeline.ccw.extract;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import gov.hhs.cms.bluebutton.datapipeline.ccw.SpringConfigForBlueButtonPipelineCcw;

/**
 * Spring {@link Configuration} for this project.
 */
@Configuration
@Import(value = { SpringConfigForBlueButtonPipelineCcw.class })
@ComponentScan(basePackageClasses = { SpringConfigForBlueButtonPipelineCcwExtract.class })
public class SpringConfigForBlueButtonPipelineCcwExtract {
}
