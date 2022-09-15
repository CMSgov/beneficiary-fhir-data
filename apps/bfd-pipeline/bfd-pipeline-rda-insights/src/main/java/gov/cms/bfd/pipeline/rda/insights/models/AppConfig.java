package gov.cms.bfd.pipeline.rda.insights.models;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.util.Set;

@Data
@FieldNameConstants
public class AppConfig {

    private DbConfig db;
    private String resourceDir;
    private String outputDir;
    private Set<Pipeline> pipelines;

}
