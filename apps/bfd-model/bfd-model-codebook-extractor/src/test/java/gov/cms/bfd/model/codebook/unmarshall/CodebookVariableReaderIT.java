package gov.cms.bfd.model.codebook.unmarshall;

import gov.cms.bfd.model.codebook.extractor.CodebookPdfToXmlApp;
import gov.cms.bfd.model.codebook.model.Variable;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Integration tests for {@link gov.cms.bfd.model.codebook.unmarshall.CodebookVariableReader} (and
 * {@link CodebookPdfToXmlApp}, by extension).
 */
public final class CodebookVariableReaderIT {
  /**
   * Verifies that {@link
   * gov.cms.bfd.model.codebook.unmarshall.CodebookVariableReader#buildVariablesMappedById()} works
   * as expected.
   */
  @Test
  public void buildVariablesMappedById() {
    Map<String, Variable> variablesById = CodebookVariableReader.buildVariablesMappedById();
    Assert.assertNotNull(variablesById);
    Assert.assertFalse(variablesById.isEmpty());

    for (Variable variable : variablesById.values()) assertVariableIsFixed(variable);
  }

  /**
   * Regression test: verifies that the {@link Variable#getCodebook()} field unmarshalls as
   * expected.
   */
  @Test
  public void unmarshalling_variableCodebookField() {
    Map<String, Variable> variablesById = CodebookVariableReader.buildVariablesMappedById();
    for (Variable variable : variablesById.values()) {
      Assert.assertNotNull(variable.getCodebook());
      Assert.assertNotNull(variable.getCodebook().getId());
      Assert.assertNotNull(variable.getCodebook().getName());
      Assert.assertNotNull(variable.getCodebook().getVersion());
    }
  }

  /**
   * Asserts that the specified {@link Variable} has been fixed (if it needed to be).
   *
   * @param variable the {@link Variable} to validate
   */
  private static void assertVariableIsFixed(Variable variable) {
    /*
     * As fixes are added to CodebookPdfToXmlApp, an assertion method for each fix
     * should be added here.
     */

    assertTypoFixForNchClmPrvdtPmtAmt(variable);
  }

  /** @param variable the {@link Variable} to check */
  private static void assertTypoFixForNchClmPrvdtPmtAmt(Variable variable) {
    Assert.assertNotEquals("NCH_CLM_PRVDT_PMT_AMT", variable.getId());
  }
}
