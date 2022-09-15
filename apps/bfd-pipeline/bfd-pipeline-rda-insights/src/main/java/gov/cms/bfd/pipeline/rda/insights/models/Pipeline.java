package gov.cms.bfd.pipeline.rda.insights.models;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class Pipeline {

    private Type type;
    private String target;
    private Pipeline pipeTo;
    private String label;

    public enum Type {
        SQL,
        READ_CSV,
        WRITE_XLSX
    }

}
