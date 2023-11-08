package gov.cms.bfd.datadictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Functions to create and format an Excel xlsx workbook from an existing set of CSV files for the
 * V1 and V2 versions of the BFD data dictionary.
 */
public class CsvToExcelCommand {

  /** Excel columns widths in characters for the V1 data dictionary. */
  private static int[] v1ColumnsWidths =
      new int[] {
        65, 225, 255, 75, 50, 50, 175, 255, 255, 255, 255, 255, 200, 200, 200, 200, 200, 75, 75, 75,
        75, 75, 75
      };

  /** Excel columns widths in characters for the V2 data dictionary. */
  private static int[] v2ColumnsWidths =
      new int[] {
        65, 225, 255, 75, 50, 50, 175, 255, 255, 255, 255, 255, 255, 255, 200, 200, 200, 200, 200,
        75, 75, 75, 75, 75, 75
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

    setColumnWidths(sheet, columnsWidths);
    setAlternatingRowStyle(sheet, new CellRangeAddress(1, maxRowIndex, 0, maxColIndex));
    setStyleForRange(sheet, new CellRangeAddress(0, 0, 0, maxColIndex), getHeaderCellStyle(wb));
    setStyleForRange(
        sheet, new CellRangeAddress(1, maxRowIndex, 1, maxColIndex), getDataCellStyle(wb));
    searchAndReplace(sheet, new CellRangeAddress(1, maxRowIndex, 3, 3), ";", "\n");
    searchAndReplace(sheet, new CellRangeAddress(1, maxRowIndex, 8, 9), ";", "\n");
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
      sheet.setColumnWidth(i, widths[i] * 256);
    }
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
    for (int i = range.getFirstRow(); i <= range.getLastRow(); i++) {
      var row = sheet.getRow(i);
      for (int j = range.getFirstColumn(); j <= range.getLastColumn(); j++) {
        var cell = row.getCell(j);
        if (cell != null) {
          var str = cell.getStringCellValue();
          cell.setCellValue(str.replace(search, replace));
        }
      }
    }
  }

  /**
   * Set the style for a range of cells.
   *
   * @param sheet the sheet to format
   * @param range the range of cells within the sheet to format
   * @param style the CellStyle to apply to the cells
   */
  private static void setStyleForRange(Sheet sheet, CellRangeAddress range, CellStyle style) {
    for (int i = range.getFirstRow(); i <= range.getLastRow(); i++) {
      var row = sheet.getRow(i);
      for (int j = range.getFirstColumn(); j <= range.getLastColumn(); j++) {
        var cell = row.getCell(j);
        if (cell != null) {
          cell.setCellStyle(style);
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
    style.setFillBackgroundColor(
        new XSSFColor(new java.awt.Color(137, 207, 240), new DefaultIndexedColorMap()));
    style.setFillPattern(FillPatternType.LESS_DOTS);
    var font = wb.getFontAt(style.getFontIndex());
    font.setBold(true);
    font.setColor(IndexedColors.WHITE.getIndex());
    style.setFont(font);
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
   * Applies alternate row shading to a range of cells.
   *
   * @param sheet the sheet to apply the shading to
   * @param range the range of cells to apply the shading to
   */
  private static void setAlternatingRowStyle(Sheet sheet, CellRangeAddress range) {
    SheetConditionalFormatting cf = sheet.getSheetConditionalFormatting();
    ConditionalFormattingRule rule =
        sheet.getSheetConditionalFormatting().createConditionalFormattingRule("MOD(ROW(), 2) <> 0");
    PatternFormatting formatting = rule.createPatternFormatting();
    formatting.setFillBackgroundColor(
        new XSSFColor(new java.awt.Color(221, 221, 221), new DefaultIndexedColorMap()));
    formatting.setFillPattern(FillPatternType.LESS_DOTS.getCode());
    ConditionalFormattingRule[] rules = {rule};
    CellRangeAddress[] regions = {range};
    cf.addConditionalFormatting(regions, rules);
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
