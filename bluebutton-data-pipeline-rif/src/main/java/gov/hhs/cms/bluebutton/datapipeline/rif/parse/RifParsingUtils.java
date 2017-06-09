package gov.hhs.cms.bluebutton.datapipeline.rif.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.input.BOMInputStream;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFile;

/**
 * Contains some shared utility code for parsing RIF files.
 */
public final class RifParsingUtils {
	/**
	 * The {@link CSVFormat} for RIF file parsing/writing.
	 */
	public static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader().withDelimiter('|').withEscape('\\');

	/**
	 * @param file
	 *            the {@link RifFile} to parse
	 * @return a {@link CSVParser} for the specified {@link RifFile}
	 */
	public static CSVParser createCsvParser(RifFile file) {
		return createCsvParser(CSV_FORMAT, file);
	}

	/**
	 * @param csvFormat
	 *            the {@link CSVFormat} to use to parse the file
	 * @param file
	 *            the {@link RifFile} to parse
	 * @return a {@link CSVParser} for the specified {@link RifFile}
	 */
	public static CSVParser createCsvParser(CSVFormat csvFormat, RifFile file) {
		return createCsvParser(csvFormat, file.open(), file.getCharset());
	}

	/**
	 * @param csvFormat
	 *            the {@link CSVFormat} to use to parse the file
	 * @param fileStream
	 *            the {@link InputStream} to build a {@link CSVParser} for
	 * @param charset
	 *            the {@link Charset} of the {@link InputStream} to be parsed
	 * @return a {@link CSVParser} for the specified {@link RifFile}
	 */
	public static CSVParser createCsvParser(CSVFormat csvFormat, InputStream fileStream, Charset charset) {
		BOMInputStream fileStreamWithoutBom = new BOMInputStream(fileStream, false);
		InputStreamReader reader = new InputStreamReader(fileStreamWithoutBom, charset);

		try {
			CSVParser parser = new CSVParser(reader, csvFormat);
			return parser;
		} catch (IOException e) {
			/*
			 * Per the docs, this should only be thrown if there's an issue with
			 * the header record. We don't use header records, so this shouldn't
			 * ever occur.
			 */
			throw new InvalidRifFileFormatException("Invalid RIF header record", e);
		}
	}
}
