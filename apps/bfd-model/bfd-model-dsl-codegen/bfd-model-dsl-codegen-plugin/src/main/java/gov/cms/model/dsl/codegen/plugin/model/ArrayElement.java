package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model class for array mappings in DSL. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArrayElement {
  /** Name of field in source object to copy array data from. */
  private String from;
  /** Name of field in destination object to copy array data to. */
  private String to;
  /** Name of DSL mapping for the array element objects. */
  private String mapping;
  /**
   * Prefix to use in error messages for any errors encountered when copying data to array elements.
   */
  private String namePrefix;
  /** Name of field in array element objects to store link to the parent object. */
  private String parentField;

  /**
   * Tests whether or not the array element objects have a field to hold a reference to the parent
   * object.
   *
   * @return true if the {@link ArrayElement#parentField} has a non-empty value
   */
  public boolean hasParentField() {
    return !Strings.isNullOrEmpty(parentField);
  }
}
