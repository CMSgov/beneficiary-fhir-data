package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for JavaTypeValidator. */
public class JavaTypeValidatorTest {
  /** Verifies null is valid. */
  @Test
  public void nullShouldBeValid() {
    JavaTypeValidator validator = new JavaTypeValidator();
    assertTrue(validator.isValid(null, null));
  }

  /** Verifies supported type name is valid. */
  @Test
  public void supportedTypeShouldBeValid() {
    JavaTypeValidator validator = new JavaTypeValidator();
    assertTrue(validator.isValid("String", null));
  }

  /** Verifies unsupported type name is not valid. */
  @Test
  public void unsupportedTypeShouldBeInvalid() {
    JavaTypeValidator validator = new JavaTypeValidator();
    assertFalse(validator.isValid("not.a.valid.type", null));
  }
}
