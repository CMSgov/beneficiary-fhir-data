package gov.cms.bfd.pipeline.rda.insights.filters;

import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PROTECTED)
public abstract class PipelineFilter {

    private final String resourceDir;
    private final String outputDir;

    protected PipelineFilter(ConfigLoader appConfigs) {
        resourceDir = appConfigs.stringOption("app.resourceDir").orElseThrow();
        outputDir = appConfigs.stringOption("app.outputDir").orElseThrow();
    }

    abstract public void run(Pipeline pipeline, PipelineValues pipelineValues);

}
