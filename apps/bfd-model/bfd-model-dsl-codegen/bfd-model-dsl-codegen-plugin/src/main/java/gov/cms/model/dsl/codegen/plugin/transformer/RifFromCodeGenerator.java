package gov.cms.model.dsl.codegen.plugin.transformer;

import static gov.cms.model.dsl.codegen.plugin.transformer.AbstractFieldTransformer.SOURCE_VAR;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.library.RifObjectWrapper;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.function.Function;

/**
 * Implementation of MessageCodeGenerator that requires the message objects to be {@link
 * RifObjectWrapper} objects. Also requires that the RIF header label must be specified as the
 * {@code from} in the transformation.
 */
public class RifFromCodeGenerator implements FromCodeGenerator {
  public static final FromCodeGenerator Instance = new RifFromCodeGenerator();

  @Override
  public CodeBlock createHasRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation, fieldName -> CodeBlock.of("() -> $L.hasValue($S)", SOURCE_VAR, fieldName));
  }

  @Override
  public CodeBlock createHasCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation, fieldName -> CodeBlock.of("$L.hasValue($S)", SOURCE_VAR, fieldName));
  }

  @Override
  public CodeBlock createGetRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of(
                "() -> $L.getValue($S, $S)",
                SOURCE_VAR,
                fieldName,
                transformation.getDefaultValue()));
  }

  @Override
  public CodeBlock createGetCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of(
                "$L.getValue($S, $S)", SOURCE_VAR, fieldName, transformation.getDefaultValue()));
  }

  /**
   * Helper method to parse the {@code from} of the specified Transformation and call one of two
   * methods depending on whether the field is a simple field or a nested field (i.e. one with a
   * field and a property of that field). Only a single period is supported in the field name. This
   * is a "hole in the middle" pattern to centralize the field name parsing in one place.
   *
   * @param transformation defines the {@code from} field
   * @param simpleProperty accepts a field label and returns a CodeBlock
   * @return CodeBlock created by appropriate method
   */
  private static CodeBlock transformationPropertyCodeBlock(
      TransformationBean transformation, Function<String, CodeBlock> simpleProperty) {
    final String from = transformation.getFrom();
    final int dotIndex = from.indexOf('.');
    if (dotIndex < 0) {
      return simpleProperty.apply(from);
    } else {
      throw new IllegalArgumentException("RIF parsing only supports simple properties");
    }
  }
}
