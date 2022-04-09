package gov.cms.model.dsl.codegen.plugin.transformer;

import static gov.cms.model.dsl.codegen.plugin.transformer.AbstractFieldTransformer.DEST_VAR;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import java.util.Optional;

public class OptionalToCodeGenerator extends StandardToCodeGenerator {
  public static final ToCodeGenerator Instance = new OptionalToCodeGenerator();

  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    if (column.isNullable()) {
      return CodeBlock.of(
          "value -> $L.set$L($T.ofNullable(value))",
          DEST_VAR,
          TransformerUtil.capitalize(column.getName()),
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
              "$L.set$L($T.ofNullable(value))",
              DEST_VAR,
              TransformerUtil.capitalize(column.getName()),
              value)
          .build();
    } else {
      return super.createSetCall(column, value);
    }
  }
}
