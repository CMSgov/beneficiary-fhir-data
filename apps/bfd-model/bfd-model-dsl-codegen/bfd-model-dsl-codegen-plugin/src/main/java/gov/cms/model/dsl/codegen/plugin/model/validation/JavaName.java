package gov.cms.model.dsl.codegen.plugin.model.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Constraint annotation for use on fields containing an id. Ensures that the id value is acceptable
 * to the java compiler.
 */
@Target({FIELD, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = JavaNameValidator.class)
@Documented
public @interface JavaName {
  /**
   * Error message used when validation fails.
   *
   * @return error message
   */
  String message() default "must be a valid Java {type} name";

  /**
   * What type of name is being validated.
   *
   * @return valid {@link JavaNameType}
   */
  JavaNameType type() default JavaNameType.Simple;

  /**
   * Only present because validator requires it.
   *
   * @return any groups for this validation
   */
  Class<?>[] groups() default {};

  /**
   * Only present because validator requires it.
   *
   * @return any payload for this validation
   */
  Class<? extends Payload>[] payload() default {};
}
