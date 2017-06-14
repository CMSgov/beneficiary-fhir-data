package gov.hhs.cms.bluebutton.data.model.codegen;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.google.common.base.Strings;

/**
 * Models the RIF field layout data.
 */
public final class RifLayout {
	private final List<RifField> rifFields;

	/**
	 * Constructs a new {@link RifLayout} instance.
	 * 
	 * @param rifFields
	 *            the value to use for {@link #getRifFields()}
	 */
	public RifLayout(List<RifField> rifFields) {
		this.rifFields = rifFields;
	}

	/**
	 * @return the ordered {@link List} of {@link RifField}s that comprise this
	 *         {@link RifLayout}
	 */
	public List<RifField> getRifFields() {
		return rifFields;
	}

	/**
	 * Parses a {@link RifLayout} instance from the specified Excel sheet.
	 * 
	 * @param workbook
	 *            the Excel file {@link Workbook} to parse
	 * @param sheetName
	 *            the name of the specific {@link Workbook} sheet to parse
	 * @return a new {@link RifLayout} instance, extracted from the layout
	 *         defined in the specified {@link Workbook} sheet
	 */
	public static RifLayout parse(Workbook workbook, String sheetName) {
		Sheet sheet = workbook.getSheet(sheetName);
		Iterator<Row> rows = sheet.iterator();

		// Skip the header rows.
		rows.next();
		rows.next();

		// Loop through all of the field definition rows.
		List<RifField> rifFields = new LinkedList<>();
		while (rows.hasNext()) {
			Row row = rows.next();

			String rifColumnName = row.getCell(0).getStringCellValue();

			/*
			 * The spreadsheet has a number of empty or
			 * "Need Missing Field Here" rows, which we should skip.
			 */
			if (rifColumnName.trim().isEmpty())
				continue;

			/*
			 * Always skip the "DML_IND" field, as we handle it elsewhere in
			 * RifFilesProcessor.
			 */
			if (rifColumnName.trim().equals("DML_IND"))
				continue;

			/*
			 * This field is sometimes included twice. Skip the second
			 * occurrence.
			 */
			if (rifColumnName.trim().equals("BENE_ID")
					&& rifFields.stream().filter(f -> f.rifColumnName.equals("BENE_ID")).count() > 0)
				continue;

			/*
			 * TODO Until we move everything to the latest schema version, skip
			 * the CLM_GRP_ID field.
			 */
			if (rifColumnName.trim().equals("CLM_GRP_ID"))
				continue;

			RifColumnType rifColumnType = RifColumnType.valueOf(row.getCell(1).getStringCellValue());
			int rifColumnLength = (int) row.getCell(2).getNumericCellValue();
			boolean rifColumnOptional = parseBoolean(row.getCell(3));
			URL dataDictionaryEntry = parseUrl(row.getCell(5));
			String rifColumnLabel = row.getCell(6).getStringCellValue();
			String javaFieldName = row.getCell(7).getStringCellValue();

			rifFields.add(new RifField(rifColumnName, rifColumnType, rifColumnLength, rifColumnOptional,
					dataDictionaryEntry, rifColumnLabel, javaFieldName));
		}

		return new RifLayout(rifFields);
	}

	/**
	 * @param cell
	 *            the {@link Cell} to try and extract a <code>boolean</code>
	 *            from
	 * @return the <code>boolean</code> value that was in the specified
	 *         {@link Cell}
	 */
	private static boolean parseBoolean(Cell cell) {
		if (cell.getCellTypeEnum() == CellType.BOOLEAN)
			return cell.getBooleanCellValue();
		else
			/*
			 * I had some trouble with actual Boolean values stored in the
			 * spreadsheet getting corrupted, so we also support String booleans
			 * here.
			 */
			return Boolean.valueOf(cell.getStringCellValue().toLowerCase());
	}

