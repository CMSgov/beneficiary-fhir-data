package gov.cms.model.dsl.codegen.plugin.model;

import gov.cms.model.dsl.codegen.library.ExternalTransformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalTransformationBean {
  /**
   * Name of the constructor parameter and field in the generated transformation class by which this
   * external {@link ExternalTransformation} will be referenced. A mapping can have zero or more of
   * these.
   */
  private String name;
}
