package gov.hhs.cms.bluebutton.data.codebook.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link CcwCodebookVariable}.
 */
public final class CcwCodebookVariableTest {
	/**
	 * Verifies that {@link CcwCodebookVariable} was generated as expected.
	 */
	@Test
	public void constants() {
		Assert.assertTrue(CcwCodebookVariable.values().length > 0);
	}

	/**
	 * Verifies that {@link CcwCodebookVariable#getVariable()} works as expected.
	 */
	@Test
	public void getVariable() {
		for (CcwCodebookVariable variableEnum : CcwCodebookVariable.values()) {
			Assert.assertNotNull(variableEnum.getVariable());
		}
	}
}
