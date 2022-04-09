package gov.cms.model.dsl.codegen.plugin.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RootBean {
  @Singular private List<MappingBean> mappings = new ArrayList<>();

  public void addMappingsFrom(RootBean other) {
    mappings.addAll(other.mappings);
  }

  public Optional<MappingBean> findMappingWithId(String id) {
    return mappings.stream().filter(m -> m.getId().equals(id)).findAny();
  }

  public Optional<MappingBean> findMappingWithEntityClassName(String entityClassName) {
    return mappings.stream().filter(m -> m.getEntityClassName().equals(entityClassName)).findAny();
  }
}
