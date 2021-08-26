package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.Assert.assertEquals;

import java.util.List;

public class TransformerTestUtils {
  /**
   * The McsClaimTransformerTest and FissClaimTransformerTest need to be able to compare lists of
   * objects and there isn't a standard matcher that is compatible and handles ordering properly.
   */
  public static <T> void assertListContentsHaveSamePropertyValues(
      List<T> expected, List<T> actual) {
    assertEquals(expected.size(), actual.size());
    for (int i = 0; i < expected.size(); ++i) {
      assertThat(actual.get(i), samePropertyValuesAs(expected.get(i)));
    }
  }
}
