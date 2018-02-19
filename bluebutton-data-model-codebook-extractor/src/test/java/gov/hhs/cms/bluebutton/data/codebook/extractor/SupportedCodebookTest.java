package gov.hhs.cms.bluebutton.data.codebook.extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.hhs.cms.bluebutton.data.codebook.model.Codebook;
import gov.hhs.cms.bluebutton.data.codebook.model.Value;
import gov.hhs.cms.bluebutton.data.codebook.model.ValueGroup;
import gov.hhs.cms.bluebutton.data.codebook.model.Variable;

/**
 * Tests for the parsed {@link SupportedCodebook} data. Not exactly unit tests,
 * but whatever.
 */
public class SupportedCodebookTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SupportedCodebookTest.class);

	/**
	 * Parses all of the {@link SupportedCodebook}s using {@link PdfParser}, looking
	 * for duplicate {@link Variable}s.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void findDuplicateVariables() throws IOException {
		Map<String, List<Variable>> variablesById = new LinkedHashMap<>();

		// Build the map of Variable IDs to the Codebooks they're seen in.
		for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
			Codebook codebook = PdfParser.parseCodebookPdf(supportedCodebook);
			for (Variable variable : codebook.getVariables()) {
				if (!variablesById.containsKey(variable.getId()))
					variablesById.put(variable.getId(), new ArrayList<>());

				variablesById.get(variable.getId()).add(variable);
			}
		}

		// Find all of the variables that appear in more than one Codebook.
		List<String> duplicatedVariableIds = new ArrayList<>();
		for (String variableId : variablesById.keySet()) {
			List<Variable> variables = variablesById.get(variableId);
			if (variables.size() > 1)
				duplicatedVariableIds.add(variableId);
		}

		// Log a detailed warning for each duplicate.
		for (String duplicatedVariableId : duplicatedVariableIds) {
			List<Variable> duplicatedVariables = variablesById.get(duplicatedVariableId);
			LOGGER.warn("The variable '{}' appears more than once: {}.", duplicatedVariableId, duplicatedVariables);
		}

		/*
		 * We know that these variables are duplicated. It isn't great, but oh well. We
		 * assert their presence just to get alerted if something interesting changes
		 * with the data or parsing.
		 */
		Assert.assertTrue(duplicatedVariableIds.contains("BENE_ID"));
		Assert.assertTrue(duplicatedVariableIds.contains("DOB_DT"));
		Assert.assertTrue(duplicatedVariableIds.contains("GNDR_CD"));

		// Blow up if anything more than those known problems appears.
		Assert.assertEquals(3, duplicatedVariableIds.size());
	}

	/**
	 * Parses all of the {@link SupportedCodebook}s using {@link PdfParser}, looking
	 * for duplicate {@link Value#getCode()}s within each {@link Variable}.
	 * 
	 * @throws IOException
	 *             Indicates test error.
	 */
	@Test
	public void findDuplicateCodes() throws IOException {
		for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
			Codebook codebook = PdfParser.parseCodebookPdf(supportedCodebook);
			for (Variable variable : codebook.getVariables()) {
				if (!variable.getValueGroups().isPresent())
					continue;

				// Build a multimap of all the Values by their codes.
				Map<String, List<Value>> valuesByCode = new LinkedHashMap<>();
				for (ValueGroup valueGroup : variable.getValueGroups().get()) {
					for (Value value : valueGroup.getValues()) {
						if (!valuesByCode.containsKey(value.getCode()))
							valuesByCode.put(value.getCode(), new ArrayList<>());

						valuesByCode.get(value.getCode()).add(value);
					}
				}

				// Find all of the codes that appear in more than one Value.
				List<String> duplicatedCodes = new ArrayList<>();
				for (String code : valuesByCode.keySet()) {
					List<Value> values = valuesByCode.get(code);
					if (values.size() > 1)
						duplicatedCodes.add(code);
				}

				// Log a detailed warning for each duplicate.
				for (String duplicatedCode : duplicatedCodes) {
					List<Value> duplicatedValues = valuesByCode.get(duplicatedCode);
					LOGGER.warn("The code '{}' appears more than once in Variable '{}': {}.", duplicatedCode, variable,
							duplicatedValues);
				}
			}
		}
	}
}
