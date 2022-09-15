package gov.cms.bfd.pipeline.rda.insights.filters;

import gov.cms.bfd.pipeline.rda.insights.models.FieldResult;
import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WriteXLSXFilter extends PipelineFilter {

    public WriteXLSXFilter(ConfigLoader appConfigs) {
        super(appConfigs);
    }

    @Override
    public void run(Pipeline pipeline, PipelineValues pipelineValues) {
        try {
            if (!pipelineValues.isEmpty()) {
                File outputFile = Path.of(getOutputDir(), pipeline.getTarget()).toFile();

                XSSFWorkbook workbook;

                if (outputFile.exists()) {
                    workbook = new XSSFWorkbook(new FileInputStream(outputFile));
                } else {
                    outputFile.getParentFile().mkdirs();
                    outputFile.createNewFile();
                    workbook = new XSSFWorkbook();
                }

                final String SHEET_NAME = pipeline.getLabel();
                Sheet sheet = workbook.getSheet(SHEET_NAME);

                if (sheet != null) {
                    workbook.removeSheetAt(workbook.getSheetIndex(SHEET_NAME));
                }

                sheet = workbook.createSheet(SHEET_NAME);

                int rowCount = 0;

                Row header = sheet.createRow(rowCount++);

                CellStyle headerStyle = workbook.createCellStyle();

                XSSFFont font = workbook.createFont();
                font.setFontName("Arial");
                font.setBold(true);
                headerStyle.setFont(font);

                Cell headerCell;
                List<String> columnNames = new ArrayList<>();

                {
                    int i = 0;
                    for (String columnName : pipelineValues.get(0).keySet()) {
                        columnNames.add(columnName);
                        headerCell = header.createCell(i++);
                        headerCell.setCellValue(columnName);
                        headerCell.setCellStyle(headerStyle);
                    }
                }

                for (PipelineValues.ValueSet valueSet : pipelineValues.valueSets()) {
                    Row row = sheet.createRow(rowCount++);

                    for (int i = 0; i < columnNames.size(); i++) {
                        final String columnName = columnNames.get(i);
                        FieldResult<?> fieldResult = valueSet.get(columnName);

                        if (fieldResult != null) {
                            Cell cell = row.createCell(i);
                            Object cellValue = fieldResult.getValue();

                            if (fieldResult.getValueType() == Integer.class) {
                                cell.setCellValue((int) cellValue);
                            } else if (fieldResult.getValueType() == Double.class) {
                                cell.setCellValue((double) cellValue);
                            } else if (fieldResult.getValueType() == Float.class) {
                                cell.setCellValue((float) cellValue);
                            } else if (fieldResult.getValueType() == Boolean.class) {
                                cell.setCellValue((boolean) cellValue);
                            } else {
                                cell.setCellValue(String.valueOf(cellValue));
                            }
                        } else {
                            throw new IllegalStateException(
                                    String.format("No result value for column '%s'", columnName));
                        }
                    }
                }

                for (int i = 0; i < columnNames.size(); ++i) {
                    sheet.autoSizeColumn(i);
                }

                FileOutputStream outputStream = new FileOutputStream(outputFile);
                workbook.write(outputStream);
                workbook.close();
            } else {
                throw new IllegalStateException("No sql results to write to excel sheet.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read from XLSX file", e);
        }
    }

}
