package gov.cms.bfd.pipeline.rda.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import gov.cms.bfd.pipeline.rda.insights.models.AppConfig;
import gov.cms.bfd.pipeline.rda.insights.models.DbConfig;
import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class Application {

    private static final String EXTERNAL_CONFIG_FLAG = "e";

    public static void main(String[] args) {
        try {
            Properties dbConfigProperties = new Properties();
            Map<String, Collection<String>> appYamlConfigMap;
            Set<Pipeline> pipelines;

            Options options = new Options()
                    .addOption(EXTERNAL_CONFIG_FLAG, true, "Path to yaml file containing run configs");

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(EXTERNAL_CONFIG_FLAG)) {
                AppConfig appConfig = readYamlConfig(cmd.getOptionValue(EXTERNAL_CONFIG_FLAG));
                appYamlConfigMap = readDbConfig(appConfig.getDb());
                addIfNotNull("app.resourceDir", appConfig.getResourceDir(), appYamlConfigMap);
                addIfNotNull("app.outputDir", appConfig.getOutputDir(), appYamlConfigMap);
                pipelines = appConfig.getPipelines();
            } else {
                log.warn("No configurations to run");
                appYamlConfigMap = new HashMap<>();
                pipelines = new HashSet<>();
            }

            try (InputStream input = Application.class.getClassLoader().getResourceAsStream("project.properties")) {
                if (input != null) {
                    dbConfigProperties.load(input);
                } else {
                    log.warn("No project properties file was found.");
                }
            } catch (IOException ex) {
                log.warn("Could not read from project properties file.");
            }

            ConfigLoader configLoader = ConfigLoader.builder()
                    // System properties if nothing else
                    .addProperties(dbConfigProperties)
                    // But prefer yaml configurations
                    .add(appYamlConfigMap::get)
                    .build();

            RdaInsights rdaInsights = new RdaInsights(configLoader, pipelines);
            rdaInsights.init();
            rdaInsights.run();

        } catch (ParseException | IOException e) {
            log.error("Failed to load configurations", e);
            System.exit(1);
        }
    }

    private static AppConfig readYamlConfig(String yamlFilePath) throws IOException {
        try (FileReader reader = new FileReader(yamlFilePath)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(reader, AppConfig.class);
        }
    }

    private static Map<String, Collection<String>> readDbConfig(DbConfig dbConfig) {
        Map<String, Collection<String>> map = new HashMap<>();
        addIfNotNull("db." + DbConfig.Fields.url, dbConfig.getUrl(), map);
        addIfNotNull("db." + DbConfig.Fields.username, dbConfig.getUsername(), map);
        addIfNotNull("db." + DbConfig.Fields.password, dbConfig.getPassword(), map);
        addIfNotNull("db." + DbConfig.Fields.schema, dbConfig.getSchema(), map);
        addIfNotNull("db." + DbConfig.Fields.driver, dbConfig.getDriver(), map);
        addIfNotNull("db." + DbConfig.Fields.dialect, dbConfig.getDialect(), map);
        addIfNotNull("db." + DbConfig.Fields.showSql, dbConfig.getDialect(), map);
        return map;
    }

    private static <K, V> void addIfNotNull(K key, V value, Map<K, Collection<V>> map) {
        if (value != null) {
            map.put(key, Collections.singleton(value));
        }
    }

}
