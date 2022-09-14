package gov.cms.bfd.pipeline.rda.insights;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import gov.cms.bfd.pipeline.rda.insights.models.AppConfig;
import gov.cms.bfd.pipeline.rda.insights.models.DbConfig;
import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class Application {

    private static final String CONFIG_FILE = "config.yml";

    public static void main(String[] args) {
        try {
            Properties dbConfigProperties = new Properties();
            Map<String, Collection<String>> yamlDbConfigMap;
            Set<Pipeline> pipelines;

            AppConfig appConfig = readYamlConfig(CONFIG_FILE);
            yamlDbConfigMap = readDbConfig(appConfig.getDb());
            pipelines = appConfig.getPipelines();

            try (InputStream input = Application.class.getClassLoader().getResourceAsStream("project.properties")) {
                if (input != null) {
                    dbConfigProperties.load(input);
                } else {
                    log.warn("No project properties file was found.");
                }
            } catch (IOException ex) {
                log.warn("Could not read from project properties file.");
            }

            ConfigLoader dbConfigLoader = ConfigLoader.builder()
                    // System properties if nothing else
                    .add(property -> {
                        String value = dbConfigProperties.getProperty("db." + property);
                        return value == null ? null : Collections.singleton(value);
                    })
                    // But prefer yaml configurations
                    .add(yamlDbConfigMap::get)
                    .build();

            new RdaInsights(dbConfigLoader, pipelines).run();

        } catch (IOException e) {
            log.error("Failed to load configurations", e);
            System.exit(1);
        }
    }

    private static AppConfig readYamlConfig(String yamlFilePath) throws IOException {
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream(yamlFilePath)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readValue(input, AppConfig.class);
        }
    }

    private static Map<String, Collection<String>> readDbConfig(DbConfig dbConfig) {
        Map<String, Collection<String>> map = new HashMap<>();
        addIfNotNull(DbConfig.Fields.url, dbConfig.getUrl(), map);
        addIfNotNull(DbConfig.Fields.username, dbConfig.getUsername(), map);
        addIfNotNull(DbConfig.Fields.password, dbConfig.getPassword(), map);
        addIfNotNull(DbConfig.Fields.schema, dbConfig.getSchema(), map);
        addIfNotNull(DbConfig.Fields.driver, dbConfig.getDriver(), map);
        addIfNotNull(DbConfig.Fields.dialect, dbConfig.getDialect(), map);
        return Map.copyOf(map);
    }

    private static <K, V> void addIfNotNull(K key, V value, Map<K, Collection<V>> map) {
        if (value != null) {
            map.put(key, Collections.singleton(value));
        }
    }

}
