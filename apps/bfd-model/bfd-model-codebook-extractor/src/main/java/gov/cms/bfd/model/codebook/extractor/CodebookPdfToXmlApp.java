package gov.cms.bfd.model.codebook.extractor;

import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.sharedutils.exceptions.UncheckedJaxbException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/** A simple application that calls {@link PdfParser} for each of the {@link SupportedCodebook}s. */
public final class CodebookPdfToXmlApp {
  /**
   * The application entry point, which will receive all non-JVM command line options in the <code>
   * args</code> array.
   *
   * @param args
   *     <p>The non-JVM command line arguments that the application was launched with. Must include:
   *     <ol>
   *       <li><code>OUTPUT_DIR</code>: the first (and only) argument for this application must be
   *           the already-existing path to write the parsed XML codebooks files out to
   *     </ol>
   */
  public static void main(String[] args) {
    if (args.length < 1) {
      System.err.println("OUTPUT_DIR argument not specified.");
      System.exit(1);
    }
    if (args.length > 1) {
      System.err.println("Invalid arguments supplied.");
      System.exit(2);
    }

    Path outputPath = Paths.get(args[0]);
    if (!Files.isDirectory(outputPath)) {
      System.err.println("OUTPUT_DIR does not exist.");
      System.exit(3);
    }

    // Define the Variable fixers/modifiers
    List<Function<Variable, Variable>> variableFixers = buildVariableFixers();

    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      // First, parse the PDF to model objects.
      Codebook codebook = PdfParser.parseCodebookPdf(supportedCodebook);

      // Then, fix/modify those model objects as needed.
      ListIterator<Variable> variablesIter = codebook.getVariables().listIterator();
      while (variablesIter.hasNext()) {
        Variable variable = variablesIter.next();
        for (Function<Variable, Variable> variableFixer : variableFixers) {
          variable = variableFixer.apply(variable);
          if (variable == null) throw new IllegalStateException();
        }
        variablesIter.set(variable);
      }

      // Finally, write out the final model objects to XML.
      Path outputFile = outputPath.resolve(supportedCodebook.getCodebookXmlResourceName());
      writeCodebookXmlToFile(codebook, outputFile);
      System.out.printf("Extracted codebook PDF to XML: %s\n", outputFile.toAbsolutePath());
    }
  }

  /**
   * @return the {@link List} of "variable fixer" {@link Function}s, each of which will be given an
   *     opportunity to modify or replace each {@link Variable}
   */
  private static List<Function<Variable, Variable>> buildVariableFixers() {
    List<Function<Variable, Variable>> variableFixers = new LinkedList<>();

    /*
     * Fix a typo in one of the Variable IDs. Appears in the
     * "December 2017, Version 1.4" PDF.
     */
    variableFixers.add(
        v -> {
          if (v.getCodebook().getId() != SupportedCodebook.FFS_CLAIMS.name()
              || !v.getId().equals("NCH_CLM_PRVDT_PMT_AMT")) return v;

          v.setId("NCH_CLM_PRVDR_PMT_AMT");
          return v;
        });

    return variableFixers;
  }

  /**
   * @param codebook the {@link Codebook} to write out
   * @param outputFile the {@link Path} of the file to write the {@link Codebook} out as XML to
   *     (which will be overwritten if it already exists)
   */
  private static void writeCodebookXmlToFile(Codebook codebook, Path outputFile) {
    try (FileWriter outputWriter = new FileWriter(outputFile.toFile()); ) {
      JAXBContext jaxbContext = JAXBContext.newInstance(Codebook.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());

      jaxbMarshaller.marshal(codebook, outputWriter);
    } catch (JAXBException e) {
      throw new UncheckedJaxbException(e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