	/**
	 * @param cell
	 *            the {@link Cell} to try and extract a {@link URL} from
	 * @return the hyperlinked {@link URL} that was in the specified
	 *         {@link Cell}, or <code>null</code>
	 */
	private static URL parseUrl(Cell cell) {
		Hyperlink hyperlink = cell.getHyperlink();
		if (hyperlink == null)
			return null;

		try {
			return new URL(hyperlink.getAddress());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Models a specific field in a RIF layout.
	 */
	public static final class RifField {
		private final String rifColumnName;
		private final RifColumnType rifColumnType;
		private final int rifColumnLength;
		private final boolean rifColumnOptional;
		private final URL dataDictionaryEntry;
		private final String rifColumnLabel;
		private final String javaFieldName;

		/**
		 * Constructs a new {@link RifField}.
		 * 
		 * @param rifColumnName
		 *            the value to use for {@link #getRifColumnName()}
		 * @param rifColumnType
		 *            the value to use for {@link #getRifColumnType()}
		 * @param rifColumnLength
		 *            the value to use for {@link #getRifColumnLength()}
		 * @param rifColumnOptional
		 *            the value to use for {@link #isRifColumnOptional()}
		 * @param dataDictionaryEntry
		 *            the value to use for {@link #getDataDictionaryEntry()}
		 * @param rifColumnLabel
		 *            the value to use for {@link #getRifColumnLabel()}
		 * @param javaFieldName
		 *            the value to use for {@link #getJavaFieldName()}
		 */
		public RifField(String rifColumnName, RifColumnType rifColumnType, int rifColumnLength,
				boolean rifColumnOptional, URL dataDictionaryEntry, String rifColumnLabel, String javaFieldName) {
			if (Strings.isNullOrEmpty(rifColumnName))
				throw new IllegalArgumentException("Missing 'Column Name'.");
			if (rifColumnType == null)
				throw new IllegalArgumentException("Missing 'Type'.");
			if (rifColumnLength < 1)
				throw new IllegalArgumentException("Missing or invalid 'Length'.");
			if (Objects.isNull(javaFieldName))
				throw new IllegalArgumentException("Missing 'Column Label/Value'.");
			if (Strings.isNullOrEmpty(javaFieldName))
				throw new IllegalArgumentException("Missing 'Java Field Name'.");

			this.rifColumnName = rifColumnName;
			this.rifColumnType = rifColumnType;
			this.rifColumnLength = rifColumnLength;
			this.rifColumnOptional = rifColumnOptional;
			this.dataDictionaryEntry = dataDictionaryEntry;
			this.rifColumnLabel = rifColumnLabel;
			this.javaFieldName = javaFieldName;
		}

		/**
		 * @return the name of the RIF column in RIF export data files
		 */
		public String getRifColumnName() {
			return rifColumnName;
		}

		/**
		 * @return the type of the RIF column, as specified in the data
		 *         dictionary
		 */
		public RifColumnType getRifColumnType() {
			return rifColumnType;
		}

		/**
		 * @return the length of the RIF column, as specified in the data
		 *         dictionary
		 */
		public int getRifColumnLength() {
			return rifColumnLength;
		}

		/**
		 * @return <code>true</code> if the specified RIF column's value is
		 *         sometimes blank, <code>false</code> if it is always populated
		 */
		public boolean isRifColumnOptional() {
			return rifColumnOptional;
		}

		/**
		 * @return the field's data dictionary entry, if any
		 */
		public Optional<URL> getDataDictionaryEntry() {
			return Optional.ofNullable(dataDictionaryEntry);
		}

		/**
		 * @return a brief label/description of the RIF column, which may be
		 *         empty for some {@link RifField}s
		 */
		public String getRifColumnLabel() {
			return rifColumnLabel;
		}

		/**
		 * @return the name of the JPA <code>Entity</code> field to store this
		 *         RIF column's data in
		 */
		public String getJavaFieldName() {
			return javaFieldName;
		}
	}

	/**
	 * Enumerates the various RIF column types.
	 */
	public static enum RifColumnType {
		CHAR,

		DATE,

		NUM;
	}
}
