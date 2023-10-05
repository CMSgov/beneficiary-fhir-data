package gov.cms.bfd.pipeline.bridge.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.entities.InpatientClaimColumn;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Fiss} class. */
public class FissTest {
  /** Verifies that all lists of numbered enum names appear in the proper sequence. */
  @Test
  public void testThatNumberedSequenceNamesAreInSequenceOrder() {
    ModelUtilTest.assertNamesFollowSequence(
        Fiss.CLM_POA_IND_SW, InpatientClaimColumn.CLM_POA_IND_SW1);
    ModelUtilTest.assertNamesFollowSequence(Fiss.ICD_DGNS_CD, InpatientClaimColumn.ICD_DGNS_CD1);
    ModelUtilTest.assertNamesFollowSequence(Fiss.ICD_PRCDR_CD, InpatientClaimColumn.ICD_PRCDR_CD1);
    ModelUtilTest.assertNamesFollowSequence(Fiss.PRCDR_DT, InpatientClaimColumn.PRCDR_DT1);
  }

  /**
   * Pairs of lists are iterated over in the same loop. This test ensures those lists have the same
   * length to ensure we don't skip any values.
   */
  @Test
  public void testThatListsOfNamesHaveSameLengths() {
    assertEquals(Fiss.ICD_DGNS_CD.size(), Fiss.CLM_POA_IND_SW.size());
    assertEquals(Fiss.ICD_PRCDR_CD.size(), Fiss.PRCDR_DT.size());
  }
}
