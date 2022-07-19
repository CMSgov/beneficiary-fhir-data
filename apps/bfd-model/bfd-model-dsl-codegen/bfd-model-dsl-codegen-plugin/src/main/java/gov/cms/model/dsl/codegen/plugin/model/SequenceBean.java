package gov.cms.model.dsl.codegen.plugin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model object containing attributes of a database sequence used to generate ids for a table. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceBean {
  /** Name of the database sequence. */
  private String name;
  /** Number of values allocated per call to the sequence. */
  @Builder.Default private int allocationSize = 1;
}
