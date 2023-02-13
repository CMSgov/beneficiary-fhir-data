package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link ValidationUtil}. */
public class ValidationUtilTest {
  /** Verify correct responses for various relevant cases. */
  @Test
  public void testIsExactlyOneNotNull() {
    final String nullString = null;
    final String notNullString = "hello";

    assertFalse(ValidationUtil.isExactlyOneNotNull());
    assertFalse(ValidationUtil.isExactlyOneNotNull(nullString));
    assertFalse(ValidationUtil.isExactlyOneNotNull(nullString, nullString));
    assertTrue(ValidationUtil.isExactlyOneNotNull(notNullString, nullString));
    assertTrue(ValidationUtil.isExactlyOneNotNull(nullString, notNullString));
    assertFalse(ValidationUtil.isExactlyOneNotNull(notNullString, notNullString));
  }
}
