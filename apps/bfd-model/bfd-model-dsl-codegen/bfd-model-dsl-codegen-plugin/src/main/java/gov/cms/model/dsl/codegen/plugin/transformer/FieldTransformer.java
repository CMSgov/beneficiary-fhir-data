package gov.cms.model.dsl.codegen.plugin.transformer;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.plugin.accessor.Getter;
import gov.cms.model.dsl.codegen.plugin.accessor.Setter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.List;

/**
 * A FieldTransformer is an object that generates java code for a specific type of field
 * transformation. Note the plugin does not do any data movement so these objects don't actually
 * interact with data. Instead they interact with objects from the DSL model that define classes and
 * entity types, etc and generate code for methods that implement the data movement.
 *
 * <p>The plugin will use a mapping of names to instances of this interface to select the proper
 * generator to use for each field in the model. See {@link
 * TransformerUtil#selectTransformerForField} for details.
 */
public interface FieldTransformer {

  /**
   * Variable/argument name used for holding the source object from which data is being extracted.
   */
  String SOURCE_VAR = "from";

  /**
   * Variable/argument name used for holding the destination object to which data is being copied.
   */
  String DEST_VAR = "to";

  /**
   * Variable/argument name used for holding the {@link DataTransformer} object used to copy data.
   */
  String TRANSFORMER_VAR = "transformer";

  /** Variable/argument name used for holding the current timestamp. */
  String NOW_VAR = "now";

  /** Variable/argument name used for holding the lambda used to hash MBI values. */
  String HASHER_VAR = "idHasher";

  /**
   * Variable/argument name used for holding the name of the field used when generating
   * transformation failure error messages.
   */
  String NAME_PREFIX_VAR = "namePrefix";

  /** A {@link CodeBlock} containing code to read the current timestamp. */
  CodeBlock NOW_VALUE = CodeBlock.of("$L", NOW_VAR);

  /**
   * Variable/Argument name used for holding the {@link
   * gov.cms.model.dsl.codegen.library.EnumStringExtractor} instance used when transforming an enum
   * value.
   */
  String ENUM_FACTORY_VAR = "enumExtractorFactory";

  /**
   * Generate a code block that will copy the field from the source object to the dest object using
   * a {@link DataTransformer} object. The returned {@link CodeBlock} will be inserted into a method
   * definition by our caller. That method will have variables for the source, destination, and
   * transformer objects that use the standard names defined in this interface as constants. For
   * example, the generated code can use {@link FieldTransformer#SOURCE_VAR} to reference the RDA
   * API source object, {@link FieldTransformer#DEST_VAR} to reference the destination entity
   * object, and {@link FieldTransformer#TRANSFORMER_VAR} to reference the {@link DataTransformer}.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param getter {@link Getter} implementation used to generate code to read from source field
   * @param setter {@link Setter} implementation used to generate code to write to the destination
   *     field
   * @return {@link CodeBlock} containing code to perform the transformation
   */
  CodeBlock generateCodeBlock(
      MappingBean mapping,
      ColumnBean column,
      TransformationBean transformation,
      Getter getter,
      Setter setter);

  /**
   * Generate a list of {@link FieldSpec} objects for any fields that need to be defined in the
   * generated class in order for our data transformations to work properly. Most transformers do
   * not require any special fields but some do (e.g. {@link EnumValueIfPresentTransformer}). The
   * plugin is required to call this method and generate all of the required fields in the class it
   * creates to transform message objects into entity objects.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @return zero or more {@link FieldSpec} objects to be added to generated transformer class
   */
  default List<FieldSpec> generateFieldSpecs(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return List.of();
  }

  /**
   * Generate a list of {@link CodeBlock} objects for any fields that need to be defined in the
   * generated class in order for our data transformations to work properly. Most transformers do
   * not require any special fields but some do (e.g. {@link EnumValueIfPresentTransformer}). The
   * plugin is required to call this method and use the code in this list to add initializers for
   * each of the fields to the object's constructor.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @return zero or more {@link CodeBlock} objects to be added to generated transformer class
   *     constructor
   */
  default List<CodeBlock> generateFieldInitializers(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return List.of();
  }
}
