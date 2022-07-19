package gov.cms.model.dsl.codegen.plugin.accessor;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/**
 * Interface for objects that generate code to determine if a field in a source object has a value
 * and also to get the value from the field. Both types of calls can be generated as immediate code
 * (expression {@link CodeBlock}) or lazily (lambda {@link CodeBlock}).
 */
public interface Getter {
  /**
   * Generates a {@link java.util.function.Supplier} compatible {@link CodeBlock} that returns true
   * if the field is present in the message.
   *
   * @param transformation defines the {@code from} field
   * @return {@link CodeBlock} for a lambda function
   */
  CodeBlock createHasRef(TransformationBean transformation);

  /**
   * Generates an expression {@link CodeBlock} that returns true if the field is present in the
   * message.
   *
   * @param transformation defines the {@code from} field
   * @return {@link CodeBlock} for an expression
   */
  CodeBlock createHasCall(TransformationBean transformation);

  /**
   * Generates a {@link java.util.function.Supplier} compatible {@link CodeBlock} that returns the
   * value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return {@link CodeBlock} for a lambda function
   */
  CodeBlock createGetRef(TransformationBean transformation);

  /**
   * Generates an expression {@link CodeBlock} that returns the value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return {@link CodeBlock} for an expression
   */
  CodeBlock createGetCall(TransformationBean transformation);
}
