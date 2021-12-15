package gov.cms.bfd.model.codegen;

import com.google.common.base.Strings;
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

/** Models the RIF field layout data. */
public final class RifLayout {
  private final String name;
  private final List<RifField> rifFields;

  /**
   * Constructs a new {@link RifLayout} instance.
   *
   * @param rifFields the value to use for {@link #getRifFields()}
   */
  public RifLayout(String name, List<RifField> rifFields) {
    this.name = name;
    this.rifFields = rifFields;
  }

  /**
   * @return the name of this {@link RifLayout} (i.e. the name of the sheet in the {@link Workbook}
   *     that defines it)
   */
  public String getName() {
    return name;
  }

  /** @return the ordered {@link List} of {@link RifField}s that comprise this {@link RifLayout} */
  public List<RifField> getRifFields() {
    return rifFields;
  }

  /**
   * Parses a {@link RifLayout} instance from the specified Excel sheet.
   *
   * @param workbook the Excel file {@link Workbook} to parse
   * @param sheetName the name of the specific {@link Workbook} sheet to parse
   * @return a new {@link RifLayout} instance, extracted from the layout defined in the specified
   *     {@link Workbook} sheet
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

      /*
       * The spreadsheet has a number of empty or
       * "Need Missing Field Here" rows, which we should skip.
       */
      if (row.getCell(0) == null || row.getCell(0).getStringCellValue().trim().isEmpty()) continue;

      String rifColumnName = row.getCell(0).getStringCellValue();

      /*
       * Always skip the "DML_IND" field, as we handle it elsewhere in
       * RifFilesProcessor.
       */
      if (rifColumnName.trim().equals("DML_IND")) continue;

      /*
       * This field is sometimes included twice. Skip the second
       * occurrence.
       */
      if (rifColumnName.trim().equals("BENE_ID")
          && rifFields.stream().filter(f -> f.rifColumnName.equals("BENE_ID")).count() > 0)
        continue;

      RifColumnType rifColumnType = RifColumnType.valueOf(row.getCell(1).getStringCellValue());
      @SuppressWarnings("deprecation")
      Optional<Integer> rifColumnLength =
          row.getCell(2).getCellTypeEnum() == CellType.NUMERIC
              ? Optional.of((int) row.getCell(2).getNumericCellValue())
              : Optional.empty();
      @SuppressWarnings("deprecation")
      Optional<Integer> rifColumnScale =
          row.getCell(3).getCellTypeEnum() == CellType.NUMERIC
              ? Optional.of((int) row.getCell(3).getNumericCellValue())
              : Optional.empty();

      boolean rifColumnOptional = parseBoolean(row.getCell(4));
      URL dataDictionaryEntry = parseUrl(row.getCell(6));
      String rifColumnLabel = row.getCell(7).getStringCellValue();
      String javaFieldName = row.getCell(8).getStringCellValue();

      // FIXME remove workarounds once CBBD-283 is resolved
      if ("HHA,Hospice,Inpatient,Outpatient,SNF".contains(sheetName)
          && "AT_PHYSN_UPIN".equals(rifColumnName)) rifColumnLength = Optional.of(9);
      if ("Inpatient,Outpatient,SNF".contains(sheetName) && "OP_PHYSN_UPIN".equals(rifColumnName))
        rifColumnLength = Optional.of(9);
      if ("Hospice".contains(sheetName) && "BENE_HOSPC_PRD_CNT".equals(rifColumnName))
        rifColumnLength = Optional.of(2);
      if ("Inpatient,Outpatient,SNF".contains(sheetName) && "OT_PHYSN_UPIN".equals(rifColumnName))
        rifColumnLength = Optional.of(9);
      if ("HHA,Hospice,Inpatient,Outpatient,SNF".contains(sheetName)
          && "PRVDR_NUM".equals(rifColumnName)) rifColumnLength = Optional.of(9);
      if ("HHA".contains(sheetName) && "CLM_HHA_TOT_VISIT_CNT".equals(rifColumnName))
        rifColumnLength = Optional.of(4);

