package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.pipeline.ccw.rif.extract.s3.DataSetManifest;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;

/**
 * This singleton class defines the XML parsing functionality specific to a BFD file manifest; it is
 * used to assert XML <code>manifest.xml</code> files are both well-formed and have valid content.
 * The embedded XML Schema Definition (XSD) will be specified per JAXB schema validation to ensure
 * data is valid.
 */
public class DataSetManifestFactory {
  /** The {@link String} pipeline manifest XML Schema Definition (XSD) filename. */
  private static final String MANIFEST_XSD_FILENAME = "bfd-manifest.xsd";

  /** The {@link Schema} XML schema validator data stream. */
  private static Schema schema = null;

  /** The {@link DataSetManifestFactory} our factory instance (singleton). */
  private static DataSetManifestFactory thisInstance;

  /** private constructor...only we get to use it {@link DataSetManifestFactory}. */
  private DataSetManifestFactory() throws SAXException {
    InputStream xsdStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(MANIFEST_XSD_FILENAME);
    SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    schema = sf.newSchema(new StreamSource(xsdStream));
  }

  /**
   * Constructs a new {@link DataSetManifestFactory} instance.
   *
   * @return the {@link DataSetManifestFactory}
   * @throws SAXException (any errors encountered will be bubbled up).
   */
  public static DataSetManifestFactory newInstance() throws SAXException {
    if (thisInstance == null) {
      thisInstance = new DataSetManifestFactory();
    }
    return thisInstance;
  }

  /**
   * Parses the input filename {@link String} and returns a new {@link DataSetManifest} instance.
   *
   * @param manifestFilename the {@link String} XML filename to read/parse
   * @return the {@link DataSetManifest}
   * @throws JAXBException (any errors encountered will be bubbled up).
   * @throws SAXException (any errors encountered will be bubbled up).
   */
  public DataSetManifest parseManifest(String manifestFilename) throws JAXBException, SAXException {
    InputStream manifestStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(manifestFilename);
    return parseManifest(manifestStream);
  }

  /**
   * Parses the input byte stream {@link InputStream} and returns a new {@link DataSetManifest}
   * instance. It validates the XML data stream {@link InputStream} to be both well-formed and
   * valid.
   *
   * @param manifestStream the {@link InputStream} stream of XML to read/parse
   * @throws JAXBException (any errors encountered will be bubbled up).
   * @throws SAXException (any errors encountered will be bubbled up).
   * @return the {@link DataSetManifest}
   */
  public DataSetManifest parseManifest(InputStream manifestStream)
      throws JAXBException, SAXException {
    JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
    Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
    jaxbUnmarshaller.setSchema(schema);
    return (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);
  }
}
