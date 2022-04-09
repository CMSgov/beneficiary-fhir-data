package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import gov.cms.model.rda.codegen.plugin.model.MappingBean;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class CharFieldTransformer extends AbstractFieldTransformer {
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
            "$L.copyCharacter($L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
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
            "$L.copyOptionalCharacter($L, $L, $L, $L)",
            TRANSFORMER_VAR,
            fieldNameReference(mapping, column),
            fromCodeGenerator.createHasRef(transformation),
            fromCodeGenerator.createGetRef(transformation),
            toCodeGenerator.createSetRef(column))
        .build();
  }
}
