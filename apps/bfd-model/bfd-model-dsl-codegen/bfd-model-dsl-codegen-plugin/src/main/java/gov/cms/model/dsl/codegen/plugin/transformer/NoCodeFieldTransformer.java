package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/**
 * Implementation of {@link FieldTransformer} for use with fields that should not have any
 * transformation code generated for them. Used for fields with special names that are handled
 * elsewhere. Use of this transformer is controlled by {@link TransformerUtil#NoCodeFromNamesRegex}
 * applied to the value of {@link TransformationBean#from}.
 */
public class NoCodeFieldTransformer implements FieldTransformer {
  /**
   * {@inheritDoc}
   *
   * <p>Simply returns an empty {@link CodeBlock} to the caller.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return an empty {@link CodeBlock}
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    return CodeBlock.builder().build();
  }
}
