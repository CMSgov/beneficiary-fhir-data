package gov.cms.model.rda.codegen.plugin.transformer;

import static gov.cms.model.rda.codegen.plugin.transformer.AbstractFieldTransformer.DEST_VAR;
import static gov.cms.model.rda.codegen.plugin.transformer.TransformerUtil.capitalize;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;

public class StandardToCodeGenerator implements ToCodeGenerator {
  public static ToCodeGenerator Instance = new StandardToCodeGenerator();

  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    return CodeBlock.of("$L::set$L", DEST_VAR, capitalize(column.getName()));
  }

  @Override
  public CodeBlock createSetCall(ColumnBean column, CodeBlock value) {
    return CodeBlock.builder()
        .addStatement("$L.set$L($L)", DEST_VAR, capitalize(column.getName()), value)
        .build();
  }
}
