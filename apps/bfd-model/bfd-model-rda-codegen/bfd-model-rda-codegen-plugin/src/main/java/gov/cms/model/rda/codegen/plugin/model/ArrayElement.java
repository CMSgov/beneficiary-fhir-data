package gov.cms.model.rda.codegen.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArrayElement {
  private String from;
  private String to;
  private String mapping;
  private String namePrefix;
}
