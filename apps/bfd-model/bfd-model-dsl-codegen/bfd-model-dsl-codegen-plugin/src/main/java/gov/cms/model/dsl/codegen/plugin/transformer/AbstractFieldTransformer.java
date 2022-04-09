package gov.cms.model.dsl.codegen.plugin.transformer;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.plugin.PoetUtil;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A FieldTransformGenerator is an object that generates java code for a specific type of field
 * transformation. Note the plugin does not actually do any data movement so these objects don't
 * actually interact with data. Instead they interact with objects from the DSL model that define
 * classes and entity types, etc and use these to generate methods to actually accomplish data
 * movement when that method is called by the program that uses the code we're generating.
 *
 * <p>The plugin will use a mapping of names to instances of this interface to select the proper
 * generator to use for each field in the model.
 */
public abstract class AbstractFieldTransformer {
  public static final String SOURCE_VAR = "from";
  public static final String DEST_VAR = "to";
  public static final String TRANSFORMER_VAR = "transformer";
  public static final String NOW_VAR = "now";
  public static final String HASHER_VAR = "idHasher";
  public static final String NAME_PREFIX_VAR = "namePrefix";
  public static final CodeBlock NOW_VALUE = CodeBlock.of("$L", NOW_VAR);
  public static final String ENUM_FACTORY_VAR = "enumExtractorFactory";

  /**
   * Generate a code block that would copy the field from the source object to the dest object using
   * the transformer object. The CodeBlock will be inserted into a method definition by the caller.
   * That method will have variables for the source, destination, and transformer objects. The
   * generated code can use the {@code SOURCE_VAR} to reference the RDA * API source object, {@code
   * DEST_VAR} to reference the destination entity object, and {@code * TRANSFORMER_VAR} to
   * reference the {@link DataTransformer}.
   *
   * @param mapping The mapping that contains the field.
   * @param transformation The specific field to be copied.
   * @param toCodeGenerator
   * @return CodeBlock for the generated block of code
   */
  public abstract CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      FromCodeGenerator fromCodeGenerator,
      ToCodeGenerator toCodeGenerator);

  public List<FieldSpec> generateFieldSpecs(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return ImmutableList.of();
  }

  public List<CodeBlock> generateFieldInitializers(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return ImmutableList.of();
  }

  protected CodeBlock fieldNameReference(MappingBean mapping, ColumnBean column) {
    return CodeBlock.of(
        "$L + $T.Fields.$L",
        NAME_PREFIX_VAR,
        PoetUtil.toClassName(mapping.getEntityClassName()),
        column.getName());
  }

  /**
   * Helper method to parse the {@code from} of the specified Transformation and call one of two
   * methods depending on whether the field is a simple field or a nested field (i.e. one with a
   * field and a property of that field). Only a single period is supported in the field name. This
   * is a "hole in the middle" pattern to centralize the field name parsing in one place. The names
   * passed to the methods are already capitalized so they are ready to have has or get prepended to
   * them as needed.
   *
   * @param transformation defines the {@code from} field
   * @param simpleProperty accepts a field name and returns a CodeBlock
   * @param nestedProperty accepts a field and property name and returns a CodeBlock
   * @return CodeBlock created by appropriate method
   */
  public static CodeBlock transformationPropertyCodeBlock(
      TransformationBean transformation,
      Function<String, CodeBlock> simpleProperty,
      BiFunction<String, String, CodeBlock> nestedProperty) {
    final String from = TransformerUtil.capitalize(transformation.getFrom());
    final int dotIndex = from.indexOf('.');
    if (dotIndex < 0) {
      return simpleProperty.apply(from);
    } else {
      final String fieldName = from.substring(0, dotIndex);
      final String propertyName = TransformerUtil.capitalize(from.substring(dotIndex + 1));
      return nestedProperty.apply(fieldName, propertyName);
    }
  }
}
