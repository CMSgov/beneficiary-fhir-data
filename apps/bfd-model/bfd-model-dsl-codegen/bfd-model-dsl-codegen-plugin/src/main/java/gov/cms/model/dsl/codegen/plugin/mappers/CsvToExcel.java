package gov.cms.model.dsl.codegen.plugin.mappers;

import gov.cms.model.dsl.codegen.plugin.util.Version;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 * Processor that loads and formats a stream of CSV formatted Strings into an Excel workbook sheet.
 */
public class CsvToExcel implements Consumer<String>, Closeable {

  /** Excel columns widths in units of 1/256th of a character width for the V1 data dictionary. */
  private static final List<Integer> v1ColumnsWidths =
      List.of(
          2080, 7200, 10400, 2400, 1600, 1600, 5600, 11200, 17600, 17600, 9600, 9600, 6400, 6400,
          6400, 6400, 6400, 2400, 2400, 2400, 2400, 2400, 2400);

  /** Excel columns widths in units of 1/256th of a character width for the V2 data dictionary. */
  private static final List<Integer> v2ColumnsWidths =
      List.of(
          2080, 7200, 10400, 2400, 1600, 1600, 5600, 11200, 14400, 17600, 17600, 17600, 9600, 9600,
          6400, 6400, 6400, 6400, 6400, 2400, 2400, 2400, 2400, 2400, 2400);

  /** Bright blue color used for header row background. */
  public static final XSSFColor CUSTOM_BLUE = new XSSFColor(new java.awt.Color(67, 133, 244), null);

  /** Light grey color used for cell borders. */
  public static final XSSFColor CUSTOM_GREY =
      new XSSFColor(new java.awt.Color(243, 243, 243), null);

  /** Cell range for the Applies To column. */
  private static final String APPLIES_TO_RANGE = "D2:D";

  /** Cell range for the STU3 Discriminator and STU3 Additional columns. */
  private static final String STU3_DISCRIMINATOR_ADDITIONAL_RANGE = "I2:J";

  /** Excel data dictionary workbook. */
  private Workbook workbook;

  /** The current workbook sheet being modified. */
  private Sheet sheet;

  /** The BFD API version, e.g. V1, V2. */
  private Version version;

  /**
   * Creates and instance of the CsvToExcel given and OutputStream, workbook and version.
   *
   * @param workbook the Excel workbook to load and format
   * @param version the BFD API version, e.g. V1, V2
   * @return an CsvToExcel
   */
  public static CsvToExcel createInstance(Workbook workbook, Version version) {
    var CsvToExcel = new CsvToExcel(workbook, version);
    CsvToExcel.init();
    return CsvToExcel;
  }

  /**
   * Adds a CSV formatted string to the Excel workbook sheet.
   *
   * @param csv the CSV formatted String to add
   */
  @Override
  public void accept(String csv) {
    var record = parseLine(csv);
    if (record != null) {
      var rowIndex = sheet.getLastRowNum() + 1;
      Row row = sheet.createRow(rowIndex);
      for (int i = 0; i < record.size(); i++) {
        Cell cell = row.createCell(i);
        cell.setCellValue(record.get(i));
      }
    }
  }

  /**
   * Formats and saves the Excel workbook.
   *
   * @throws IOException upon write errors
   */
  @Override
  public void close() throws IOException {
    var widths = ("V1".equalsIgnoreCase(version.name())) ? v1ColumnsWidths : v2ColumnsWidths;
    formatSheet(widths);
  }

  /**
   * Private constructor.
   *
   * @param workbook the Excel workbook to load and format
   * @param version the BFD API version, e.g. V1, V2
   */
  private CsvToExcel(Workbook workbook, Version version) {
    this.workbook = workbook;
    this.version = version;
  }

  /** Initializes a new Excel workbook sheet for a given BFD API version. */
  private void init() {
    sheet = workbook.createSheet(version.name());
  }

  /**
   * Applies formatting to data dictionary workbook sheet.
   *
   * @param columnsWidths array of column widths to apply to sheet
   */
  private void formatSheet(List<Integer> columnsWidths) {
    var maxRowIndex = sheet.getLastRowNum();
    var maxColIndex = columnsWidths.size() - 1;
    searchAndReplace(CellRangeAddress.valueOf(APPLIES_TO_RANGE + (maxRowIndex + 1)), ";", "\n");
    searchAndReplace(
        CellRangeAddress.valueOf(STU3_DISCRIMINATOR_ADDITIONAL_RANGE + (maxRowIndex + 1)),
        ";",
        "\n");
    setStyle(new CellRangeAddress(0, 0, 0, maxColIndex), getHeaderCellStyle());
    setStyle(new CellRangeAddress(1, maxRowIndex, 0, maxColIndex), getDataCellStyle());
    rowShading(new CellRangeAddress(1, maxRowIndex, 0, maxColIndex), getRowShadingStyle());
    setColumnWidths(columnsWidths);
    setRowHeights(new CellRangeAddress(0, maxRowIndex, 0, maxColIndex));
    sheet.createFreezePane(2, 1);
  }

