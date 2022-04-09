package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;

public interface ToCodeGenerator {
  /**
   * Generates a {@code Consumer<T>} compatible CodeBlock that accepts a value and calls the target
   * object's setter method.
   *
   * @param column column definition for the {@code to} field
   * @param transformation defines the {@code to} field
   * @return CodeBlock for a lambda function
   */
  CodeBlock createSetRef(ColumnBean column);

  /**
   * Generates a statement CodeBlock that accepts a value and calls * the target object's setter
   * method.
   *
   * @param column column definition for the {@code to} field
   * @param transformation defines the {@code to} field
   * @param value CodeBlock for the value to be passed to the setter
   * @return CodeBlock for an expression
   */
  CodeBlock createSetCall(ColumnBean column, CodeBlock value);
}
