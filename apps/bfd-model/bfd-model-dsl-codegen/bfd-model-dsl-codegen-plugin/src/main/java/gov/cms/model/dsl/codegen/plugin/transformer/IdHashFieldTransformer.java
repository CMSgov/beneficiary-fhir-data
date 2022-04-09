package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

public class IdHashFieldTransformer extends AbstractFieldTransformer {
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
    final String value =
        String.format("%s.apply(%s)", HASHER_VAR, fromCodeGenerator.createGetCall(transformation));
    return CodeBlock.builder()
        .addStatement(
            "$L.copyString($L, $L, 1, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.isNullable(),
            column.computeLength(),
            value,
            toCodeGenerator.createSetRef(column))
        .build();
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator) {
    final String valueFunc =
        String.format(
            "()-> %s.apply(%s)", HASHER_VAR, fromCodeGenerator.createGetCall(transformation));
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalString($L, 1, $L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            column.computeLength(),
            fromCodeGenerator.createHasRef(transformation),
            valueFunc,
            toCodeGenerator.createSetRef(column))
        .build();
  }
}
