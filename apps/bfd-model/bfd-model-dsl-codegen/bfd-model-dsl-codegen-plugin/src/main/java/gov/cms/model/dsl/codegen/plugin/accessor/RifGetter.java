package gov.cms.model.dsl.codegen.plugin.accessor;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.library.RifObjectWrapper;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import gov.cms.model.dsl.codegen.plugin.transformer.FieldTransformer;
import java.util.function.Function;

/**
 * Implementation of {@link Getter} that requires the message objects to be {@link RifObjectWrapper}
 * objects. Also requires that the RIF header label must be specified as the {@code from} in the
 * transformation.
 */
public class RifGetter implements Getter {
  /** Sharable singleton instance. */
  public static final Getter Instance = new RifGetter();

  /**
   * {@inheritDoc}
   *
   * <p>This implementation requires that the source object is a {@link RifObjectWrapper} and the
   * {@code from} is a RIF header name.
   */
  @Override
  public CodeBlock createHasRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("() -> $L.hasValue($S)", FieldTransformer.SOURCE_VAR, fieldName));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation requires that the source object is a {@link RifObjectWrapper} and the
   * {@code from} is a RIF header name.
   */
  @Override
  public CodeBlock createHasCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("$L.hasValue($S)", FieldTransformer.SOURCE_VAR, fieldName));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation requires that the source object is a {@link RifObjectWrapper} and the
   * {@code from} is a RIF header name.
   */
  @Override
  public CodeBlock createGetRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of(
                "() -> $L.getValue($S, $S)",
                FieldTransformer.SOURCE_VAR,
                fieldName,
                transformation.getDefaultValue()));
  }

  /**
   * {@inheritDoc}
   *
   * <p>This implementation requires that the source object is a {@link RifObjectWrapper} and the
   * {@code from} is a RIF header name.
   */
  @Override
  public CodeBlock createGetCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName ->
            CodeBlock.of(
                "$L.getValue($S, $S)",
                FieldTransformer.SOURCE_VAR,
                fieldName,
                transformation.getDefaultValue()));
  }

  /**
   * Helper method to parse the {@code from} of the specified {@link TransformationBean} and call a
   * lambda that generates code to get the value of the property. Only simple property names are
   * allowed (i.e. no nested properties inside of other objects).
   *
   * @param transformation defines the {@code from} field
   * @param simpleProperty accepts a field label and returns a {@link CodeBlock}
   * @return {@link CodeBlock} created by appropriate method
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
