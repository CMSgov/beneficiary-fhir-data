package gov.cms.bfd.model.codebook.unmarshall;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.SupportedCodebook;
import gov.cms.bfd.model.codebook.model.Variable;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Can unmarshall {@link Codebook} XML and find {@link Variable}s. */
public final class CodebookVariableReader {
  /** A function that produces a {@link ByteSource} for every {@link SupportedCodebook}. */
  private final Function<SupportedCodebook, ByteSource> byteSourceFactory;

  /**
   * Initializes an instance that reads XML for each {@link SupportedCodebook} from a classpath
   * resource.
   */
  public CodebookVariableReader() {
    this(CodebookVariableReader::getCodebookXMLFromResource);
  }

  /**
   * Initializes an instance that reads XML for each {@link SupportedCodebook} from a file in the
   * given directory.
   *
   * @param xmlDirectory directory containing the XML files
   */
  public CodebookVariableReader(File xmlDirectory) {
    this(codebook -> getCodebookXMLFromFile(xmlDirectory, codebook));
  }

  /**
   * Initializes an instance.
   *
   * @param byteSourceFactory function that produces a {@link ByteSource} for every {@link
   *     SupportedCodebook}
   */
  public CodebookVariableReader(Function<SupportedCodebook, ByteSource> byteSourceFactory) {
    this.byteSourceFactory = byteSourceFactory;
  }

  /**
   * Builds a {@link Map} of the known {@link Codebook} {@link Variable}s, keyed by Variable.getId()
   * (with duplicates removed safely). Reads all values from XML already defined as a resource.
   *
   * @return the de-duped map of {@link Variable}s
   */
  public Map<String, Variable> buildVariablesMappedById() {
    Map<String, List<Variable>> variablesMultimapById = buildVariablesMultimappedById();
    Map<String, Variable> variablesMappedById = new LinkedHashMap<>(variablesMultimapById.size());
    final Set<String> allowedMultipleDefinitionIds = Set.of("BENE_ID", "DOB_DT", "GNDR_CD");
    for (String id : variablesMultimapById.keySet()) {
      List<Variable> variablesForId = variablesMultimapById.get(id);
      if (variablesForId.size() == 1) {
        Variable variable = variablesForId.get(0);
        variablesMappedById.put(variable.getId(), variable);
      } else if (allowedMultipleDefinitionIds.contains(id)) {
        Variable variable = variablesForId.get(0);
        variablesMappedById.put(variable.getId(), variable);
        /*
         * FIXME The code books for part a/b/d and a/b/c/d have
         * overlapping fields between them. They also have fields that
         * aren't in the other code book so we need to include both code
         * books ..codebook-mbsf-abd.pdf and codebook-mbsf-abcd.pdf.
         * Thus the reason to allow duplicate fields below.
         */
      } else if (variablesForId.size() == 2) {
        Variable variable = variablesForId.get(0);
        variablesMappedById.put(variable.getId(), variable);
      } else
        throw new IllegalStateException(
            String.format("%s with duplicates found: %s", id, variablesForId));
    }
    return variablesMappedById;
  }

  /**
   * Looks up XML file for the provided codebook and returns a {@link ByteSource} for that file.
   *
   * @param xmlDirectory the directory containing the xml files
   * @param codebook the codebook to find
   * @return {@link ByteSource} for the XML file
   */
  private static ByteSource getCodebookXMLFromFile(File xmlDirectory, SupportedCodebook codebook) {
    final File xmlFile = new File(xmlDirectory, codebook.getCodebookXmlResourceName());
    if (!xmlFile.exists()) {
      throw new IllegalStateException(
          String.format(
              "Unable to locate XML file: '%s' for codebook %s.",
              xmlFile.getAbsolutePath(), codebook));
    }
    return Files.asByteSource(xmlFile);
  }

  /**
   * Looks up the resource {@link URL} for the provided codebook's XML resource and returns a {@link
   * ByteSource} for that URL.
   *
   * @param codebook the codebook to find
   * @return {@link ByteSource} for the XML resource
   */
  private static ByteSource getCodebookXMLFromResource(SupportedCodebook codebook) {
    final ClassLoader contextClassLoader = Codebook.class.getClassLoader();
    final URL codebookUrl = contextClassLoader.getResource(codebook.getCodebookXmlResourceName());
    if (codebookUrl == null) {
      throw new IllegalStateException(
          String.format(
              "Unable to locate classpath resource: '%s'."
                  + " JVM Classpath: '%s'. Classloader: '%s'. Classloader URLs: '%s'.",
              codebook.getCodebookXmlResourceName(),
              System.getProperty("java.class.path"),
              contextClassLoader,
              contextClassLoader instanceof URLClassLoader
                  ? Arrays.toString(((URLClassLoader) contextClassLoader).getURLs())
                  : "N/A"));
    }
    return Resources.asByteSource(codebookUrl);
  }

  /**
   * Builds A multimap of the known {@link Variable}s.
   *
   * <p>A multimap is used because some {@link Variable}s appear in more than one {@link Codebook}.
   *
   * @return A multimap of the known {@link Variable}s
   */
  private Map<String, List<Variable>> buildVariablesMultimappedById() {
    /*
     * Build a multimap of the known Variables. Why a multimap? Because some
     * Variables appear in more than one Codebook, and we need to cope with that.
     */
    Map<String, List<Variable>> variablesById = new LinkedHashMap<>();
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      // Unmarshall the Codebook XML and pull its Variables.
      try {
        final ByteSource byteSource = byteSourceFactory.apply(supportedCodebook);
        final Codebook codebook = unmarshallCodebookXml(byteSource);
        for (Variable variable : codebook.getVariables()) {
          if (!variablesById.containsKey(variable.getId()))
            variablesById.put(variable.getId(), new ArrayList<>());
          variablesById.get(variable.getId()).add(variable);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    return variablesById;
  }

  /**
   * Unmarshalls an input stream representing a xml codebook.
   *
   * @param codebookXmlByteSource provides the {@link Codebook} XML {@link InputStream} to
   *     unmarshall
   * @return the {@link Codebook} that was unmarshalled from the specified XML
   */
  private Codebook unmarshallCodebookXml(ByteSource codebookXmlByteSource) throws IOException {
    try {
      final JAXBContext jaxbContext =
          JAXBContext.newInstance(Codebook.class.getPackageName(), Codebook.class.getClassLoader());
      final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
      final CharSource codebookXmlCharSource =
          codebookXmlByteSource.asCharSource(StandardCharsets.UTF_8);
      try (Reader codebookXmlReader = codebookXmlCharSource.openBufferedStream()) {
        Codebook codebook = (Codebook) jaxbUnmarshaller.unmarshal(codebookXmlReader);
        return codebook;
      }
    } catch (JAXBException e) {
      throw new IOException(e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Null input stream", e);
    }
  }
}
