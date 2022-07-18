package gov.cms.model.dsl.codegen.plugin.accessor;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;

/**
 * Interface for objects that allow generation of code that calls setters on the destination object
 * either directly (a statement) or lazily (a lambda).
 */
public interface Setter {
  /**
   * Generates a {@link java.util.function.Consumer} compatible {@link CodeBlock} that accepts a
   * value and calls the destination object's setter method with that value.
   *
   * @param column column definition for the {@code to} field
   * @return {@link CodeBlock} for a lambda function that sets the {@code to} field value
   */
  CodeBlock createSetRef(ColumnBean column);

  /**
   * Generates a statement {@link CodeBlock} that accepts a value and calls the destination object's
   * setter method with that value.
   *
   * @param column column definition for the {@code to} field
   * @param value {@link CodeBlock} for the value to be passed to the setter
   * @return {@link CodeBlock} for a statement that sets the {@code to} field value
   */
  CodeBlock createSetCall(ColumnBean column, CodeBlock value);
}
