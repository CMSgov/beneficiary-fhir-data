package gov.cms.model.dsl.codegen.plugin.model.validation;

import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import gov.cms.model.dsl.codegen.plugin.transformer.FieldTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Custom {@link ConstraintValidator} that verifies that the string matches a transformation
 * recognized by {@link TransformerUtil}.
 */
public class TransformerNameValidator implements ConstraintValidator<TransformerName, String> {
  /**
   * Validate that the transformer name can be mapped to a {@link FieldTransformer} using the {@link
   * TransformerUtil#getFieldTransformer} method. Null values are treated as valid so that the
   * annotation can be applied to optional fields.
   *
   * @param value object to validate
   * @param context context in which the constraint is evaluated
   * @return true if the string matches the name of a {@link FieldTransformer}
   */
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null
        || TransformationBean.ArrayTransformName.equals(value)
        || TransformerUtil.getFieldTransformer(value).isPresent();
  }
}
