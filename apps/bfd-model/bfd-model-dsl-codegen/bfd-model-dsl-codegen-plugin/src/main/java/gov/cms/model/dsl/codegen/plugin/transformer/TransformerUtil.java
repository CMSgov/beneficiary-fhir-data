package gov.cms.model.dsl.codegen.plugin.transformer;

import com.google.common.collect.ImmutableMap;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class TransformerUtil {
  public static final String TimestampFromName = "NOW";
  private static final String NoMappingFromName = "NONE";
  public static final String ParentFromName = "PARENT";
  public static final String IndexFromName = "INDEX";
  private static final Pattern NoMappingFromNamesRegex = Pattern.compile("NONE|PARENT|INDEX");
  private static final CharFieldTransformer CharInstance = new CharFieldTransformer();
  private static final IntFieldTransformer IntInstance = new IntFieldTransformer();
  private static final DateFieldTransformer DateInstance = new DateFieldTransformer();
  private static final AmountFieldTransformer AmountInstance = new AmountFieldTransformer();
  private static final StringFieldTransformer StringInstance = new StringFieldTransformer();
  private static final IdHashFieldTransformer IdHashInstance = new IdHashFieldTransformer();
  private static final RifTimestampFieldTransformer RifTimestampInstance =
      new RifTimestampFieldTransformer();
  private static final IntStringFieldTransformer IntStringInstance =
      new IntStringFieldTransformer();
  private static final LongStringFieldTransformer LongStringInstance =
      new LongStringFieldTransformer();
  private static final MessageEnumFieldTransformer MessageEnumInstance =
      new MessageEnumFieldTransformer();
  private static final TimestampFieldTransformer TimestampInstance =
      new TimestampFieldTransformer();
  public static final String ID_HASH_TRANSFORM_NAME = "IdHash";
  public static final String ENUM_VALUE_TRANSFORM_NAME = "EnumValueIfPresent";
  public static final String MESSAGE_ENUM_TRANSFORM_NAME = "MessageEnum";
  private static final Map<String, AbstractFieldTransformer> transformersByName =
      ImmutableMap.of(
          ID_HASH_TRANSFORM_NAME,
          IdHashInstance,
          "Now",
          TimestampInstance,
          MESSAGE_ENUM_TRANSFORM_NAME,
          MessageEnumInstance,
          ENUM_VALUE_TRANSFORM_NAME,
          new EnumValueIfPresentTransformer(),
          "RifTimestamp",
          RifTimestampInstance,
          "IntString",
          IntStringInstance,
          "LongString",
          LongStringInstance);
  private static final Map<String, AbstractFieldTransformer> transformersByFrom =
      ImmutableMap.of(TimestampFromName, TimestampInstance);

  public static boolean mappingRequiresIdHasher(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .anyMatch(transform -> ID_HASH_TRANSFORM_NAME.equals(transform.getTransformer()));
  }

  public static boolean mappingRequiresEnumExtractor(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .anyMatch(transform -> MESSAGE_ENUM_TRANSFORM_NAME.equals(transform.getTransformer()));
  }

  public static String capitalize(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  public static Optional<AbstractFieldTransformer> selectTransformerForField(
      ColumnBean column, TransformationBean transformation) {
    if (transformation.hasTransformer()) {
      return Optional.ofNullable(transformersByName.get(transformation.getTransformer()));
    }

    Optional<AbstractFieldTransformer> answer =
        Optional.ofNullable(transformersByFrom.get(transformation.getFrom()));
    if (!(answer.isPresent()
        || NoMappingFromNamesRegex.matcher(transformation.getFrom()).matches())) {
      if (column.isEnum()) {
        // TODO add support for message enums
        answer = Optional.empty();
      } else if (column.isChar()) {
        answer = Optional.of(CharInstance);
      } else if (column.isCharacter()) {
        answer = Optional.of(CharInstance);
      } else if (column.isString()) {
        answer = Optional.of(StringInstance);
      } else if (column.isInt()) {
        answer = Optional.of(IntInstance);
      } else if (column.isDecimal()) {
        answer = Optional.of(AmountInstance);
      } else if (column.isDate()) {
        answer = Optional.of(DateInstance);
      }
    }

    return answer;
  }
}
