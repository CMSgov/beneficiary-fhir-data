package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hibernate.annotations.common.annotationfactory.AnnotationDescriptor;
import org.hibernate.annotations.common.annotationfactory.AnnotationFactory;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link JavaNameValidator}. */
public class JavaNameValidatorTest {
  /** Verify that null values are considered valid. */
  @Test
  public void shouldAcceptNull() {
    assertTrue(createValidator(JavaNameType.Simple).isValid(null, null));
  }

  /** Verify that only a single part identifier is valid when type is Simple. */
  @Test
  public void shouldValidateSimpleNamesCorrectly() {
    final JavaNameValidator validator = createValidator(JavaNameType.Simple);
    assertTrue(validator.isValid("x", null));
    assertTrue(validator.isValid("_x", null));
    assertTrue(validator.isValid("x1", null));
    assertFalse(validator.isValid("1x", null));
    assertFalse(validator.isValid("a1.b_2", null));
    assertFalse(validator.isValid("a1.b_2.C3", null));
    assertFalse(validator.isValid("", null));
  }

  /** Verify that only one or two part identifiers are valid when type is Property. */
  @Test
  public void shouldValidatePropertyNamesCorrectly() {
    final JavaNameValidator validator = createValidator(JavaNameType.Property);
    assertTrue(validator.isValid("x", null));
    assertTrue(validator.isValid("_x", null));
    assertTrue(validator.isValid("x1", null));
    assertFalse(validator.isValid("1x", null));
    assertTrue(validator.isValid("a1.b_2", null));
    assertFalse(validator.isValid("a1.b_2.C3", null));
    assertFalse(validator.isValid("", null));
  }

  /** Verify that one or more part identifiers are valid when type is Compound. */
  @Test
  public void shouldValidateCompoundNamesCorrectly() {
    final JavaNameValidator validator = createValidator(JavaNameType.Compound);
    assertTrue(validator.isValid("x", null));
    assertTrue(validator.isValid("_x", null));
    assertTrue(validator.isValid("x1", null));
    assertFalse(validator.isValid("1x", null));
    assertTrue(validator.isValid("a1.b_2", null));
    assertTrue(validator.isValid("a1.b_2.C3", null));
    assertFalse(validator.isValid("", null));
  }

  /**
   * Creates an instance of the validator for use in a test. Constructs an instance of the {@link
   * JavaName} annotation with the specified value for its {@link JavaName#type} parameter.
   *
   * @param type value for the type parameter
   * @return the validator
   */
  private JavaNameValidator createValidator(JavaNameType type) {
    AnnotationDescriptor annotationDescriptor = new AnnotationDescriptor(JavaName.class);
    annotationDescriptor.setValue("type", type);
    JavaName annotation = AnnotationFactory.create(annotationDescriptor);
    JavaNameValidator validator = new JavaNameValidator();
    validator.initialize(annotation);
    return validator;
  }
}
