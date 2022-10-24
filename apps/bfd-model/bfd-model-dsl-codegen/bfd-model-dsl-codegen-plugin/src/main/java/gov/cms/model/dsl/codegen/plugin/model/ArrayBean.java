package gov.cms.model.dsl.codegen.plugin.model;

import com.google.common.base.Strings;
import gov.cms.model.dsl.codegen.plugin.model.validation.JavaName;
import gov.cms.model.dsl.codegen.plugin.model.validation.MappingExists;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Model class for array mappings in DSL. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArrayBean implements ModelBean {
  /** Name of field in source object to copy array data from. */
  @NotNull @JavaName private String from;

  /** Name of field in destination object to copy array data to. */
  @NotNull @JavaName private String to;

  /** Name of DSL mapping for the array element objects. */
  @NotNull @JavaName @MappingExists private String mapping;

  /**
   * Prefix to use in error messages for any errors encountered when copying data to array elements.
   */
  @NotEmpty private String namePrefix;

  /** Name of field in array element objects to store link to the parent object. */
  @JavaName private String parentField;

  /**
   * Tests whether or not the array element objects have a field to hold a reference to the parent
   * object.
   *
   * @return true if the {@link ArrayBean#parentField} has a non-empty value
   */
  public boolean hasParentField() {
    return !Strings.isNullOrEmpty(parentField);
  }

  @Override
  public String getDescription() {
    return String.format("array of %s", mapping);
  }
}
