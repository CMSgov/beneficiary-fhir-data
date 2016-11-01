package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

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

import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest.DataSetManifestEntry;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Unit tests for {@link DataSetManifest}.
 */
public final class DataSetManifestTest {
	/**
	 * Verifies that {@link DataSetManifest} can be unmarshalled, as expected.
	 * 
	 * @throws JAXBException
	 *             (indicates test failure)
	 */
	@Test
	public void jaxbUnmarshalling() throws JAXBException {
		InputStream manifestStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("manifest-sample-a.xml");

		JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

		DataSetManifest manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);

		Assert.assertNotNull(manifest);
		Assert.assertEquals(1994,
				LocalDateTime.ofInstant(manifest.getTimestamp(), ZoneId.systemDefault()).get(ChronoField.YEAR));
		Assert.assertEquals(2, manifest.getEntries().size());
		Assert.assertEquals("sample-a-beneficiaries.txt", manifest.getEntries().get(0).getName());
		Assert.assertEquals(RifFileType.BENEFICIARY, manifest.getEntries().get(0).getType());
	}

	/**
	 * Just a simple little app that will use JAXB to marshall a sample
	 * {@link DataSetManifest} to XML. This was used as the basis for the test
	 * resources used in these tests.
	 * 
	 * @param args
	 *            (not used)
	 * @throws JAXBException
	 *             (programmer error)
	 */
	public static void main(String[] args) throws JAXBException {
		DataSetManifest manifest = new DataSetManifest(Instant.now(),
				new DataSetManifestEntry("foo.xml", RifFileType.BENEFICIARY),
				new DataSetManifestEntry("bar.xml", RifFileType.PDE));

		JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		jaxbMarshaller.marshal(manifest, System.out);
	}
}
