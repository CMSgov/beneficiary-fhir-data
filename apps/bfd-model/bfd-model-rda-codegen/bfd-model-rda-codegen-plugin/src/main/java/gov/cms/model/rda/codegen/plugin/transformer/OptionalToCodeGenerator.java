package gov.cms.model.rda.codegen.plugin.transformer;

import static gov.cms.model.rda.codegen.plugin.transformer.AbstractFieldTransformer.DEST_VAR;
import static gov.cms.model.rda.codegen.plugin.transformer.TransformerUtil.capitalize;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.ColumnBean;
import java.util.Optional;

public class OptionalToCodeGenerator extends StandardToCodeGenerator {
  public static final ToCodeGenerator Instance = new OptionalToCodeGenerator();

  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    if (column.isNullable()) {
      return CodeBlock.of(
          "value -> $L.set$L($T.ofNullable(value))",
          DEST_VAR,
          capitalize(column.getName()),
          Optional.class);
    } else {
      return super.createSetRef(column);
    }
  }

  @Override
  public CodeBlock createSetCall(ColumnBean column, CodeBlock value) {
    if (column.isNullable()) {
      return CodeBlock.builder()
          .addStatement(
              "$L.set$L($T.ofNullable(value))", DEST_VAR, capitalize(column.getName()), value)
          .build();
    } else {
      return super.createSetCall(column, value);
    }
  }
}
