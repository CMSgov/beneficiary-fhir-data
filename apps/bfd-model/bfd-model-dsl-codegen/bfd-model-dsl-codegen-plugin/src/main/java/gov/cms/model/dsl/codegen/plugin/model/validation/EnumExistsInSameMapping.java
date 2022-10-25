package gov.cms.model.dsl.codegen.plugin.model.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Constraint annotation for use on {@link ColumnBean#enumType}. to validate that the specified enum
 * exists in the same {@link MappingBean} as the {@link ColumnBean}.
 */
@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = EnumExistsInSameMappingValidator.class)
@Documented
public @interface EnumExistsInSameMapping {
  /**
   * Error message used when validation fails.
   *
   * @return error message
   */
  String message() default "must match an enum in same mapping as this column";

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
