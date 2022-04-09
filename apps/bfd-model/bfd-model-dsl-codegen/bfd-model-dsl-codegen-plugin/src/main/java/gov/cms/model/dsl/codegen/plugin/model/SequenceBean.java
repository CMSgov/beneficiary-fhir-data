package gov.cms.model.dsl.codegen.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceBean {
  String name;
  int allocationSize = 1;
}
