package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifPipelineJob;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest.DataSetManifestId;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link DataSetManifest}. */
public final class DataSetManifestTest {
  /**
   * Verifies that {@link DataSetManifest} can be unmarshalled, as expected.
   *
   * @throws JAXBException (indicates test failure)
   */
  @Test
  public void jaxbUnmarshallingForSampleA() throws JAXBException {
    InputStream manifestStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest-sample-a.xml");

    JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    DataSetManifest manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);

    Assert.assertNotNull(manifest);
    Assert.assertEquals(
        1994,
        LocalDateTime.ofInstant(manifest.getTimestamp(), ZoneId.systemDefault())
            .get(ChronoField.YEAR));
    Assert.assertEquals(1, manifest.getSequenceId());
    Assert.assertEquals(2, manifest.getEntries().size());
    Assert.assertEquals("sample-a-beneficiaries.txt", manifest.getEntries().get(0).getName());
    Assert.assertEquals(RifFileType.BENEFICIARY, manifest.getEntries().get(0).getType());
  }

  /**
   * Verifies that {@link DataSetManifest} can be unmarshalled, as expected. The sample XML document
   * used here was produced by Scott Koerselman on 2016-12-19.
   *
   * @throws JAXBException (indicates test failure)
   */
  @Test
  public void jaxbUnmarshallingForSampleB() throws JAXBException {
    InputStream manifestStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest-sample-b.xml");

    JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    DataSetManifest manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);

    Assert.assertNotNull(manifest);
    Assert.assertNotNull(manifest.getTimestamp());
    Assert.assertEquals(
        2016,
        LocalDateTime.ofInstant(manifest.getTimestamp(), ZoneId.systemDefault())
            .get(ChronoField.YEAR));
    Assert.assertEquals(1, manifest.getSequenceId());
    Assert.assertEquals(9, manifest.getEntries().size());
    Assert.assertEquals("bene.txt", manifest.getEntries().get(0).getName());
    for (int i = 0; i < manifest.getEntries().size(); i++) {
      DataSetManifestEntry entry = manifest.getEntries().get(i);
      Assert.assertNotNull("Null entry: " + i, entry);
      Assert.assertNotNull("Null entry name: " + i, entry.getName());
      Assert.assertNotNull("Null entry type: " + i, entry.getType());
    }
    Assert.assertEquals(RifFileType.BENEFICIARY, manifest.getEntries().get(0).getType());
  }

  /**
   * Verifies that {@link DataSetManifest} can be unmarshalled, as expected, even when the <code>
   * timestamp</code> attribute has unexpected leading whitespace. This is a regression test case
   * for <a href="http://issues.hhsdevcloud.us/browse/CBBD-207">CBBD-207: Invalid manifest timestamp
   * causes ETL service to fail</a>.
   *
   * @throws JAXBException (indicates test failure)
   */
  @Test
  public void jaxbUnmarshallingForTimestampsWithLeadingWhitespace() throws JAXBException {
    InputStream manifestStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream("manifest-sample-c.xml");

    JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

    DataSetManifest manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);

    Assert.assertNotNull(manifest);
    Assert.assertNotNull(manifest.getTimestamp());
  }

  /**
   * Verifies that {@link DataSetManifestId}s can be round-tripped, as expected. A regression test
   * case for <a href="http://issues.hhsdevcloud.us/browse/CBBD-298">CBBD-298: Error reading some
   * data set manifests in S3: "AmazonS3Exception: The specified key does not exist"</a>.
   */
  @Test
  public void manifestIdRoundtrip() {
    String s3Key =
        CcwRifPipelineJob.S3_PREFIX_PENDING_DATA_SETS + "/2017-07-11T00:00:00.000Z/1_manifest.xml";
    DataSetManifestId manifestId = DataSetManifestId.parseManifestIdFromS3Key(s3Key);

    Assert.assertEquals(
        s3Key, manifestId.computeS3Key(CcwRifPipelineJob.S3_PREFIX_PENDING_DATA_SETS));
  }

  /**
   * Just a simple little app that will use JAXB to marshall a sample {@link DataSetManifest} to
   * XML. This was used as the basis for the test resources used in these tests.
   *
   * @param args (not used)
   * @throws JAXBException (programmer error)
   */
  public static void main(String[] args) throws JAXBException {
    DataSetManifest manifest =
        new DataSetManifest(
            Instant.now(),
            0,
            new DataSetManifestEntry("foo.xml", RifFileType.BENEFICIARY),
            new DataSetManifestEntry("bar.xml", RifFileType.PDE));

    JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

    jaxbMarshaller.marshal(manifest, System.out);
  }
}
