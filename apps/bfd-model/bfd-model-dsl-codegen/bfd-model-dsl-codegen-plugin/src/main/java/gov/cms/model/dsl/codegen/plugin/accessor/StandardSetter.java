package gov.cms.model.dsl.codegen.plugin.accessor;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.transformer.FieldTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;

/**
 * {@link Setter} implementation that generates code to set the value of a field using an unwrapped
 * value.
 */
public class StandardSetter implements Setter {
  /** Singleton instance of this class that can be used everywhere. */
  public static final Setter Instance = new StandardSetter();

  /**
   * {@inheritDoc}
   *
   * <p>This implementation accepts a value and passes it to the setter as-is.
   */
  @Override
  public CodeBlock createSetRef(ColumnBean column) {
    return CodeBlock.of(
        "$L::set$L", FieldTransformer.DEST_VAR, TransformerUtil.capitalize(column.getName()));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation accepts a value and passes it to the setter as-is.
   */
  @Override
  public CodeBlock createSetCall(ColumnBean column, CodeBlock value) {
    return CodeBlock.builder()
        .addStatement(
            "$L.set$L($L)",
            FieldTransformer.DEST_VAR,
            TransformerUtil.capitalize(column.getName()),
            value)
        .build();
  }
}