  /**
   * Set the column widths for a workbook sheet.
   *
   * @param widths an array of column widths in characters in column order
   */
  private void setColumnWidths(List<Integer> widths) {
    for (int i = 0; i < widths.size(); i++) {
      sheet.setColumnWidth(i, widths.get(i));
    }
  }

  /**
   * Optimally set the row heights for a workbook sheet.
   *
   * @param range the sheet range to set
   */
  private void setRowHeights(CellRangeAddress range) {
    var defaultHeight = sheet.getDefaultRowHeight();
    for (int i = range.getFirstRow(); i <= range.getLastRow(); i++) {
      var row = sheet.getRow(i);
      if (row != null) {
        var max = maxLinesInRow(row, range.getLastColumn());
        if (row.getRowNum() == 0) {
          max = 2;
        }
        row.setHeight((short) (max * defaultHeight));
      }
    }
  }

  /**
   * Determines the maximum number of lines in a row.
   *
   * @param row the row to inspect
   * @param lastCol the last column in the row to inspect
   * @return the maximum number of lines for any cell in a row (min:1)
   */
  private short maxLinesInRow(Row row, int lastCol) {
    short maxLines = 1;
    for (int i = 0; i <= lastCol; i++) {
      var cell = row.getCell(i);
      if (cell != null) {
        var lines = cell.getStringCellValue().split("\r\n|\n|\r").length;
        if (maxLines < lines) {
          maxLines = (short) lines;
        }
      }
    }
    return maxLines;
  }

  /**
   * Do a text search and replace on a range of cells.
   *
   * @param range the range of cells to search/replace
   * @param search the text to search for in each cell
   * @param replace the text to replace the searched values with
   */
  private void searchAndReplace(CellRangeAddress range, String search, String replace) {
    applyToRange(
        range,
        (c) -> {
          c.setCellValue(c.getStringCellValue().replace(search, replace));
        });
  }

  /**
   * Set the style for a range of cells.
   *
   * @param range the range of cells within the sheet to format
   * @param style the CellStyle to apply to the cells
   */
  private void setStyle(CellRangeAddress range, CellStyle style) {
    applyToRange(
        range,
        (c) -> {
          c.setCellStyle(style);
        });
  }

  /**
   * Implement alternate row shading.
   *
   * @param range the range of cells with the sheet to format
   * @param style the CellStyle to apply to alternate rows (odd numbered)
   */
  private void rowShading(CellRangeAddress range, CellStyle style) {
    applyToRange(
        range,
        (c) -> {
          if (c.getRowIndex() % 2 == 1) {
            c.setCellStyle(style);
          }
        });
  }

  /**
   * Apply a consumer function to a range of cells in a sheet.
   *
   * @param range the range of cells to update
   * @param fn a consumer function that takes a cell and returns void
   */
  private void applyToRange(CellRangeAddress range, Consumer<Cell> fn) {
    for (int i = range.getFirstRow(); i <= range.getLastRow(); i++) {
      var row = sheet.getRow(i);
      if (row != null) {
        for (int j = range.getFirstColumn(); j <= range.getLastColumn(); j++) {
          var cell = row.getCell(j);
          if (cell != null) {
            fn.accept(cell);
          }
        }
      }
    }
  }

  /**
   * Create the style for the sheet header row.
   *
   * @return a CellStyle
   */
  private CellStyle getHeaderCellStyle() {
    var style = workbook.createCellStyle();
    style.setFillForegroundColor(CUSTOM_BLUE);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    var font = workbook.createFont();
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
    style.setVerticalAlignment(VerticalAlignment.TOP);
    style.setWrapText(true);
    return style;
  }

  /**
   * Create the style for the sheet data rows.
   *
   * @return a CellStyle
   */
  private CellStyle getDataCellStyle() {
    var style = workbook.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.TOP);
    style.setWrapText(true);
    return style;
  }

  /**
   * Create the style for shaded rows.
   *
   * @return a CellStyle
   */
  private CellStyle getRowShadingStyle() {
    var style = workbook.createCellStyle();
    style.cloneStyleFrom(getDataCellStyle());
    style.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillForegroundColor(CUSTOM_GREY);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderTop(BorderStyle.THIN);
    style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderLeft(BorderStyle.THIN);
    style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setBorderRight(BorderStyle.THIN);
    style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    return style;
  }

  /**
   * Parses a String into a CVSRecord.
   *
   * @param line the String to parse
   * @return a CVSRecord
   */
  private static CSVRecord parseLine(String line) {
    try (CSVParser parse = CSVParser.parse(line, CSVFormat.EXCEL)) {
      return parse.getRecords().get(0);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
