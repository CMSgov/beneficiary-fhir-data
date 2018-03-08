package gov.hhs.cms.bluebutton.data.codebook.extractor;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.justdavis.karl.misc.exceptions.unchecked.UncheckedJaxbException;

import gov.hhs.cms.bluebutton.data.codebook.model.Codebook;

/**
 * A simple application that calls {@link PdfParser} for each of the
 * {@link SupportedCodebook}s.
 */
public final class CodebookPdfToXmlApp {
	/**
	 * The application entry point, which will receive all non-JVM command line
	 * options in the <code>args</code> array.
	 * 
	 * @param args
	 *            <p>
	 *            The non-JVM command line arguments that the application was
	 *            launched with. Must include:
	 *            </p>
	 *            <ol>
	 *            <li><code>OUTPUT_DIR</code>: the first (and only) argument for
	 *            this application must be the already-existing path to write the
	 *            parsed XML codebooks files out to</li>
	 *            </ol>
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

		for (SupportedCodebook supportedCodebook : SupportedCodebook.values()) {
			Codebook codebook = PdfParser.parseCodebookPdf(supportedCodebook);

			Path outputFile = outputPath.resolve(supportedCodebook.getCodebookXmlResourceName());
			writeCodebookXmlToFile(codebook, outputFile);
			System.out.printf("Extracted codebook PDF to XML: %s\n", outputFile.toAbsolutePath());
		}
	}

	/**
	 * @param codebook
	 *            the {@link Codebook} to write out
	 * @param outputFile
	 *            the {@link Path} of the file to write the {@link Codebook} out as
	 *            XML to (which will be overwritten if it already exists)
	 */
	private static void writeCodebookXmlToFile(Codebook codebook, Path outputFile) {
		try (FileWriter outputWriter = new FileWriter(outputFile.toFile());) {
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
