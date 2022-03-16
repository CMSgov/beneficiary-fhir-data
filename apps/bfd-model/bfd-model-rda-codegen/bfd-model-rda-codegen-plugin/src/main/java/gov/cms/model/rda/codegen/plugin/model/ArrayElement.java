package gov.cms.model.rda.codegen.plugin.model;

import com.google.common.base.Strings;
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
  private String parentField;

  public boolean hasParentField() {
    return !Strings.isNullOrEmpty(parentField);
  }
}
