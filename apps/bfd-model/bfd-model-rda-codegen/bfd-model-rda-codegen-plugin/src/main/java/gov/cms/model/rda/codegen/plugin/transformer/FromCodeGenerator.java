package gov.cms.model.rda.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public interface FromCodeGenerator {
  /**
   * Generates a {@code Supplier<Boolean>} compatible CodeBlock that that returns true if the field
   * is present in the message.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for a lambda function
   */
  CodeBlock createHasRef(TransformationBean transformation);

  /**
   * Generates an expression CodeBlock that returns true if the field is present in the message.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for an expression
   */
  CodeBlock createHasCall(TransformationBean transformation);

  /**
   * Generates a {@code Supplier<T>} compatible CodeBlock that that returns the value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for a lambda function
   */
  CodeBlock createGetRef(TransformationBean transformation);

  /**
   * Generates an expression CodeBlock that that returns the value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for an expression
   */
  CodeBlock createGetCall(TransformationBean transformation);
}
