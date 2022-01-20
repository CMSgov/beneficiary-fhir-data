package gov.cms.bfd.model.codebook.data;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link gov.cms.bfd.model.codebook.data.CcwCodebookVariable}. */
public final class CcwCodebookVariableTest {
  /**
   * Verifies that {@link gov.cms.bfd.model.codebook.data.CcwCodebookVariable} was generated as
   * expected.
   */
  @Test
  public void constants() {
    assertTrue(CcwCodebookVariable.values().length > 0);
  }

  /**
   * Verifies that {@link gov.cms.bfd.model.codebook.data.CcwCodebookVariable#getVariable()} works
   * as expected.
   */
  @Test
  public void getVariable() {
    for (CcwCodebookVariable variableEnum : CcwCodebookVariable.values()) {
      assertNotNull(variableEnum.getVariable());
    }
  }
}
