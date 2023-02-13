package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import org.junit.jupiter.api.Test;

/** Unit tests for TransformerNameValidator. */
public class TransformerNameValidatorTest {
  /** Verifies null is valid. */
  @Test
  public void nullShouldBeValid() {
    TransformerNameValidator validator = new TransformerNameValidator();
    assertTrue(validator.isValid(null, null));
  }

  /** Verifies supported transformer name is valid. */
  @Test
  public void supportedTransformerNameShouldBeValid() {
    TransformerNameValidator validator = new TransformerNameValidator();
    assertTrue(validator.isValid(TransformerUtil.IdHashTransformName, null));
  }

  /** Verifies unsupported transformer name is not valid. */
  @Test
  public void unsupportedTransformerNameShouldBeInvalid() {
    TransformerNameValidator validator = new TransformerNameValidator();
    assertFalse(validator.isValid("SmashIntoPieces", null));
  }
}
