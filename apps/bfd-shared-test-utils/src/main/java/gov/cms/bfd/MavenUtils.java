package gov.cms.bfd;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.testcontainers.shaded.com.google.common.annotations.VisibleForTesting;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Extract maven project meta data for use in tests. */
public class MavenUtils {
  /** Name of maven pom file to search for. */
  public static final String POM_FILE_NAME = "pom.xml";

  /**
   * Searches for a {@code pom.xml} file starting from the current working directory and then
   * proceeding through parent directories until root is reached. Looks for parent version string
   * within the pom file and returns it if one is found that is not empty.
   *
   * @return project version string from pom file
   * @throws RuntimeException if any error happens or no suitable pom file could be found
   */
  public static String findProjectVersion() {
    return findProjectVersion(POM_FILE_NAME);
  }

  /**
   * Searches for a file with the given name starting from the current working directory and then
   * proceeding through parent directories until root is reached. Looks for parent version string
   * within the pom file and returns it if one is found that is not empty.
   *
   * @param pomFileName name of file to search for
   * @return project version string from pom file
   * @throws RuntimeException if any error happens or no suitable pom file could be found
   */
  @VisibleForTesting
  static String findProjectVersion(String pomFileName) {
    File previousDirectory;
    File directory = new File(".").getAbsoluteFile();
    do {
      File pomFile = new File(directory, pomFileName);
      if (pomFile.exists()) {
        try {
          DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
          // It is advisable to restrict the resolution of external entities by disabling
          // DOCTYPE declarations entirely when they are not essential.
          factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
          DocumentBuilder builder = factory.newDocumentBuilder();
          Document document = builder.parse(new File(pomFileName));
          XPath xPath = XPathFactory.newInstance().newXPath();
          NodeList nodes =
              (NodeList)
                  xPath.evaluate("/project/parent/version", document, XPathConstants.NODESET);
          for (int i = 0; i < nodes.getLength(); ++i) {
            Element e = (Element) nodes.item(i);
            String version = e.getTextContent();
            if (!Strings.isNullOrEmpty(version)) {
              return version;
            }
          }
        } catch (Exception ex) {
          throw new RuntimeException("encountered error while parsing pom.xml file", ex);
        }
      }
      previousDirectory = directory;
      directory = previousDirectory.getParentFile();
    } while (directory != null && !directory.equals(previousDirectory));
    throw new RuntimeException("failed to find a pom.xml file containing a version!");
  }
}
