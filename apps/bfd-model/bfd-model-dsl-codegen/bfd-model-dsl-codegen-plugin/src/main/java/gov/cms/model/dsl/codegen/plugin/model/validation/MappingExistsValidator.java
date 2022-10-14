package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom {@link ConstraintValidator} that verifies that the string matches the {@link
 * MappingBean#id} of a mapping in the model.
 */
public class MappingExistsValidator implements ConstraintValidator<MappingExists, String> {
  /**
   * Searches for a {@link MappingBean} with the given id in the model that is currently being
   * validated. Returns true if such a mapping was found, or false otherwise.
   *
   * @param value object to validate
   * @param context context in which the constraint is evaluated
   * @return true if the string matches the id of a mapping in the model
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null
        || ValidationUtil.getRootBeanFromContext(context)
            .flatMap(root -> root.findMappingWithId(value))
            .isPresent();
  }
}
