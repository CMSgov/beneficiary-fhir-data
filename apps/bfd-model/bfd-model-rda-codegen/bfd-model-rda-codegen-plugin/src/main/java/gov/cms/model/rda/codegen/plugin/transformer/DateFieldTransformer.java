package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class DateFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return transformation.isOptional()
        ? generateBlockForOptional(mapping, column, transformation)
        : generateBlockForRequired(mapping, column, transformation);
  }

  private CodeBlock generateBlockForRequired(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyDate($L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.isNullable(),
            sourceValue(transformation),
            destSetRef(column))
        .build();
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalDate($L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            sourceHasRef(transformation),
            sourceGetRef(transformation),
            destSetRef(column))
        .build();
  }
}
