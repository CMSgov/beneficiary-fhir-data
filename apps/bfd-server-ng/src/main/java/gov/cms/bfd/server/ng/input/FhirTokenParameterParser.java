package gov.cms.bfd.server.ng.input;

import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/** Generic parser for token parameters using AND/OR semantics. */
public class FhirTokenParameterParser {

  private FhirTokenParameterParser() {}

  /**
   * Parses the query parameter into a nested list of criteria.
   *
   * <p>Outer list is AND conditions, inner list is OR conditions.
   *
   * @param <T> the type of the criteria objects being parsed
   * @param param param from request
   * @param tokenMapper a function to map parsed token values into a list of T objects
   * @return list of parsed tokens
   */
  public static <T> List<List<T>> parse(
      @Nullable TokenAndListParam param, Function<TokenParam, List<T>> tokenMapper) {
    if (param == null || param.getValuesAsQueryTokens().isEmpty()) {
      return Collections.emptyList();
    }

    return param.getValuesAsQueryTokens().stream()
        .map(orList -> parseOrList(orList, tokenMapper))
        .filter(list -> !list.isEmpty())
        .toList();
  }

  private static <T> List<T> parseOrList(
      TokenOrListParam orList, Function<TokenParam, List<T>> tokenMapper) {
    if (orList == null || orList.getValuesAsQueryTokens().isEmpty()) {
      return Collections.emptyList();
    }

    return orList.getValuesAsQueryTokens().stream()
        .flatMap(token -> tokenMapper.apply(token).stream())
        .toList();
  }

  /**
   * Flattens a TokenAndListParam into a single stream of TokenParam objects.
   *
   * @param param from request
   * @return stream of TokenParams
   */
  public static Stream<TokenParam> flatten(TokenAndListParam param) {
    if (param == null || param.getValuesAsQueryTokens().isEmpty()) {
      return Stream.empty();
    }
    return param.getValuesAsQueryTokens().stream()
        .flatMap(orList -> orList.getValuesAsQueryTokens().stream());
  }
}
