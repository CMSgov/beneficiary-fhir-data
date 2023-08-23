package gov.cms.bfd.pipeline.bridge.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Mcs} class. */
public class McsTest {
  /** Verifies that all lists of numbered enum names appear in the proper sequence. */
  @Test
  public void testThatNumberedSequenceNamesAreInSequenceOrder() {
    ModelUtilTest.assertNamesFollowSequence(Mcs.ICD_DGNS_CD, CarrierClaimColumn.ICD_DGNS_CD1);
    ModelUtilTest.assertNamesFollowSequence(
        Mcs.ICD_DGNS_VRSN_CD, CarrierClaimColumn.ICD_DGNS_VRSN_CD1);
  }

  /**
   * Pairs of lists are iterated over in the same loop. This test ensures those lists have the same
   * length to ensure we don't skip any values.
   */
  @Test
  public void testThatListsOfNamesHaveSameLengths() {
    assertEquals(Mcs.ICD_DGNS_CD.size(), Mcs.ICD_DGNS_VRSN_CD.size());
  }
}
