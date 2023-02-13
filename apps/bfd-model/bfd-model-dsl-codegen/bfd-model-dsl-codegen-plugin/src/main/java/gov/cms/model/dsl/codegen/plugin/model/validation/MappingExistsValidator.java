package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom {@link ConstraintValidator} that verifies that the string matches the {@link
 * MappingBean#id} of a mapping in the model.
 */
public class MappingExistsValidator implements ConstraintValidator<MappingExists, String> {
  /**
   * Validates that the name corresponds to the {@link MappingBean#id} of a mapping in the {@link
   * RootBean}. Null values are treated as valid so that the annotation can be applied to optional
   * fields.
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
