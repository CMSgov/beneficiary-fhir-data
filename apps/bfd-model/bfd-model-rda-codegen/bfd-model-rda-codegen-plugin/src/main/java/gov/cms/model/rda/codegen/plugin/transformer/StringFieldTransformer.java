package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class StringFieldTransformer extends AbstractFieldTransformer {
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
            "$L.copyString($L, $L, 1, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.isNullable(),
            column.computeLength(),
            sourceValue(transformation),
            destSetRef(column))
        .build();
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalString($L, 1, $L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.computeLength(),
            sourceHasRef(transformation),
            sourceGetRef(transformation),
            destSetRef(column))
        .build();
  }
}
