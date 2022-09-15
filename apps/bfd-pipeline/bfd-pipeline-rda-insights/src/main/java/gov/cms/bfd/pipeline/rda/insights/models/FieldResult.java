package gov.cms.bfd.pipeline.rda.insights.models;

import lombok.Data;

@Data
public class FieldResult<T> {

    private final String columnName;
    private final Class<T> valueType;
    private final T value;

}
