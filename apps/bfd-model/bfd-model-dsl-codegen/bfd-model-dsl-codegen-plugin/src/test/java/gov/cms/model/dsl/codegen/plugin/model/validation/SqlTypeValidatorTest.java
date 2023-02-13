package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for SqlTypeValidator. */
public class SqlTypeValidatorTest {
  /** Verifies null is valid. */
  @Test
  public void nullShouldBeValid() {
    SqlTypeValidator validator = new SqlTypeValidator();
    assertTrue(validator.isValid(null, null));
  }

  /** Verifies supported type name is valid. */
  @Test
  public void supportedTypeShouldBeValid() {
    SqlTypeValidator validator = new SqlTypeValidator();
    assertTrue(validator.isValid("varchar(10)", null));
  }

  /** Verifies unsupported type name is not valid. */
  @Test
  public void unsupportedTypeShouldBeInvalid() {
    SqlTypeValidator validator = new SqlTypeValidator();
    assertFalse(validator.isValid("atomicweight", null));
  }
}
