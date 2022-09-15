package gov.cms.bfd.pipeline.rda.insights.filters;

import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.pipeline.rda.insights.util.HibernateConnections;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SQLFilter extends PipelineFilter {

    private final String schema;
    private final HibernateConnections connections;

    public SQLFilter(ConfigLoader appConfigs) {
        super(appConfigs);
        this.schema = appConfigs.stringOption("db.schema").orElseThrow();
        this.connections = new HibernateConnections(appConfigs);
    }

    @Override
    public void run(Pipeline pipeline, PipelineValues pipelineValues) {
        try {
            String baseQuery = String.join("\n", Files
                    .readAllLines(Path.of(getResourceDir(), pipeline.getTarget())));
            Set<String> sqlQueries = expandQueries(baseQuery, pipelineValues, schema);
            pipelineValues.clear();

            Session session = connections.getSession();

            for (String sqlQuery : sqlQueries) {
                Transaction tx = session.beginTransaction();
                Query<?> query = session.createNativeQuery(sqlQuery);
                pipelineValues.addAll(retrieveRowResults(query));
                tx.commit();
            }
        } catch (IOException e) {
            log.error("Failed to run pipeline", e);
        }
    }

    private PipelineValues retrieveRowResults(Query<?> query) {
        List<?> rawResults = query.getResultList();
        List<String> columnNames = pullColumnNames(query.getQueryString());

        PipelineValues pipelineValues = new PipelineValues();

        for (Object o : rawResults) {
            pipelineValues.createNewSet();

            if (columnNames.size() == 1) {
                addRowToPipelineValues(pipelineValues.currentSet(), new Object[]{o}, columnNames);
            } else {
                if (o instanceof Object[]) {
                    addRowToPipelineValues(pipelineValues.currentSet(), (Object[]) o, columnNames);
                } else {
                    throw new IllegalStateException("Expected multiple columns in result");
                }
            }
        }

        return pipelineValues;
    }

    private void addRowToPipelineValues(PipelineValues.ValueSet valueSet, Object[] rowValues, List<String> columnNames) {
        for (int i = 0; i < rowValues.length; ++i) {
            valueSet.addResult(columnNames.get(i), rowValues[i]);
        }
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

    private Set<String> expandQueries(String query, PipelineValues pipelineValues, String schema) {
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
            for (PipelineValues.ValueSet valueSet : pipelineValues.valueSets()) {
                String newQuery = query;

                for (Placeholder placeholder : placeholders) {
                    String placeholderValue = placeholder.name.equalsIgnoreCase("schema")
                            ? schema
                            : String.valueOf(valueSet.get(placeholder.name).getValue());

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

}
