package gov.cms.model.rda.codegen.plugin.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnumTypeBean {
  private String name;
  @Singular private List<String> values = new ArrayList<>();

  public String findValue(String value) {
    if (!values.contains(value)) {
      throw new IllegalArgumentException(
          String.format("reference to unknown enum value %s in enum %s", value, name));
    } else {
      return value;
    }
  }

  public boolean isInnerClass() {
    return name.indexOf('.') < 0;
  }

  public String enumPackageName() {
    return ModelUtil.packageName(name);
  }

  public String enumClassName() {
    return ModelUtil.className(name);
  }
}
