package gov.cms.bfd.pipeline.bridge.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.InpatientClaimColumn;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ModelUtil} class. */
public class ModelUtilTest {
  /**
   * Extracts the names of a numbered enum and verifies that the names all fit the same base name
   * and occur in the list in the correct numbered order.
   */
  @Test
  public void testThatNumberedSequenceNamesAreExtractedInOrder() {
    final List<String> names =
        ModelUtil.listNumberedEnumNames(
            InpatientClaimColumn.values(), InpatientClaimColumn.ICD_DGNS_CD1);
    assertNamesFollowSequence(names, InpatientClaimColumn.ICD_DGNS_CD1);
  }

  /**
   * Lists of names produced by {@link ModelUtil#listNumberedEnumNames} are expected to all have the
   * exact same name followed by an integer from 1 to the number of items in this list. This helper
   * method verifies that this pattern holds for the provided list of names and first enum name.
   *
   * @param names names produced by {@link ModelUtil#listNumberedEnumNames}
   * @param firstEnumInSequence first enum value whose name appears in the list
   */
  static void assertNamesFollowSequence(List<String> names, Enum<?> firstEnumInSequence) {
    final String firstNameInSequence = firstEnumInSequence.name();
    final String commonPrefix = firstNameInSequence.substring(0, firstNameInSequence.length() - 1);
    assertEquals(firstNameInSequence, commonPrefix + "1");
    int sequenceNumber = 1;
    for (String name : names) {
      assertEquals(name, commonPrefix + sequenceNumber);
      sequenceNumber += 1;
    }
  }
}
