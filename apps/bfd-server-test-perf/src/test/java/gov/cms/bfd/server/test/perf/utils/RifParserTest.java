package gov.cms.bfd.server.test.perf.utils;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import java.net.URISyntaxException;
import org.junit.*;

/** Unit test for simple RifParser. */
public class RifParserTest {
  /** Test RIF file parsing */
  @Test
  public void testRifFileStatic() {
    RifParser parser = new RifParser(StaticRifResource.SAMPLE_A_BENES.toRifFile());
    RifRecordEvent<?> rifRecordEvent = parser.next();
    if (rifRecordEvent != null) {
      Beneficiary beneRow = (Beneficiary) rifRecordEvent.getRecord();
      Assert.assertEquals("-88888888888888", beneRow.getBeneficiaryId());
    }
  }

  @Test
  public void testRifFileLocal() {
    try {
      RifParser parser = new RifParser("beneficiary_truncated.rif", RifFileType.BENEFICIARY);
      RifRecordEvent<?> rifRecordEvent = parser.next();
      if (rifRecordEvent != null) {
        Beneficiary beneRow = (Beneficiary) rifRecordEvent.getRecord();
        Assert.assertEquals("1", beneRow.getBeneficiaryId());
      }
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }
}
