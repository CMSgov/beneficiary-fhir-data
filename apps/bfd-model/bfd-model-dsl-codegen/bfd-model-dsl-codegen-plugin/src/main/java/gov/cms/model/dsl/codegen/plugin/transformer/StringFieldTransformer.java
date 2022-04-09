package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

public class StringFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator) {
    return transformation.isOptional()
        ? generateBlockForOptional(
            mapping, column, transformation, fromCodeGenerator, toCodeGenerator)
        : generateBlockForRequired(
            mapping, column, transformation, fromCodeGenerator, toCodeGenerator);
  }

  private CodeBlock generateBlockForRequired(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyString($L, $L, $L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.isNullable(),
            mapping.getMinStringLength(),
            column.computeLength(),
            fromCodeGenerator.createGetCall(transformation),
            toCodeGenerator.createSetRef(column))
        .build();
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalString($L, $L, $L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            mapping.getMinStringLength(),
            column.computeLength(),
            fromCodeGenerator.createHasRef(transformation),
            fromCodeGenerator.createGetRef(transformation),
            toCodeGenerator.createSetRef(column))
        .build();
  }
}
