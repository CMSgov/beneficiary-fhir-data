package gov.cms.model.rda.codegen.plugin.transformer;

import static gov.cms.model.rda.codegen.plugin.transformer.AbstractFieldTransformer.*;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.rda.codegen.plugin.model.TransformationBean;

public class GrpcFromCodeGenerator implements FromCodeGenerator {
  public static final FromCodeGenerator Instance = new GrpcFromCodeGenerator();

  /**
   * Generates a {@code Supplier<Boolean>} compatible CodeBlock that that returns true if the field
   * is present in the message.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for a lambda function
   */
  public CodeBlock createHasRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("$L::has$L", SOURCE_VAR, fieldName),
        (fieldName, propertyName) ->
            CodeBlock.of(
                "() -> $L.has$L() && $L.get$L().has$L()",
                SOURCE_VAR,
                fieldName,
                SOURCE_VAR,
                fieldName,
                propertyName));
  }

  /**
   * Generates an expression CodeBlock that returns true if the field is present in the message.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for an expression
   */
  public CodeBlock createHasCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("$L.has$L()", SOURCE_VAR, fieldName),
        (fieldName, propertyName) ->
            CodeBlock.of(
                "($L.has$L() && $L.get$L().has$L())",
                SOURCE_VAR,
                fieldName,
                SOURCE_VAR,
                fieldName,
                propertyName));
  }

  /**
   * Generates a {@code Supplier<T>} compatible CodeBlock that that returns the value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for a lambda function
   */
  public CodeBlock createGetRef(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("$L::get$L", SOURCE_VAR, fieldName),
        (fieldName, propertyName) ->
            CodeBlock.of("() -> $L.get$L().get$L()", SOURCE_VAR, fieldName, propertyName));
  }

  /**
   * Generates an expression CodeBlock that that returns the value of the field.
   *
   * @param transformation defines the {@code from} field
   * @return CodeBlock for an expression
   */
  public CodeBlock createGetCall(TransformationBean transformation) {
    return transformationPropertyCodeBlock(
        transformation,
        fieldName -> CodeBlock.of("$L.get$L()", SOURCE_VAR, fieldName),
        (fieldName, propertyName) ->
            CodeBlock.of("$L.get$L().get$L()", SOURCE_VAR, fieldName, propertyName));
  }
}
