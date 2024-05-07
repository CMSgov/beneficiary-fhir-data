package gov.cms.model.dsl.codegen.plugin.transformer;

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.GenerateDataDictionaryFromDslMojo;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * Class containing static helper methods for implementing and finding implementations of {@link
 * FieldTransformer}.
 */
public class TransformerUtil {
  /**
   * The name used in {@code from} field of a {@link TransformationBean} to designate the
   * destination field as one that should be populated with the current timestamp rather than copied
   * from a source field.
   */
  public static final String TimestampFromName = "NOW";

  /**
   * The name used in {@code from} field of a {@link TransformationBean} to designate the
   * destination field requires no mapping. This can be used while authoring a DSL file as a
   * placeholder until an actual transformation has been selected.
   */
  public static final String NoMappingFromName = "NONE";

  /**
   * The name used in {@code from} field of a {@link TransformationBean} to designate the
   * destination field is one that is populated directly from a field of the same name in the
   * object's parent. This is used to copy the primary key values from a parent into an array
   * element.
   */
  public static final String ParentFromName = "PARENT";

  /**
   * The name used in {@code from} field of a {@link TransformationBean} to designate the
   * destination field is one that is populated directly from the current array index. This is used
   * to set the {@code priority} field an array element.
   */
  public static final String IndexFromName = "INDEX";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * IdHashFieldTransformer} should be used.
   */
  public static final String IdHashTransformName = "IdHash";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * EnumValueIfPresentTransformer} should be used.
   */
  public static final String EnumValueTransformName = "EnumValueIfPresent";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * MessageEnumFieldTransformer} should be used.
   */
  public static final String MessageEnumTransformName = "MessageEnum";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * TimestampFieldTransformer} should be used.
   */
  public static final String TimestampTransformName = "Now";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * RifTimestampFieldTransformer} should be used.
   */
  public static final String RifTimestampTransformName = "RifTimestamp";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * IntStringFieldTransformer} should be used.
   */
  public static final String IntStringTransformName = "IntString";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * ShortStringFieldTransformer} should be used.
   */
  public static final String ShortStringTransformName = "ShortString";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * LongStringFieldTransformer} should be used.
   */
  public static final String LongStringTransformName = "LongString";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * UintToShortFieldTransformer} should be used.
   */
  public static final String UintToShortTransformName = "UintToShort";

  /**
   * Fixed name for {@code transformer} field of {@link TransformationBean} to indicate {@link
   * Base64FieldTransformer} should be used.
   */
  public static final String Base64TransformerName = "Base64";

  /** Fixed name for {@code transformer} field. */
  public static final String IdentifierTransformName = "Identifier";

  /**
   * Regex used to detect special values in the {@code from} field of a {@link TransformationBean}
   * that indicate there is no corresponding {@link FieldTransformer}.
   */
  private static final Pattern NoCodeFromNamesRegex =
      Pattern.compile(String.format("%s|%s|%s", NoMappingFromName, ParentFromName, IndexFromName));

  /** Shared instance of {@link NoCodeFieldTransformer}. */
  private static final NoCodeFieldTransformer NoCodeInstance = new NoCodeFieldTransformer();

  /** Shared instance of {@link CharFieldTransformer}. */
  private static final CharFieldTransformer CharInstance = new CharFieldTransformer();

  /** Shared instance of {@link IntFieldTransformer}. */
  private static final IntFieldTransformer IntInstance = new IntFieldTransformer();

  /** Shared instance of {@link LongFieldTransformer}. */
  private static final LongFieldTransformer LongInstance = new LongFieldTransformer();

  /** Shared instance of {@link DateFieldTransformer}. */
  private static final DateFieldTransformer DateInstance = new DateFieldTransformer();

  /** Shared instance of {@link AmountFieldTransformer}. */
  private static final AmountFieldTransformer AmountInstance = new AmountFieldTransformer();

  /** Shared instance of {@link StringFieldTransformer}. */
  private static final StringFieldTransformer StringInstance = new StringFieldTransformer();

  /** Shared instance of {@link IdHashFieldTransformer}. */
  private static final IdHashFieldTransformer IdHashInstance = new IdHashFieldTransformer();

  /** Shared instance of {@link RifTimestampFieldTransformer}. */
  private static final RifTimestampFieldTransformer RifTimestampInstance =
      new RifTimestampFieldTransformer();

  /** Shared instance of {@link IntStringFieldTransformer}. */
  private static final IntStringFieldTransformer IntStringInstance =
      new IntStringFieldTransformer();

  /** Shared instance of {@link ShortStringFieldTransformer}. */
  private static final ShortStringFieldTransformer ShortStringInstance =
      new ShortStringFieldTransformer();

  /** Shared instance of {@link LongStringFieldTransformer}. */
  private static final LongStringFieldTransformer LongStringInstance =
      new LongStringFieldTransformer();

  /** Shared instance of {@link UintToShortFieldTransformer}. */
  private static final UintToShortFieldTransformer UintToShortInstance =
      new UintToShortFieldTransformer();

  /** Shared instance of {@link MessageEnumFieldTransformer}. */
  private static final MessageEnumFieldTransformer MessageEnumInstance =
      new MessageEnumFieldTransformer();

  /** Shared instance of {@link TimestampFieldTransformer}. */
  private static final TimestampFieldTransformer TimestampInstance =
      new TimestampFieldTransformer();

  /** Shared instance of {@link Base64FieldTransformer}. */
  private static final Base64FieldTransformer Base64Instance = new Base64FieldTransformer();

  /** Shared instance of {@link IdentifierTransformer}. */
  private static final IdentifierTransformer IdentifierInstance = new IdentifierTransformer();

  /**
   * {@link Map} used internally to recognized standard {@link TransformationBean} {@code
   * transformer} values and their corresponding {@link FieldTransformer} instances.
   */
  private static final Map<String, FieldTransformer> transformersByName =
      ImmutableMap.of(
          IdHashTransformName,
          IdHashInstance,
          TimestampTransformName,
          TimestampInstance,
          MessageEnumTransformName,
          MessageEnumInstance,
          EnumValueTransformName,
          new EnumValueIfPresentTransformer(),
          RifTimestampTransformName,
          RifTimestampInstance,
          IntStringTransformName,
          IntStringInstance,
          ShortStringTransformName,
          ShortStringInstance,
          LongStringTransformName,
          LongStringInstance,
          UintToShortTransformName,
          UintToShortInstance,
          Base64TransformerName,
          Base64Instance);

  /**
   * {@link Map} used internally to recognize standard transformer values and their corresponding
   * {@link FhirElementTransformer} instances.
   */
  private static final Map<String, FhirElementTransformer> fhirElementTransformersByName =
      ImmutableMap.of(IdentifierTransformName, IdentifierInstance);

  /**
   * {@link Map} used internally to recognized standard {@link TransformationBean} {@code from}
   * values and their corresponding {@link FieldTransformer} instances.
   */
  private static final Map<String, FieldTransformer> transformersByFrom =
      ImmutableMap.of(TimestampFromName, TimestampInstance);

  /** Prevent instance creation. */
  private TransformerUtil() {}

  /**
   * Scans all of the {@link MappingBean} transformations looking for any that require the caller to
   * provide a lambda to hash string values.
   *
   * @param mapping {@link MappingBean} to scan
   * @return true if caller must provide a hashing function
   */
  public static boolean mappingRequiresIdHasher(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .anyMatch(transform -> IdHashTransformName.equals(transform.getTransformer()));
  }

  /**
   * Scans all of the {@link MappingBean}s looking for any that require the caller to provide a
   * lambda to hash string values.
   *
   * @param mappings {@link Stream} of {@link MappingBean} to scan
   * @return true if caller must provide a hashing function
   */
  public static boolean anyMappingRequiresIdHasher(Stream<MappingBean> mappings) {
    return mappings.anyMatch(TransformerUtil::mappingRequiresIdHasher);
  }

  /**
   * Scans all of the {@link MappingBean} transformations looking for any that require the caller to
   * provide an {@link gov.cms.model.dsl.codegen.library.EnumStringExtractor} instance.
   *
   * @param mapping {@link MappingBean} to scan
   * @return true if caller must provide any enum string extractors
   */
  public static boolean mappingRequiresEnumExtractor(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .anyMatch(transform -> MessageEnumTransformName.equals(transform.getTransformer()));
  }

  /**
   * Capitalize the first letter of the string.
   *
   * @param name a name to be capitalized
   * @return new string with just the first letter capitalized
   */
  public static String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  /**
   * Apply heuristics to select a {@link FieldTransformer} instance for the provided {@link
   * ColumnBean} and {@link TransformationBean}.
   *
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @return an {@link Optional} containing the {@link FieldTransformer} if one is appropriate
   */
  public static Optional<FieldTransformer> selectTransformerForField(
      ColumnBean column, TransformationBean transformation) {
    if (transformation.hasTransformer()) {
      return getFieldTransformer(transformation.getTransformer());
    }

    Optional<FieldTransformer> answer =
        Optional.ofNullable(transformersByFrom.get(transformation.getFrom()));
    if (answer.isEmpty()) {
      if (NoCodeFromNamesRegex.matcher(transformation.getFrom()).matches()) {
        answer = Optional.of(NoCodeInstance);
      } else if (column.isEnum()) {
        answer = Optional.empty();
      } else if (column.isChar()) {
        answer = Optional.of(CharInstance);
      } else if (column.isCharacter()) {
        answer = Optional.of(CharInstance);
      } else if (column.isString()) {
        answer = Optional.of(StringInstance);
      } else if (column.isInt()) {
        answer = Optional.of(IntInstance);
      } else if (column.isLong()) {
        answer = Optional.of(LongInstance);
      } else if (column.isNumeric()) {
        answer = Optional.of(AmountInstance);
      } else if (column.isDate()) {
        answer = Optional.of(DateInstance);
      }
    }

    return answer;
  }

  /**
   * Apply heuristics to select a {@link FhirElementTransformer} instance for the provided
   * FhirTransformationDto.
   *
   * @param fhirTransformationDto model object describing the fhir element
   * @return an {@link Optional} containing the {@link FhirElementTransformer} if one is appropriate
   */
  public static Optional<FhirElementTransformer> selectFhirElementTransformer(
      GenerateDataDictionaryFromDslMojo.FhirTransformationDto fhirTransformationDto) {
    String transformer = fhirTransformationDto.getFhirElementTransformer();
    return Optional.ofNullable(fhirElementTransformersByName.get(transformer));
  }

  /**
   * Looks for a {@link FieldTransformer} with the given name.
   *
   * @param transformerName name of the {@link FieldTransformer}
   * @return {@link Optional} containing the transformer, empty otherwise
   */
  @Nonnull
  public static Optional<FieldTransformer> getFieldTransformer(String transformerName) {
    return Optional.ofNullable(transformersByName.get(transformerName));
  }

  /**
   * Helper method to parse the {@code from} of the specified Transformation and call one of two
   * lambdas depending on whether the field is a simple field or a nested field (i.e. one with a
   * field and a property of that field). Only a single period is supported in the field name. This
   * is a "hole in the middle" pattern to centralize the field name parsing in one place. The names
   * passed to the methods are already capitalized so they are ready to have has or get prepended to
   * them as needed.
   *
   * @param transformation defines the {@code from} field
   * @param simpleProperty accepts capitalized field name and returns a CodeBlock
   * @param nestedProperty accepts capitalized field and property names and returns a CodeBlock
   * @return CodeBlock created by appropriate method
   */
  public static CodeBlock createPropertyAccessCodeBlock(
      TransformationBean transformation,
      Function<String, CodeBlock> simpleProperty,
      BiFunction<String, String, CodeBlock> nestedProperty) {
    final String from = capitalize(transformation.getFrom());
    final int dotIndex = from.indexOf('.');
    if (dotIndex < 0) {
      return simpleProperty.apply(from);
    } else {
      final String fieldName = from.substring(0, dotIndex);
      final String propertyName = capitalize(from.substring(dotIndex + 1));
      return nestedProperty.apply(fieldName, propertyName);
    }
  }

  /**
   * Methods of the {@link gov.cms.model.dsl.codegen.library.DataTransformer} class require a field
   * name to be provided for use when reporting errors. These field names are composed of a prefix
   * string (usually empty but for fields in array objects the prefix contains the name of the array
   * and the index of the object being transformed) plus the name of the field in the entity class.
   * The prefix string is stored in a variable named {@link FieldTransformer#NAME_PREFIX_VAR} and
   * the field name is accessed using the {@code Fields} static variable created by lombok in the
   * entity class.
   *
   * <p>This method produces code to add the field name to the prefix string to produce a field name
   * that will be passed to a method in the {@link
   * gov.cms.model.dsl.codegen.library.DataTransformer} class.
   *
   * @param mapping The mapping that contains the field.
   * @param column model object describing the database column
   * @return an expression that produces the complete field name
   */
  public static CodeBlock createFieldNameForErrorReporting(MappingBean mapping, ColumnBean column) {
    return CodeBlock.of(
        "$L + $T.Fields.$L",
        FieldTransformer.NAME_PREFIX_VAR,
        toClassName(mapping.getEntityClassName()),
        column.getName());
  }

  /**
   * Split the full class name into package and simple name and create a {@link ClassName} reference
   * for the class.
   *
   * @param fullClassName full name including package
   * @return correct {@link ClassName}
   * @throws IllegalArgumentException if the name does not include a package part
   */
  public static ClassName toClassName(String fullClassName) {
    final int lastComponentDotIndex = fullClassName.lastIndexOf('.');
    if (lastComponentDotIndex <= 0) {
      throw new IllegalArgumentException("expected a full class name but there was no .");
    }
    return ClassName.get(
        fullClassName.substring(0, lastComponentDotIndex),
        fullClassName.substring(lastComponentDotIndex + 1));
  }
}
