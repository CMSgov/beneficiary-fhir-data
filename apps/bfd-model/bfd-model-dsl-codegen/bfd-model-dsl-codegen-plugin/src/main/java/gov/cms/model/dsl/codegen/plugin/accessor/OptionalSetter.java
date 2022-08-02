package gov.cms.model.dsl.codegen.plugin.accessor;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.transformer.FieldTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import java.util.Optional;

/**
 * {@link Setter} implementation that generates code to set the value of a field using an {@link
 * Optional} to wrap the value.
 */
public class OptionalSetter extends StandardSetter {
  /** Sharable singleton instance. */
  public static final Setter Instance = new OptionalSetter();

  /**
   * {@inheritDoc}
   *
   * <p>This implementation accepts an unwrapped value and passes it to the setter wrapped in an
   * {@link Optional}.
   */
  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    if (column.isNullable()) {
      return CodeBlock.of(
          "value -> $L.set$L($T.ofNullable(value))",
          FieldTransformer.DEST_VAR,
          TransformerUtil.capitalize(column.getName()),
          Optional.class);
    } else {
      return super.createSetRef(column);
    }
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation accepts an unwrapped value and passes it to the setter wrapped in an
   * {@link Optional}.
   */
  @Override
  public CodeBlock createSetCall(ColumnBean column, CodeBlock value) {
    if (column.isNullable()) {
      return CodeBlock.builder()
          .addStatement(
              "$L.set$L($T.ofNullable($L))",
              FieldTransformer.DEST_VAR,
              TransformerUtil.capitalize(column.getName()),
              Optional.class,
              value)
          .build();
    } else {
      return super.createSetCall(column, value);
    }
  }
}
