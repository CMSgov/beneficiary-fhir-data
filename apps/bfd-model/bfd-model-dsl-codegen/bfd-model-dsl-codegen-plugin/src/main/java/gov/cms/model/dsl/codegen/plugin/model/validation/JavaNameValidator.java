package gov.cms.model.dsl.codegen.plugin.model.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates that a given string is a valid java name string. One of several types of names can be
 * validated based on the type parameter (@link JavaNameType} value. Defaults to {@link
 * JavaNameType#Simple}.
 */
public class JavaNameValidator implements ConstraintValidator<JavaName, String> {

  /** Which {@link JavaNameType} to validate. */
  private JavaNameType type = JavaNameType.Simple;

  @Override
  public void initialize(JavaName constraintAnnotation) {
    ConstraintValidator.super.initialize(constraintAnnotation);
    type = constraintAnnotation.type();
  }

  /**
   * Validate that a given string is a valid java identifier or class name string. Null values are
   * treated as valid so that the annotation can be applied to optional fields.
   *
   * @param value string to validate
   * @param context context in which the constraint is evaluated
   * @return true if the value is valid
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || type.getRegex().matcher(value).matches();
  }
}
