package gov.cms.bfd.pipeline.rda.insights.models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PipelineValues {

    private final List<ValueSet> valueSets = new ArrayList<>();
    private int currentIndex = -1;

    public List<ValueSet> valueSets() {
        return List.copyOf(valueSets);
    }

    public ValueSet get(int index) {
        return valueSets.get(index);
    }

    public boolean isEmpty() {
        return valueSets.isEmpty();
    }

    public void clear() {
        valueSets.clear();
        currentIndex = -1;
    }

    public void addAll(PipelineValues pipelineValues) {
        valueSets.addAll(pipelineValues.valueSets);
        currentIndex = valueSets.size() - 1;
    }

    public ValueSet createNewSet() {
        valueSets.add(new ValueSet());
        currentIndex = valueSets.size() - 1;
        return valueSets.get(currentIndex);
    }

    public ValueSet currentSet() {
        return valueSets.get(currentIndex);
    }

    public static class ValueSet {

        private final Map<String, FieldResult<?>> set = new LinkedHashMap<>();

        public void addResult(String key, Object value) {
            set.put(key, new FieldResult(key, value == null ? null : value.getClass(), value));
        }

        public FieldResult get(String key) {
            return set.get(key);
        }

        public Set<String> keySet() {
            return set.keySet();
        }

    }

}
