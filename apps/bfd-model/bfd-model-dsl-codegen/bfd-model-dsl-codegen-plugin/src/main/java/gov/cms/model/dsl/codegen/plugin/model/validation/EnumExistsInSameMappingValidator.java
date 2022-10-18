package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom {@link ConstraintValidator} that verifies that the string matches an {@link EnumTypeBean}
 * in the mapping that is currently being validated.
 */
public class EnumExistsInSameMappingValidator
    implements ConstraintValidator<EnumExistsInSameMapping, String> {
  /**
   * Searches for a {@link EnumTypeBean} with the given id in the mapping that is currently being
   * validated. Returns true if such an enum was found, or false otherwise. Null values are treated
   * as valid so that the annotation can be applied to optional fields.
   *
   * @param value object to validate
   * @param context context in which the constraint is evaluated
   * @return true if the string matches the id of a mapping in the model
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null
        || ValidationUtil.getMappingBeanFromContext(context)
            .flatMap(mapping -> mapping.getEnum(value))
            .isPresent();
  }
}
