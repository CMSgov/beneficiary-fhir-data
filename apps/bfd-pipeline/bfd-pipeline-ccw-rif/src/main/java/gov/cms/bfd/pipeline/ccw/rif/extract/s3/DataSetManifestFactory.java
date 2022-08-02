package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import java.io.ByteArrayInputStream;
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
  /** The {@link String} pipeline manifest XML Schema Definition (XSD). */
  public static final String MANIFEST_XSD =
      "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
          + "<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"qualified\""
          + "	targetNamespace=\"http://cms.hhs.gov/bluebutton/api/schema/ccw-rif/v9\""
          + "	xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
          + "  <xs:element name=\"dataSetManifest\">"
          + "    <xs:complexType>"
          + "      <xs:sequence>"
          + "        <xs:element maxOccurs=\"unbounded\" name=\"entry\">"
          + "          <xs:complexType>"
          + "            <xs:attribute name=\"name\" type=\"xs:string\" use=\"required\" />"
          + "            <xs:attribute name=\"type\" type=\"xs:string\" use=\"required\" />"
          + "          </xs:complexType>"
          + "        </xs:element>"
          + "      </xs:sequence>"
          + "      <xs:attribute name=\"timestamp\" type=\"xs:dateTime\" use=\"required\" />"
          + "      <xs:attribute name=\"sequenceId\" type=\"xs:unsignedByte\" use=\"required\" />"
          + "      <xs:attribute name=\"syntheticData\" type=\"xs:boolean\" use=\"optional\" />"
          + "    </xs:complexType>"
          + "  </xs:element>"
          + "</xs:schema>";

  /** The {@link DataSetManifestFactory} our factory instance (singleton). */
  private static DataSetManifestFactory thisInstance;

  /** private constructor...only we get to use it {@link DataSetManifestFactory}. */
  private DataSetManifestFactory() {}

  /**
   * Constructs a new {@link DataSetManifestFactory} instance.
   *
   * @return the {@link DataSetManifestFactory}
   */
  public static DataSetManifestFactory newInstance() {
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
   */
  public DataSetManifest parseManifest(String manifestFilename) throws JAXBException {
    try {
      InputStream manifestStream =
          Thread.currentThread().getContextClassLoader().getResourceAsStream(manifestFilename);
      return parseManifest(manifestStream);
    } catch (Exception e) {
      throw new JAXBException(e.getMessage(), e);
    }
  }

  /**
   * Parses the input byte stream {@link InputStream} and returns a new {@link DataSetManifest}
   * instance. It validates the XML data stream {@link InputStream} to be both well-formed and
   * valid.
   *
   * @param manifestStream the {@link InputStream} stream of XML to read/parse
   * @return the {@link DataSetManifest}
   * @throws JAXBException (any errors encountered will be bubbled up).
   * @return the {@link DataSetManifest}
   */
  public DataSetManifest parseManifest(InputStream manifestStream) throws JAXBException {
    DataSetManifest manifest = null;
    try {
      InputStream xsdStream = new ByteArrayInputStream(MANIFEST_XSD.getBytes());
      SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Schema schema = sf.newSchema(new StreamSource(xsdStream));
      JAXBContext jaxbContext = JAXBContext.newInstance(DataSetManifest.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      jaxbUnmarshaller.setSchema(schema);
      manifest = (DataSetManifest) jaxbUnmarshaller.unmarshal(manifestStream);
    } catch (SAXException e) {
      throw new JAXBException(e.getMessage(), e);
    } catch (Exception e) {
      throw new JAXBException(e.getMessage(), e);
    }
    return manifest;
  }
}
