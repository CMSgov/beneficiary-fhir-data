package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

public class IntFieldTransformer extends AbstractFieldTransformer {
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
        : generateBlockForRequired();
  }

  private CodeBlock generateBlockForRequired() {
    throw new IllegalArgumentException("non-optional ints are not currently supported");
  }

  private CodeBlock generateBlockForOptional(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator) {
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalInt($L, $L, $L)",
            TRANSFORMER_VAR,
            fromCodeGenerator.createHasRef(transformation),
            fromCodeGenerator.createGetRef(transformation),
            toCodeGenerator.createSetRef(column))
        .build();
  }
}
