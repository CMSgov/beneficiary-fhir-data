package gov.cms.model.dsl.codegen.plugin.transformer;

import static gov.cms.model.dsl.codegen.plugin.transformer.AbstractFieldTransformer.DEST_VAR;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;

public class StandardToCodeGenerator implements ToCodeGenerator {
  public static ToCodeGenerator Instance = new StandardToCodeGenerator();

  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    return CodeBlock.of("$L::set$L", DEST_VAR, TransformerUtil.capitalize(column.getName()));
  }

  @Override
  public CodeBlock createSetCall(ColumnBean column, CodeBlock value) {
    return CodeBlock.builder()
        .addStatement("$L.set$L($L)", DEST_VAR, TransformerUtil.capitalize(column.getName()), value)
        .build();
  }
}
