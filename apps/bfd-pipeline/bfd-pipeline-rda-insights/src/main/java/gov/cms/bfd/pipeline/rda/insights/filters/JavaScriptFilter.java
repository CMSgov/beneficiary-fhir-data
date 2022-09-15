package gov.cms.bfd.pipeline.rda.insights.filters;

import gov.cms.bfd.pipeline.rda.insights.models.Pipeline;
import gov.cms.bfd.pipeline.rda.insights.models.PipelineValues;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import lombok.extern.slf4j.Slf4j;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class JavaScriptFilter extends PipelineFilter {

    public JavaScriptFilter(ConfigLoader appConfigs) {
        super(appConfigs);
    }

    @Override
    public void run(Pipeline pipeline, PipelineValues pipelineValues) {
        try {
            Path scriptPath = Path.of(getResourceDir(), pipeline.getTarget());
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("JavaScript");
            engine.eval(Files.newBufferedReader(scriptPath, StandardCharsets.UTF_8));
            Invocable invocable = (Invocable) engine;

            for (PipelineValues.ValueSet valueSet : pipelineValues.valueSets()) {
                Map<String, Object> jsonMap = valueSet
                        .keySet()
                        .stream()
                        .map(key -> Map.entry(key, valueSet.get(key).getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                Object result = invocable.invokeFunction("main", jsonMap);

                if (result instanceof Map) {
                    //unchecked - We should be good.
                    //noinspection unchecked
                    Map<String, Object> resultMap = (Map<String, Object>) result;
                    valueSet.putAll(resultMap);
                }
            }
        } catch (IOException | ScriptException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to read/execute script", e);
        }
    }

}
