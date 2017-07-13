package gov.hhs.cms.bluebutton.data.model.rif.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Optional;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.io.input.BOMInputStream;

import gov.hhs.cms.bluebutton.data.model.rif.RifFile;

/**
 * Contains some shared utility code for parsing RIF files.
 */
public final class RifParsingUtils {
	/**
	 * The {@link CSVFormat} for RIF file parsing/writing.
	 */
	public static final CSVFormat CSV_FORMAT = CSVFormat.EXCEL.withHeader().withDelimiter('|').withEscape('\\');

	private final static DateTimeFormatter RIF_DATE_FORMATTER = new DateTimeFormatterBuilder().parseCaseInsensitive()
			.appendPattern("dd-MMM-yyyy").toFormatter();

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

	/**
	 * @param string
	 *            the value to parse
	 * @return the {@link String} that was specified (yes, this is a silly
	 *         method, but it's here for consistency)
	 */
	public static String parseString(String string) {
		return string;
	}

	/**
	 * @param string
	 *            the value to parse
	 * @return an {@link Optional} {@link String}, where
	 *         {@link Optional#isPresent()} will be <code>false</code> if the
	 *         specified value was empty, and will otherwise contain the
	 *         specified value
	 */
	public static Optional<String> parseOptionalString(String string) {
		return string.isEmpty() ? Optional.empty() : Optional.of(string);
	}

	/**
	 * @param intText
	 *            the number string to parse
	 * @return the specified text parsed into an {@link Integer}
	 */
	public static Integer parseInteger(String intText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */
		try {
			return Integer.parseInt(intText);
		} catch (NumberFormatException e) {
			throw new InvalidRifValueException(String.format("Unable to parse integer value: '%s'.", intText), e);
		}
	}

	/**
	 * @param intText
	 *            the number string to parse
	 * @return an {@link Optional} populated with an {@link Integer} if the
	 *         input has data, or an empty Optional if not
	 */
	public static Optional<Integer> parseOptionalInteger(String intText) {
		if (intText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseInteger(intText));
		}
	}

	/**
	 * @param decimalText
	 *            the decimal string to parse
	 * @return the specified text parsed into a {@link BigDecimal}
	 */
	public static BigDecimal parseDecimal(String decimalText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */
		if (decimalText.isEmpty()) {
			return new BigDecimal(0);
		} else {
			try {
				return new BigDecimal(decimalText);
			} catch (NumberFormatException e) {
				throw new InvalidRifValueException(String.format("Unable to parse decimal value: '%s'.", decimalText),
						e);
			}
		}
	}

	/**
	 * @param decimalText
	 *            the decimal string to parse
	 * @return the result of {@link #parseDecimal(String)} if the specified text
	 *         isn't empty, or an empty Optional if it is empty
	 */
	public static Optional<BigDecimal> parseOptionalDecimal(String decimalText) {
		if (decimalText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseDecimal(decimalText));
		}
	}

	/**
	 * @param dateText
	 *            the date string to parse
	 * @return the specified text as a {@link LocalDate}, parsed using
	 *         {@link #RIF_DATE_FORMATTER}
	 */
	public static LocalDate parseDate(String dateText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		try {
			LocalDate dateFrom = LocalDate.parse(dateText, RIF_DATE_FORMATTER);
			return dateFrom;
		} catch (DateTimeParseException e) {
			throw new InvalidRifValueException(String.format("Unable to parse date value: '%s'.", dateText), e);
		}
	}

	/**
	 * @param dateText
	 *            the date string to parse
	 * @return an {@link Optional} populated with a {@link LocalDate} if the
	 *         input has data, or an empty Optional if not
	 */
	public static Optional<LocalDate> parseOptionalDate(String dateText) {
		if (dateText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseDate(dateText));
		}
	}

	/**
	 * @param charText
	 *            the char string to parse
	 * @return the specified text as a {@link Character} (first character only),
	 *         parsed using {@link #RIF_DATE_FORMATTER}
	 */
	public static Character parseCharacter(String charText) {
		/*
		 * Might seem silly to pull this out, but it makes the code a bit easier
		 * to read, and ensures that this parsing is standardized.
		 */

		if (charText.length() != 1)
			throw new InvalidRifValueException(String.format("Unable to parse character value: '%s'.", charText));

		return charText.charAt(0);
	}

	/**
	 * @param charText
	 *            the date string to parse
	 * @return an {@link Optional} populated with a {@link Character} if the
	 *         input has data, or an empty Optional if not
	 */
	public static Optional<Character> parseOptionalCharacter(String charText) {
		if (charText.isEmpty()) {
			return Optional.empty();
		} else {
			return Optional.of(parseCharacter(charText));
		}
	}
}
