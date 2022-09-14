package gov.cms.bfd.pipeline.rda.insights.models;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants
public class DbConfig {

    private String url;
    private String username;
    private String password;
    private String schema;
    private String driver;
    private String dialect;

}
