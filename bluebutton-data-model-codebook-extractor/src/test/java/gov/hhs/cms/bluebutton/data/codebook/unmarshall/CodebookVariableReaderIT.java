package gov.hhs.cms.bluebutton.data.codebook.unmarshall;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import gov.hhs.cms.bluebutton.data.codebook.extractor.CodebookPdfToXmlApp;
import gov.hhs.cms.bluebutton.data.codebook.model.Variable;

/**
 * Integration tests for {@link CodebookVariableReader} (and
 * {@link CodebookPdfToXmlApp}, by extension).
 */
public final class CodebookVariableReaderIT {
	/**
	 * Verifies that {@link CodebookVariableReader#buildVariablesMappedById()} works
	 * as expected.
	 */
	@Test
	public void buildVariablesMappedById() {
		Map<String, Variable> variablesById = CodebookVariableReader.buildVariablesMappedById();
		Assert.assertNotNull(variablesById);
		Assert.assertFalse(variablesById.isEmpty());
	}

	/**
	 * Regression test: verifies that the {@link Variable#getCodebook()} field
	 * unmarshalls as expected.
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
}
