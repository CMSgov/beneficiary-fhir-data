package gov.cms.bfd.model.codebook.unmarshall;

import gov.cms.bfd.model.codebook.extractor.SupportedCodebook;
import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.sharedutils.exceptions.UncheckedJaxbException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/** Can unmarshall {@link Codebook} XML and find {@link Variable}s. */
public final class CodebookVariableReader {
  /**
   * @return a {@link Map} of the known {@link Codebook} {@link Variable}s, keyed by {@link
   *     Variable#getId()} (with duplicates removed safely)
   */
  public static Map<String, Variable> buildVariablesMappedById() {
    Map<String, List<Variable>> variablesMultimapById = buildVariablesMultimappedById();
    Map<String, Variable> variablesMappedById = new LinkedHashMap<>(variablesMultimapById.size());
    for (String id : variablesMultimapById.keySet()) {
      List<Variable> variablesForId = variablesMultimapById.get(id);
      if (variablesForId.size() == 1) {
        Variable variable = variablesForId.get(0);
        variablesMappedById.put(variable.getId(), variable);
      } else if (Arrays.asList("BENE_ID", "DOB_DT", "GNDR_CD").contains(id)) {
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
   * @return A multimap of the known Variables. Why a multimap? Because some {@link Variable}s
   *     appear in more than one {@link Codebook}, and we need to cope with that.
   */
  private static Map<String, List<Variable>> buildVariablesMultimappedById() {
    /*
     * Build a multimap of the known Variables. Why a multimap? Because some
     * Variables appear in more than one Codebook, and we need to cope with that.
     */
    Map<String, List<Variable>> variablesById = new LinkedHashMap<>();
    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      /*
       * Find the Codebook XML file on the classpath. Note that this code will be used
       * inside an annotation processor, and those have odd context classloaders. So
       * instead of the context classloader, we use the classloader used to load one
       * of the types from the same JAR.
       */
      ClassLoader contextClassLoader = Codebook.class.getClassLoader();
      URL supportedCodebookUrl =
          contextClassLoader.getResource(supportedCodebook.getCodebookXmlResourceName());
      if (supportedCodebookUrl == null) {
        throw new IllegalStateException(
            String.format(
                "Unable to locate classpath resource: '%s'."
                    + " JVM Classpath: '%s'. Classloader: '%s'. Classloader URLs: '%s'.",
                supportedCodebook.getCodebookXmlResourceName(),
                System.getProperty("java.class.path"),
                contextClassLoader,
                contextClassLoader instanceof URLClassLoader
                    ? Arrays.toString(((URLClassLoader) contextClassLoader).getURLs())
                    : "N/A"));
      }

      // Unmarshall the Codebook XML and pull its Variables.
      try (InputStream codebookXmlStream = supportedCodebookUrl.openStream(); ) {
        Codebook codebook = unmarshallCodebookXml(codebookXmlStream);
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
   * @param codebookXmlStream the {@link Codebook} XML {@link InputStream} to unmarshall
   * @return the {@link Codebook} that was unmarshalled from the specified XML
   */
  private static Codebook unmarshallCodebookXml(InputStream codebookXmlStream) {
    try {
      JAXBContext jaxbContext = JAXBContext.newInstance(Codebook.class);
      Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

      InputStreamReader codebookXmlReader =
          new InputStreamReader(codebookXmlStream, StandardCharsets.UTF_8.name());
      Codebook codebook = (Codebook) jaxbUnmarshaller.unmarshal(codebookXmlReader);
      return codebook;
    } catch (JAXBException e) {
      throw new UncheckedJaxbException(e);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Null input stream", e);
    } catch (UnsupportedEncodingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
