package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validates that a given string can be successfully mapped to a sql type. */
public class SqlTypeValidator implements ConstraintValidator<SqlType, String> {
  /**
   * Validate that the string can be mapped to a sql type using the {@link
   * ModelUtil#mapSqlTypeToTypeName} method. Null values are treated as valid so that the annotation
   * can be applied to optional fields.
   *
   * @param value sql type name to validate
   * @param context context in which the constraint is evaluated
   * @return true if the value can be mapped
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || ModelUtil.mapSqlTypeToTypeName(value).isPresent();
  }
}
