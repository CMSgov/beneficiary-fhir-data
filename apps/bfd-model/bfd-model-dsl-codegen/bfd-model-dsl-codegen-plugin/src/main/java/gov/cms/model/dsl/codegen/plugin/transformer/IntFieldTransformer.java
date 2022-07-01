package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/** Implementation of {@link FieldTransformer} for use with native integer fields. */
public class IntFieldTransformer implements FieldTransformer {
  /**
   * {@inheritDoc}
   *
   * <p>Generate code to call either {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyOptionalInt} when the field is optional.
   * Required int fields are unsupported so an exception is thrown to indicate the error.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return a statement calling a method to copy the value
   * @throws IllegalArgumentException if the field is not optional
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    if (!transformation.isOptional()) {
      throw new IllegalArgumentException("non-optional ints are not currently supported");
    }
    return CodeBlock.builder()
        .addStatement(
            "$L.copyOptionalInt($L, $L, $L)",
            TRANSFORMER_VAR,
            getter.createHasRef(transformation),
            getter.createGetRef(transformation),
            setter.createSetRef(column))
        .build();
  }
}