      rifFields.add(
          new RifField(
              rifColumnName,
              rifColumnType,
              rifColumnLength,
              rifColumnScale,
              rifColumnOptional,
              dataDictionaryEntry,
              rifColumnLabel,
              javaFieldName));
    }

    return new RifLayout(sheetName, rifFields);
  }

  /**
   * @param cell the {@link Cell} to try and extract a <code>boolean</code> from
   * @return the <code>boolean</code> value that was in the specified {@link Cell}
   */
  @SuppressWarnings("deprecation")
  private static boolean parseBoolean(Cell cell) {
    if (cell.getCellTypeEnum() == CellType.BOOLEAN) return cell.getBooleanCellValue();
    else
      /*
       * I had some trouble with actual Boolean values stored in the
       * spreadsheet getting corrupted, so we also support String booleans
       * here.
       */
      return Boolean.valueOf(cell.getStringCellValue().toLowerCase());
  }

  /**
   * @param cell the {@link Cell} to try and extract a {@link URL} from
   * @return the hyperlinked {@link URL} that was in the specified {@link Cell}, or <code>null
   *     </code>
   */
  private static URL parseUrl(Cell cell) {
    Hyperlink hyperlink = cell.getHyperlink();
    if (hyperlink == null) return null;

    try {
      return new URL(hyperlink.getAddress());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** Models a specific field in a RIF layout. */
  public static final class RifField {
    private final String rifColumnName;
    private final RifColumnType rifColumnType;
    private final Integer rifColumnLength;
    private final Integer rifColumnScale;
    private final boolean rifColumnOptional;
    private final URL dataDictionaryEntry;
    private final String rifColumnLabel;
    private final String javaFieldName;

    /**
     * Constructs a new {@link RifField}.
     *
     * @param rifColumnName the value to use for {@link #getRifColumnName()}
     * @param rifColumnType the value to use for {@link #getRifColumnType()}
     * @param rifColumnLength the value to use for {@link #getRifColumnLength()}
     * @param rifColumnScale the value to use for {@link #getRifColumnScale()}
     * @param rifColumnOptional the value to use for {@link #isRifColumnOptional()}
     * @param dataDictionaryEntry the value to use for {@link #getDataDictionaryEntry()}
     * @param rifColumnLabel the value to use for {@link #getRifColumnLabel()}
     * @param javaFieldName the value to use for {@link #getJavaFieldName()}
     */
    public RifField(
        String rifColumnName,
        RifColumnType rifColumnType,
        Optional<Integer> rifColumnLength,
        Optional<Integer> rifColumnScale,
        boolean rifColumnOptional,
        URL dataDictionaryEntry,
        String rifColumnLabel,
        String javaFieldName) {
      if (Strings.isNullOrEmpty(rifColumnName)) {
        throw new IllegalArgumentException("Missing 'Column Name'");
      }
      if (rifColumnType == null) {
        throw new IllegalArgumentException("Missing 'Type' for colum name: " + rifColumnName);
      }
      if (rifColumnLength.isPresent() && rifColumnLength.get() < 0) {
        throw new IllegalArgumentException("Invalid 'Length' for colum name: " + rifColumnName);
      }
      if (rifColumnScale.isPresent() && rifColumnScale.get() < 0) {
        throw new IllegalArgumentException("Invalid 'Scale' for colum name: " + rifColumnName);
      }
      if (Objects.isNull(javaFieldName)) {
        throw new IllegalArgumentException(
            "Missing 'Column Label/Value' for colum name: " + rifColumnName);
      }
      if (Strings.isNullOrEmpty(javaFieldName)) {
        throw new IllegalArgumentException(
            "Missing 'Java Field Name' for colum name: " + rifColumnName);
      }

      this.rifColumnName = rifColumnName;
      this.rifColumnType = rifColumnType;
      this.rifColumnLength = rifColumnLength.orElse(null);
      this.rifColumnScale = rifColumnScale.orElse(null);
      this.rifColumnOptional = rifColumnOptional;
      this.dataDictionaryEntry = dataDictionaryEntry;
      this.rifColumnLabel = rifColumnLabel;
      this.javaFieldName = javaFieldName;
    }

    /** @return the name of the RIF column in RIF export data files */
    public String getRifColumnName() {
      return rifColumnName;
    }

    /** @return the type of the RIF column, as specified in the data dictionary */
    public RifColumnType getRifColumnType() {
      return rifColumnType;
    }

    /**
     * @return the length (or, for <code>NUM</code> columns, the precision) of the RIF column, as
     *     specified in the data dictionary, or {@link Optional#empty()} if none is defined
     */
    public Optional<Integer> getRifColumnLength() {
      return Optional.ofNullable(rifColumnLength);
    }

    /**
     * @return the scale of the RIF column, as specified in the data dictionary, or {@link
     *     Optional#empty()} if none is defined
     */
    public Optional<Integer> getRifColumnScale() {
      return Optional.ofNullable(rifColumnScale);
    }

    /**
     * @return <code>true</code> if the specified RIF column's value is sometimes blank, <code>false
     *     </code> if it is always populated
     */
    public boolean isRifColumnOptional() {
      return rifColumnOptional;
    }

    /** @return the field's data dictionary entry, if any */
    public Optional<URL> getDataDictionaryEntry() {
      return Optional.ofNullable(dataDictionaryEntry);
    }

    /**
     * @return a brief label/description of the RIF column, which may be empty for some {@link
     *     RifField}s
     */
    public String getRifColumnLabel() {
      return rifColumnLabel;
    }

    /** @return the java entity property name associated with the JPA <code>Entity</code> */
    public String getJavaFieldName() {
      return javaFieldName;
    }

    /** @return a String dumpt of the RifField */
    public String toString() {
      StringBuilder sb = new StringBuilder("RifField: { ");
      sb.append("columnName=")
          .append(rifColumnName)
          .append(", ")
          .append("columnType=")
          .append(rifColumnType != null ? rifColumnType.toString() : "N/A")
          .append(", ")
          .append("javaName=")
          .append(javaFieldName != null ? javaFieldName : "N/A")
          .append(" }");
      return sb.toString();
    }
  }

  /** Enumerates the various RIF column types. */
  public static enum RifColumnType {
    CHAR,

    DATE,

    NUM,

    TIMESTAMP;
  }
}
