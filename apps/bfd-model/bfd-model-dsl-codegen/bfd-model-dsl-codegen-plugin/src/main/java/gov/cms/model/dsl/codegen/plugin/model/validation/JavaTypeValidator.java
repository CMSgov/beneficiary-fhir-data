package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates that a given string can be successfully mapped to a java type by the plugin. */
public class JavaTypeValidator implements ConstraintValidator<JavaType, String> {
  /**
   * Validate that the string can be mapped to a java type using the {@link
   * ModelUtil#mapJavaTypeToTypeName} method. Null values are treated as valid so that the
   * annotation can be applied to optional fields.
   *
   * @param value java type name to validate
   * @param context context in which the constraint is evaluated
   * @return true if the value can be mapped
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || ModelUtil.mapJavaTypeToTypeName(value).isPresent();
  }
}
