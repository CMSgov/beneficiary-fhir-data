package gov.cms.bfd.datadictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.csv.CSVFormat;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Functions to create and format an Excel xlsx workbook from an existing set of CSV files for the
 * V1 and V2 versions of the BFD data dictionary.
 */
public class CsvToExcelCommand {

  /** Excel columns widths in units of 1/256th of a character width for the V1 data dictionary. */
  private static int[] v1ColumnsWidths =
      new int[] {
        2080, 7200, 10400, 2400, 1600, 1600, 5600, 11200, 17600, 17600, 9600, 9600, 6400, 6400,
        6400, 6400, 6400, 2400, 2400, 2400, 2400, 2400, 2400
      };

  /** Excel columns widths in units of 1/256th of a character width for the V2 data dictionary. */
  private static int[] v2ColumnsWidths =
      new int[] {
        2080, 7200, 10400, 2400, 1600, 1600, 5600, 11200, 14400, 17600, 17600, 17600, 9600, 9600,
        6400, 6400, 6400, 6400, 6400, 2400, 2400, 2400, 2400, 2400, 2400
      };

  /**
   * Creates, formats and saves an Excel workbook (xlsx) file.
   *
   * @param v1CsvPathName pathname of the V1 data dictionary CSV file
   * @param v2CsvPathName pathname of the V2 data dictionary CSV file
   * @param xslPathName pathname of the Excel file to output
   */
  public static void createAndSaveWorkbook(
      String v1CsvPathName, String v2CsvPathName, String xslPathName) {
    try (OutputStream os = new FileOutputStream(xslPathName);
        Workbook wb = new XSSFWorkbook()) {

      // create and format V1 sheet
      var v1Sheet = wb.createSheet("V1");
      importSheet(v1CsvPathName, v1Sheet);
      formatSheet(wb, v1Sheet, v1ColumnsWidths);

      // create and format V2 sheet
      var v2Sheet = wb.createSheet("V2");
      importSheet(v2CsvPathName, v2Sheet);
      formatSheet(wb, v2Sheet, v2ColumnsWidths);

      // save Excel file
      wb.write(os);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Applies formatting to data dictionary workbook sheet.
   *
   * @param wb the data dictionary Excel workbook
   * @param sheet the data dictionary sheet to format
   * @param columnsWidths array of column widths to apply to sheet
   */
  private static void formatSheet(Workbook wb, Sheet sheet, int[] columnsWidths) {
    var maxRowIndex = sheet.getLastRowNum();
    var maxColIndex = columnsWidths.length - 1;
    searchAndReplace(sheet, CellRangeAddress.valueOf("D2:D" + (maxRowIndex + 1)), ";", "\n");
    searchAndReplace(sheet, CellRangeAddress.valueOf("I2:J" + (maxRowIndex + 1)), ";", "\n");
    setStyle(sheet, new CellRangeAddress(0, 0, 0, maxColIndex), getHeaderCellStyle(wb));
    setStyle(sheet, new CellRangeAddress(1, maxRowIndex, 0, maxColIndex), getDataCellStyle(wb));
    rowShading(sheet, new CellRangeAddress(1, maxRowIndex, 0, maxColIndex), getRowShadingStyle(wb));
    setColumnWidths(sheet, columnsWidths);
    setRowHeights(sheet, new CellRangeAddress(0, maxRowIndex, 0, maxColIndex));
    sheet.createFreezePane(2, 1);
  }

  /**
   * Load an CSV file into a workbook sheet.
   *
   * @param csvFileName the CSV file to import
   * @param sheet the workbook sheet to receive the imported data
   */
  private static void importSheet(String csvFileName, Sheet sheet) {
    var rowIndex = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(csvFileName))) {
      Iterable<CSVRecord> records = CSVFormat.EXCEL.parse(reader);
      for (CSVRecord record : records) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < record.size(); i++) {
          Cell cell = row.createCell(i);
          cell.setCellValue(record.get(i));
        }
        rowIndex += 1;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the column widths for a workbook sheet.
   *
   * @param sheet the sheet with columns to set
   * @param widths an array of column widths in characters in column order
   */
  private static void setColumnWidths(Sheet sheet, int[] widths) {
    for (int i = 0; i < widths.length; i++) {
      sheet.setColumnWidth(i, widths[i]);
    }
  }

  /**
   * Optimally set the row heights for a workbook sheet.
   *
   * @param sheet the sheet with rows to set
   * @param range the sheet range to set
   */
  private static void setRowHeights(Sheet sheet, CellRangeAddress range) {
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
  private static short maxLinesInRow(Row row, int lastCol) {
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
   * @param sheet the sheet to search/replace
   * @param range the range of cells to search/replace
   * @param search the text to search for in each cell
   * @param replace the text to replace the searched values with
   */
  private static void searchAndReplace(
      Sheet sheet, CellRangeAddress range, String search, String replace) {
    applyToRange(
        sheet,
        range,
        (c) -> {
          c.setCellValue(c.getStringCellValue().replace(search, replace));
        });
  }

  /**
   * Set the style for a range of cells.
   *
   * @param sheet the sheet to format
   * @param range the range of cells within the sheet to format
   * @param style the CellStyle to apply to the cells
   */
  private static void setStyle(Sheet sheet, CellRangeAddress range, CellStyle style) {
    applyToRange(
        sheet,
        range,
        (c) -> {
          c.setCellStyle(style);
        });
  }

  /**
   * Implement alternate row shading.
   *
   * @param sheet the sheet to format
   * @param range the range of cells with the sheet to format
   * @param style the CellStyle to apply to alternate rows (odd numbered)
   */
  private static void rowShading(Sheet sheet, CellRangeAddress range, CellStyle style) {
    applyToRange(
        sheet,
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
   * @param sheet the sheet to update
   * @param range the range of cells to update
   * @param fn a consumer function that takes a cell and returns void
   */
  private static void applyToRange(Sheet sheet, CellRangeAddress range, Consumer<Cell> fn) {
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
   * @param wb the workbook
   * @return a CellStyle
   */
  private static CellStyle getHeaderCellStyle(Workbook wb) {
    var style = wb.createCellStyle();
    style.setFillForegroundColor(new XSSFColor(new java.awt.Color(67, 133, 244), null));
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    var font = wb.createFont();
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
   * @param wb the workbook
   * @return a CellStyle
   */
  private static CellStyle getDataCellStyle(Workbook wb) {
    var style = wb.createCellStyle();
    style.setVerticalAlignment(VerticalAlignment.TOP);
    style.setWrapText(true);
    return style;
  }

  /**
   * Create the style for shadded rows.
   *
   * @param wb the workbook
   * @return a CellStyle
   */
  private static CellStyle getRowShadingStyle(Workbook wb) {
    var style = wb.createCellStyle();
    style.cloneStyleFrom(getDataCellStyle(wb));
    style.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillForegroundColor(new XSSFColor(new java.awt.Color(243, 243, 243), null));
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
   * Main method to create and save a new data dictionary Excel workbook.
   *
   * @param args the pathnames for the v1, v2 CSV input files, and the Excel workbook output file
   */
  public static void main(String[] args) {

    if (args.length != 3) {
      throw new RuntimeException(
          "V1 CSV file path, V2 CSV file path, and Excel workbook file path are required.");
    }

    var v1CsvPathName = args[0];
    var v2CsvPathName = args[1];
    var xslPathName = args[2];

    // ensure that the CSV files exist
    for (String fileName : Set.of(v1CsvPathName, v2CsvPathName)) {
      var csvFile = new File(fileName);
      if (!csvFile.isFile()) {
        throw new RuntimeException(String.format("CSV file (%s) does not exist.", fileName));
      }
    }

    // invoke command to create and save a new data dictionary Excel workbook
    CsvToExcelCommand.createAndSaveWorkbook(v1CsvPathName, v2CsvPathName, xslPathName);
  }
}
