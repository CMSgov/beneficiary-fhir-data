package gov.cms.model.dsl.codegen.plugin.model;

import gov.cms.model.dsl.codegen.library.ExternalTransformation;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for defining the name of an external transformation function to call when
 * transforming a message object into an entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalTransformationBean implements ModelBean {
  /**
   * Name of the constructor parameter and field in the generated transformation class by which this
   * external {@link ExternalTransformation} will be referenced. A mapping can have zero or more of
   * these.
   */
  @NotNull @JavaName private String name;

  @Override
  public String getDescription() {
    return "external transformation " + name;
  }
}
