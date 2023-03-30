package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import org.hamcrest.beans.SamePropertyValuesAs;

/** Utility class for assertions related to transformations. */
public class TransformerTestUtils {
  /**
   * The McsClaimTransformerTest and FissClaimTransformerTest need to be able to compare lists of
   * objects and there isn't a standard matcher that is compatible and handles ordering properly.
   *
   * @param <T> the function param type
   * @param expectedSet the expected set
   * @param actualSet the actual set
   * @param priority the priority for ordering
   */
  public static <T> void assertListContentsHaveSamePropertyValues(
      Set<T> expectedSet, Set<T> actualSet, ToIntFunction<T> priority) {
    assertContentsHaveSamePropertyValues(expectedSet, actualSet, Comparator.comparingInt(priority));
  }

  /**
   * Compare two collections of objects using reflection and report any mismatched properties. Both
   * collections must have the same size and be sortable using the comparator. The comparator only
   * needs to compare enough fields to uniquely identify an object for purposes of comparison (e.g.
   * using primary key for a database entity).
   *
   * @param expected expected values
   * @param actual values to be compared to expected values
   * @param comparatorForSort used to sort values prior to comparison
   * @param <T> type of beans being compared
   */
  public static <T> void assertContentsHaveSamePropertyValues(
      Collection<T> expected, Collection<T> actual, Comparator<T> comparatorForSort) {
    assertEquals(expected.size(), actual.size());
    final var expectedList =
        expected.stream().sorted(comparatorForSort).collect(Collectors.toList());
    final var actualList = actual.stream().sorted(comparatorForSort).collect(Collectors.toList());
    for (int i = 0; i < expectedList.size(); ++i) {
      assertObjectsHaveSamePropertyValues(expectedList.get(i), actualList.get(i));
    }
  }

  /**
   * Compare two objects using reflection.
   *
   * @param expected expected value
   * @param actual value to be compared to expected value
   * @param <T> type of beans being compared
   */
  public static <T> void assertObjectsHaveSamePropertyValues(T expected, T actual) {
    assertThat(actual, SamePropertyValuesAs.samePropertyValuesAs(expected));
  }
}
