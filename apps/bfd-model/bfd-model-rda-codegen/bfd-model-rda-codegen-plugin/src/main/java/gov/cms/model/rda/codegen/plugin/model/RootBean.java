package gov.cms.model.rda.codegen.plugin.model;

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

  public Optional<MappingBean> findMappingWithId(String id) {
    return mappings.stream().filter(m -> m.getId().equals(id)).findAny();
  }
}
