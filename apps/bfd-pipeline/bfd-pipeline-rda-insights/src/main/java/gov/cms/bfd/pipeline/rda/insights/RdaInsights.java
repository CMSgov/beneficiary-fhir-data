package gov.cms.bfd.pipeline.rda.insights;

import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.util.HibernateConnections;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class RdaInsights {

    private final ConfigLoader dbConfigs;
    private final Set<Pipeline> pipelines;

    public void run() {
        HibernateConnections connections = new HibernateConnections(dbConfigs);

        String schema = dbConfigs.stringValue("schema", "public");

        try (SessionFactory factory = connections.getSessionFactory()) {
            Session session = factory.openSession();

            for (Pipeline pipeline : pipelines) {
                List<Map<String, FieldResult<?>>> sqlResults = new ArrayList<>();

                while (pipeline != null) {
                    switch (pipeline.getType()) {
                        case SQL:
                            try (InputStream input = getClass()
                                    .getClassLoader()
                                    .getResourceAsStream("queries/" + pipeline.getTarget())) {
                                if (input != null) {
                                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                                        String baseQuery = reader.lines().collect(Collectors.joining("\n"));
                                        Set<String> sqlQueries = expandQueries(baseQuery, sqlResults, schema);
                                        sqlResults.clear();

                                        for (String sqlQuery : sqlQueries) {
                                            Transaction tx = session.beginTransaction();
                                            Query<?> query = session.createNativeQuery(sqlQuery);
                                            sqlResults.addAll(retrieveRowResults(query));
                                            tx.commit();
                                        }
                                    }
                                } else {
                                    throw new IllegalArgumentException(
                                            String.format("SQL file '%s' not found", pipeline.getTarget()));
                                }
                            }
                            break;
                        case XLSX:
                            if (!sqlResults.isEmpty()) {
                                File file = new File(pipeline.getTarget());

                                XSSFWorkbook workbook;

                                if (file.exists()) {
                                    workbook = new XSSFWorkbook(new FileInputStream(file));
                                } else {
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
//                                font.setFontHeightInPoints((short) 16);
                                font.setBold(true);
                                headerStyle.setFont(font);

                                Cell headerCell;
                                List<String> columnNames = new ArrayList<>();

                                {
                                    int i = 0;
                                    for (String columnName : sqlResults.get(0).keySet()) {
                                        columnNames.add(columnName);
                                        headerCell = header.createCell(i++);
                                        headerCell.setCellValue(columnName);
                                        headerCell.setCellStyle(headerStyle);
                                    }
                                }

                                for (Map<String, FieldResult<?>> fieldResultMap : sqlResults) {
                                    Row row = sheet.createRow(rowCount++);

                                    for (int i = 0; i < columnNames.size(); i++) {
                                        final String columnName = columnNames.get(i);
                                        FieldResult<?> fieldResult = fieldResultMap.get(columnName);

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

                                FileOutputStream outputStream = new FileOutputStream(pipeline.getTarget());
                                workbook.write(outputStream);
                                workbook.close();
                            } else {
                                throw new IllegalStateException("No sql results to write to excel sheet.");
                            }
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported pipeline type");
                    }

                    pipeline = pipeline.getPipeTo();
                }
            }
        } catch (IOException e) {
            log.error("Failed to run pipeline", e);
        }
    }

    private Set<String> expandQueries(String query, List<Map<String, FieldResult<?>>> prevResults, String schema) {
        Set<String> queries = new HashSet<>();
        Pattern placeholderPattern = Pattern.compile("\\$\\{([^}]+)}");

        List<Placeholder> placeholders = new ArrayList<>();
        boolean shouldExpand = false;
        Matcher matches = placeholderPattern.matcher(query);

        while (matches.find()) {
            String placeholderName = matches.group(1);
            placeholders.add(new Placeholder(placeholderName, matches.start(), matches.end()));
            shouldExpand = shouldExpand || !placeholderName.equalsIgnoreCase("schema");
        }

        // Reverse list so that we do string replacements from the end to avoid offset changes
        Collections.reverse(placeholders);

        if (shouldExpand) {
            for (Map<String, FieldResult<?>> prevResult : prevResults) {
                String newQuery = query;

                for (Placeholder placeholder : placeholders) {
                    String placeholderValue = placeholder.name.equalsIgnoreCase("schema")
                            ? schema
                            : String.valueOf(prevResult.get(placeholder.name).getValue());

                    if (placeholderValue != null) {
                        newQuery = newQuery.substring(0, placeholder.startLocation)
                                + placeholderValue
                                + newQuery.substring(placeholder.endLocation);
                    } else {
                        throw new IllegalArgumentException("Unsupported placeholder, no result value to use");
                    }
                }

                queries.add(newQuery);
            }
        } else {
            String newQuery = query;

            for (Placeholder placeholder : placeholders) {
                if (placeholder.name.equalsIgnoreCase("schema")) {
                    newQuery = newQuery.substring(0, placeholder.startLocation)
                            + schema
                            + newQuery.substring(placeholder.endLocation);
                } else {
                    throw new IllegalArgumentException("Unsupported placeholder, requires expansion");
                }
            }

            queries.add(newQuery);
        }

        return queries;
    }

    @Data
    private static class Placeholder {

        private final String name;
        private final int startLocation;
        private final int endLocation;

    }

    private List<Map<String, FieldResult<?>>> retrieveRowResults(Query<?> query) {
        List<?> rawResults = query.getResultList();
        List<String> columnNames = pullColumnNames(query.getQueryString());

        List<Map<String, FieldResult<?>>> results = new ArrayList<>();

        for (Object o : rawResults) {
            if (columnNames.size() == 1) {
                results.add(mapRowToFieldResults(new Object[]{o}, columnNames));
            } else {
                if (o instanceof Object[]) {
                    results.add(mapRowToFieldResults((Object[]) o, columnNames));
                } else {
                    throw new IllegalStateException("Expected multiple columns in result");
                }
            }
        }

        return results;
    }

    private Map<String, FieldResult<?>> mapRowToFieldResults(Object[] rowValues, List<String> columnNames) {
        Map<String, FieldResult<?>> mappedResults = new LinkedHashMap<>();

        for (int i = 0; i < rowValues.length; ++i) {
            Object fieldValue = rowValues[i];

            mappedResults.put(
                    columnNames.get(i),
                    new FieldResult(columnNames.get(i), fieldValue == null ? null : fieldValue.getClass(), fieldValue));
        }

        return mappedResults;
    }

    private List<String> pullColumnNames(String query) {
        List<String> columnNames = new ArrayList<>();
        Scanner scanner = new Scanner(query);

        String columnCandidate = null;
        boolean hasMore = true;

        if (scanner.next().equalsIgnoreCase("select")) {
            while (hasMore && scanner.hasNext()) {
                String nextValue = scanner.next();

                if (nextValue.equalsIgnoreCase("from")) {
                    columnNames.add(columnCandidate);
                    hasMore = false;
                } else {
                    if (columnCandidate == null) {
                        if (nextValue.endsWith(",")) {
                            nextValue = nextValue.substring(0, nextValue.length() - 1);
                            columnNames.add(nextValue);
                        } else {
                            columnCandidate = nextValue;
                        }
                    } else {
                        if (nextValue.equals(",")) {
                            columnNames.add(columnCandidate);
                        } else if (nextValue.endsWith(",")) {
                            nextValue = nextValue.substring(0, nextValue.length() - 1);
                            columnNames.add(nextValue);
                        } else {
                            columnCandidate = nextValue;
                        }
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("Query was not a select query");
        }

        return columnNames;
    }

    @Data
    private static class FieldResult<T> {

        private final String columnName;
        private final Class<T> valueType;
        private final T value;

    }

}
