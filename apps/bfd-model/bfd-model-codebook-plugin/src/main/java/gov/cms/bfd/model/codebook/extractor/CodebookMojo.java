package gov.cms.bfd.model.codebook.extractor;

import gov.cms.bfd.model.codebook.model.Codebook;
import gov.cms.bfd.model.codebook.model.SupportedCodebook;
import gov.cms.bfd.model.codebook.model.Variable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/** Maven plugin that processes codebook PDF files to produce XML files and enum class. */
@Mojo(name = "codebooks", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CodebookMojo extends AbstractMojo {
  /** Path to directory to contain generated xml files. */
  @Parameter(
      property = "xmlFilesDirectory",
      defaultValue = "${project.build.directory}/generated-resources")
  private String xmlFilesDirectory;

  /** Path to directory to contain generated enum class file. */
  @Parameter(
      property = "javaFilesDirectory",
      defaultValue = "${project.build.directory}/generated-sources")
  private String javaFilesDirectory;

  /** Name of package containing the enum class. */
  @Parameter(property = "enumPackage", defaultValue = "gov.cms.bfd.model.codebook")
  private String enumPackage;

  /** Name of package containing the enum class. */
  @Parameter(property = "enumClass", defaultValue = "CcwCodebookVariable")
  private String enumClass;

  /**
   * When true this causes the {@link PdfParser} to log a warning about variables it finds in pdfs.
   * Intended for use when testing a new version of a pdf to detect potential problems with parsing.
   */
  @Parameter(property = "warnAboutVariables", defaultValue = "false")
  private Boolean warnAboutVariables;

  /**
   * Instance of {@link MavenProject} used to call {@link MavenProject#addCompileSourceRoot(String)}
   * to ensure our generated classes are compiled.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /**
   * Execute the mojo logic. Generates XML files and an enum class.
   *
   * @throws MojoExecutionException if execution fails
   */
  @Override
  public void execute() throws MojoExecutionException {
    try {
      new File(xmlFilesDirectory).mkdirs();
      new File(javaFilesDirectory).mkdirs();
      project.addCompileSourceRoot(javaFilesDirectory);
      var resource = new Resource();
      resource.setFiltering(false);
      resource.setDirectory(xmlFilesDirectory);
      project.addResource(resource);
      generateXmlFiles();
      generateEnumClassFile();
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Generate XML files from our PDF files.
   *
   * @return list of generated files
   * @throws IOException may be thrown while writing files
   */
  List<Path> generateXmlFiles() throws IOException {
    final List<Path> outputFiles = new ArrayList<>();

    final Path outputPath = Paths.get(xmlFilesDirectory);

    // Define the Variable fixers/modifiers
    List<Function<Variable, Variable>> variableFixers = buildVariableFixers();

    for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
      // First, parse the PDF to model objects.
      Codebook codebook = new PdfParser(warnAboutVariables).parseCodebookPdf(supportedCodebook);

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
      outputFiles.add(outputFile);
    }

    return outputFiles;
  }

  /**
   * Generate an enum class file from the previously generated XML files.
   *
   * @throws IOException may be thrown while writing files
   */
  void generateEnumClassFile() throws IOException {
    CodebookEnumGenerator.builder()
        .javaDirectory(new File(javaFilesDirectory))
        .xmlDirectory(new File(xmlFilesDirectory))
        .packageName(enumPackage)
        .enumName(enumClass)
        .build()
        .generateEnum();
  }

  /**
   * Build a list of functions that will modify or replace variables as needed.
   *
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
          if (!SupportedCodebook.FFS_CLAIMS.name().equals(v.getCodebook().getId())
              || !v.getId().equals("NCH_CLM_PRVDT_PMT_AMT")) {
            return v;
          }

          v.setId("NCH_CLM_PRVDR_PMT_AMT");
          return v;
        });

    return variableFixers;
  }

  /**
   * Writes the given codebook as xml to a file, overwriting any file that already exists.
   *
   * @param codebook the {@link Codebook} to write out
   * @param outputFile the {@link Path} of the file to write the {@link Codebook} out as XML to
   *     (which will be overwritten if it already exists)
   */
  private static void writeCodebookXmlToFile(Codebook codebook, Path outputFile)
      throws IOException {
    try (FileWriter outputWriter = new FileWriter(outputFile.toFile()); ) {
      JAXBContext jaxbContext = JAXBContext.newInstance(Codebook.class);
      Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      jaxbMarshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.name());

      jaxbMarshaller.marshal(codebook, outputWriter);
    } catch (JAXBException e) {
      throw new IOException(e);
    }
  }
}
