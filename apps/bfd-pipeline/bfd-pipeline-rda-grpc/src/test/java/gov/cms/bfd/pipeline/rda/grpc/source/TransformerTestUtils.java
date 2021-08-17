package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class TransformerTestUtils {
  /**
   * The McsClaimTransformerTest and FissClaimTransformerTest need to be able to compare lists of
   * objects and there isn't a standard matcher that is compatible and handles ordering properly.
   */
  public static <T> void assertListContentsHaveSamePropertyValues(
      Set<T> expectedSet, Set<T> actualSet, ToIntFunction<T> priority) {
    List<T> expected =
        expectedSet.stream().sorted(Comparator.comparingInt(priority)).collect(Collectors.toList());
    List<T> actual =
        actualSet.stream().sorted(Comparator.comparingInt(priority)).collect(Collectors.toList());
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); ++i) {
      assertThat(actual.get(i), samePropertyValuesAs(expected.get(i)));
    }
  }
}
