package gov.cms.bfd.pipeline.rda.insights;

import gov.cms.bfd.pipeline.rda.insights.filters.PipelineFilter;
import gov.cms.bfd.pipeline.rda.insights.filters.ReadCSVFilter;
import gov.cms.bfd.pipeline.rda.insights.filters.SQLFilter;
import gov.cms.bfd.pipeline.rda.insights.filters.UnsupportedFilter;
import gov.cms.bfd.pipeline.rda.insights.filters.WriteXLSXFilter;
import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class RdaInsights {

    private final ConfigLoader appConfigs;
    private final Set<Pipeline> pipelines;
    private final Map<Pipeline.Type, PipelineFilter> pipelineFilters = new EnumMap<>(Pipeline.Type.class);

    private PipelineFilter UNKNOWN_FILTER;

    public void init() {
        if (UNKNOWN_FILTER == null) {
            UNKNOWN_FILTER = new UnsupportedFilter(appConfigs);
        }

        pipelineFilters.put(Pipeline.Type.SQL, new SQLFilter(appConfigs));
        pipelineFilters.put(Pipeline.Type.READ_CSV, new ReadCSVFilter(appConfigs));
        pipelineFilters.put(Pipeline.Type.WRITE_XLSX, new WriteXLSXFilter(appConfigs));
    }

    public void run() {
        for (Pipeline pipeline : pipelines) {
            PipelineValues pipelineValues = new PipelineValues();

            while (pipeline != null) {
                pipelineFilters.getOrDefault(pipeline.getType(), UNKNOWN_FILTER).run(pipeline, pipelineValues);

                pipeline = pipeline.getPipeTo();
            }
        }
    }

}
