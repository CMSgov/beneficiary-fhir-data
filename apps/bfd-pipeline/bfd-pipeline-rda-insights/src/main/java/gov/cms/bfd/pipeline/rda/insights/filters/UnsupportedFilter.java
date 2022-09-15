package gov.cms.bfd.pipeline.rda.insights.filters;

import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;

public class UnsupportedFilter extends PipelineFilter {

    public UnsupportedFilter(ConfigLoader appConfigs) {
        super(appConfigs);
    }

    @Override
    public void run(Pipeline pipeline, PipelineValues pipelineValues) {
        throw new UnsupportedOperationException("Execution of unsupported filter type");
    }

}
