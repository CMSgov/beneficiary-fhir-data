package gov.cms.model.dsl.codegen.plugin.model;

import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

/** Model object containing attributes of a database sequence used to generate ids for a table. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceBean implements ModelBean {
  /** Name of the database sequence. */
  @NotNull @JavaName private String name;
  /** Number of values allocated per call to the sequence. */
  @Range(min = 1)
  @Builder.Default
  private int allocationSize = 1;

  @Override
  public String getDescription() {
    return "sequence " + name;
  }
}
