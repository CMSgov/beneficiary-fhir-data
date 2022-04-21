package gov.cms.bfd.pipeline.bridge.model;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Holds common utility methods for model classes. */
class ModelUtil {
  /**
   * We need a list of the names of all enum values that end with a number and have the same base
   * name as the provided example. Names fit the pattern ICD_PRCDR_CD1, ICD_PRCDR_CD2, etc.
   *
   * @param enumValues expected to be the result of a call to {@link Enum#values}
   * @param baseValue a template enum value to extract the base name from
   * @param <E> a RIF column enum class containing our enum values
   * @return unmodifiable {@link List} of names of all numbered enums with same base name
   */
  static <E extends Enum<?>> List<String> listNumberedEnumNames(E[] enumValues, E baseValue) {
    final String baseName = baseValue.name().replaceAll("\\d+$", "");
    return Stream.of(enumValues)
        .map(Enum::name)
        .filter(name -> name.startsWith(baseName))
        .collect(Collectors.toUnmodifiableList());
  }
}
