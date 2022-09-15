package gov.cms.bfd.pipeline.rda.insights.filters;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class ReadCSVFilter extends PipelineFilter {

    public ReadCSVFilter(ConfigLoader appConfigs) {
        super(appConfigs);
    }

    @Override
    public void run(Pipeline pipeline, PipelineValues pipelineValues) {
        pipelineValues.clear();

        try (CSVReader csvReader = new CSVReader(new FileReader(Path.of(getResourceDir(), pipeline.getTarget()).toFile()))) {
            List<String[]> lines = csvReader.readAll();

            if (!lines.isEmpty()) {
                List<String> columnNames = Arrays.asList(lines.get(0));

                for (int i = 1; i < lines.size(); ++i) {
                    pipelineValues.createNewSet();
                    String[] rowData = lines.get(i);

                    for (int col = 0; col < columnNames.size(); ++col) {
                        final String columnName = columnNames.get(col);
                        pipelineValues.currentSet().addResult(columnName, rowData[col]);
                    }
                }
            }
        } catch (CsvException | IOException e) {
            log.error(String.format("Failed to read csv '%s'", pipeline.getTarget()));
            throw new RuntimeException("Failed to read CSV file", e);
        }
    }

}
