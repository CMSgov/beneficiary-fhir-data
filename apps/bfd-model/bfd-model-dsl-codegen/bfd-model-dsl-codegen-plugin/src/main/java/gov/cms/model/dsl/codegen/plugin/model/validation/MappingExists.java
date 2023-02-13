package gov.cms.model.dsl.codegen.plugin.model.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Constraint annotation for use on fields containing a mapping id. */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = MappingExistsValidator.class)
@Documented
public @interface MappingExists {
  /**
   * Error message used when validation fails.
   *
   * @return error message
   */
  String message() default "must match an existing mapping";

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
