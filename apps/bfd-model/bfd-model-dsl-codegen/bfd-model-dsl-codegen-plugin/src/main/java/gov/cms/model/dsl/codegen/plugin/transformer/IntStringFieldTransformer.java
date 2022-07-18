package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;

/**
 * Implementation of {@link FieldTransformer} for use with int fields that are stored as strings in
 * the source but native ints in the destination.
 */
public class IntStringFieldTransformer implements FieldTransformer {
  /**
   * {@inheritDoc}
   *
   * <p>Generate code to call either {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyIntString} or {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer#copyOptionalIntString} depending on whether
   * the field is optional.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return a statement calling a method to copy the value
   */
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter) {
    return transformation.isOptional()
        ? CodeBlock.builder()
            .addStatement(
                "$L.copyOptionalIntString($L, $L, $L, $L)",
                TRANSFORMER_VAR,
                TransformerUtil.createFieldNameForErrorReporting(mapping, column),
                getter.createHasRef(transformation),
                getter.createGetRef(transformation),
                setter.createSetRef(column))
            .build()
        : CodeBlock.builder()
            .addStatement(
                "$L.copyIntString($L, $L, $L, $L)",
                TRANSFORMER_VAR,
                TransformerUtil.createFieldNameForErrorReporting(mapping, column),
                column.isNullable(),
                getter.createGetCall(transformation),
                setter.createSetRef(column))
            .build();
  }
}
