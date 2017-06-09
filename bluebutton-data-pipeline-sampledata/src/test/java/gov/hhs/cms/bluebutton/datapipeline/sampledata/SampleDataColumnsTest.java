package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.BeneficiaryRow;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.CarrierClaimGroup;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;
import gov.hhs.cms.bluebutton.datapipeline.rif.parse.RifParsingUtils;

/**
 * Verifies that the columns in the sample data match the columns in the various
 * RIF column enums in our Java ETL code, e.g. {@link BeneficiaryRow.Column},
 * {@link CarrierClaimGroup.Column}.
 */
public final class SampleDataColumnsTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataColumnsTest.class);

	/**
	 * No assertions here: it just logs out the enum columns for posterity and
	 * other uses.
	 */
	@Test
	public void logEnumColumns() {
		for (RifFileType rifFileType : RifFileType.values()) {
			Enum<?>[] columnsInEnum = getColumnsInEnum(rifFileType);
			LOGGER.info("Enum columns for '{}': {}", columnsInEnum[0].getDeclaringClass().getName(),
					toHeaderFormat(columnsInEnum, c -> c.name()));
		}
	}

	/**
	 * Checks our code's columns against the
	 * {@link StaticRifResourceGroup#SAMPLE_A} data file headers.
	 */
	@Test
	public void verifySampleAColumns() {
		verifyColumns(StaticRifResourceGroup.SAMPLE_A);
	}

	/**
	 * Checks our code's columns against the
	 * {@link StaticRifResourceGroup#SAMPLE_B} data file headers.
	 */
	@Test
	public void verifySampleBColumns() {
		verifyColumns(StaticRifResourceGroup.SAMPLE_B);
	}

	/**
	 * Verifies that our code's column {@link Enum}s match the headers in the
	 * specified sample files.
	 * 
	 * @param sampleGroup
	 *            the sample data files to check the headers of
	 */
	private void verifyColumns(StaticRifResourceGroup sampleGroup) {
		try {
			for (StaticRifResource sampleFile : sampleGroup.getResources()) {
				Enum<?>[] columnsInEnum = getColumnsInEnum(sampleFile.getRifFileType());

				// Use a CSVParser to parse the header out of the sample file.
				CSVFormat parserFormat = CSVFormat.DEFAULT.withDelimiter('|');
				CSVParser parser = RifParsingUtils.createCsvParser(parserFormat, sampleFile.toRifFile());
				CSVRecord sampleHeaderRecord = parser.getRecords().get(0);
				String[] columnsInSample = new String[sampleHeaderRecord.size()];
				for (int col = 0; col < columnsInSample.length; col++)
					columnsInSample[col] = sampleHeaderRecord.get(col);

				Assert.assertEquals(
						String.format("Column count mismatch for '%s'.\nSample Columns: %s\nEnum Columns:   %s\n",
								sampleFile.name(), toHeaderFormat(columnsInSample, c -> c),
								toHeaderFormat(columnsInEnum, c -> c.name())),
						columnsInSample.length, columnsInSample.length);

				/*
				 * Loop through the columns in the sample data and ensure that
				 * our column enums match.
				 */
				for (int col = 0; col < columnsInSample.length; col++) {
					String columnNameFromEnum = columnsInEnum[col].name();
					String columnNameFromSample = columnsInSample[col];
					Assert.assertEquals(
							String.format(
									"Unable to match column '%d' from sample data for '%s'.\nSample Columns: %s\nEnum Columns:   %s\n",
									col, sampleFile.name(), toHeaderFormat(columnsInSample, c -> c),
									toHeaderFormat(columnsInEnum, c -> c.name())),
							columnNameFromSample, columnNameFromEnum);
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @param rifFileType
	 *            the {@link RifFileType} to get the column {@link Enum}s for
	 * @return the RIF column {@link Enum}s for the specified
	 *         {@link RifFileType}
	 */
	private static Enum<?>[] getColumnsInEnum(RifFileType rifFileType) {
		Enum<?> idColumn = rifFileType.getIdColumn();
		Class<?> columnEnumClass = idColumn.getDeclaringClass();
		try {
			Enum<?>[] columnEnums = (Enum<?>[]) columnEnumClass.getMethod("values").invoke(null);
			return columnEnums;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * @param columns
	 *            the array of columns to format
	 * @param columnNameFunction
	 *            the {@link Function} to convert each column entry to the
	 *            column name
	 * @return the specified columns, but in the format used in RIF header rows
	 */
	private static <T> String toHeaderFormat(T[] columns, Function<T, String> columnNameFunction) {
		StringBuilder formattedColumns = new StringBuilder();
		for (int i = 0; i < columns.length; i++) {
			formattedColumns.append(columnNameFunction.apply(columns[i]));
			if (i < (columns.length - 1))
				formattedColumns.append('|');
		}
		return formattedColumns.toString();
	}
}
