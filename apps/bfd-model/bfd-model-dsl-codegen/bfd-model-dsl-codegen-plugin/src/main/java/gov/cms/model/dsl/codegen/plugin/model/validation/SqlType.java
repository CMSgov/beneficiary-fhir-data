package gov.cms.model.dsl.codegen.plugin.model.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Constraint annotation for use on fields containing a sql type name. */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = SqlTypeValidator.class)
@Documented
public @interface SqlType {
  /**
   * Error message used when validation fails.
   *
   * @return error message
   */
  String message() default "must be a supported sql type";

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
