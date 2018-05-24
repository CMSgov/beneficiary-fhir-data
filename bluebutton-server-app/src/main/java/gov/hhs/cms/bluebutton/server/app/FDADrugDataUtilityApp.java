package gov.hhs.cms.bluebutton.server.app;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

/**
 * A simple application that downloads the FDA NDC (national drug code) file;
 * unzips it and then converts it to UTF-8 format
 */
public final class FDADrugDataUtilityApp {

	/**
	 * Size of the buffer to read/write data
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * 
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
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length < 1) {
			System.err.println("OUTPUT_DIR argument not specified for FDA NDC download.");
			System.exit(1);
		}
		if (args.length > 1) {
			System.err.println("Invalid arguments supplied for FDA NDC download.");
			System.exit(2);
		}

		Path outputPath = Paths.get(args[0]);
		if (!Files.isDirectory(outputPath)) {
			System.err.println("OUTPUT_DIR does not exist for FDA NDC download.");
			System.exit(3);
		}

		// download FDA NDC file
		String nationalDrugCodeDownloadableFile = "https://www.accessdata.fda.gov/cder/ndctext.zip";
		String downloadedNdcZipFile = outputPath.toString() + "\\ndctext.zip";
		try {
			// connectionTimeout, readTimeout = 10 seconds
			FileUtils.copyURLToFile(new URL(nationalDrugCodeDownloadableFile), new File(downloadedNdcZipFile), 10000, 10000);
		} catch (IOException e) {
			e.printStackTrace();
		}
		 
		// unzip FDA NDC file
		unzip(downloadedNdcZipFile, outputPath.toString());
		Files.move(Paths.get(outputPath.toString() + "\\product.txt"),
				Paths.get(outputPath.toString() + "\\fda_products_cp1252.tsv"), REPLACE_EXISTING);
		
		// convert file format from cp1252 to utf8
		CharsetDecoder inDec=Charset.forName("windows-1252").newDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);

		CharsetEncoder outEnc=StandardCharsets.UTF_8.newEncoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);

		try
		(FileInputStream is = new FileInputStream(outputPath.toString() + "\\fda_products_cp1252.tsv");
			 BufferedReader reader=new BufferedReader(new InputStreamReader(is, inDec));
				FileOutputStream fw = new FileOutputStream(outputPath.toString() + "\\fda_products_utf8.tsv");
			 BufferedWriter out=new BufferedWriter(new OutputStreamWriter(fw, outEnc))) {

			 for(String in; (in = reader.readLine()) != null; ) {
				   out.write(in);
				   out.newLine();
		      }
		}

		/*
		 * Convert tab separated file to comma separated file
		 */

		File outputNDCFile = new File(outputPath.toString() + "\\fda_products_utf8.csv");

		try {
			String line;
			Stack stack = new Stack();
			BufferedReader br = new BufferedReader(new FileReader(outputPath.toString() + "\\fda_products_utf8.tsv"));
			FileWriter writer = (new FileWriter(outputNDCFile));
			while ((line = br.readLine()) != null)
				stack.push(line.replace("\t", ","));
			while (!stack.isEmpty()) {
				writer.write((String) stack.pop());
				if (!stack.empty())
					writer.write("\n");
			}

		} catch (FileNotFoundException a) {
			System.out.println("Could not open file");
			a.printStackTrace();
		} catch (IOException b) {
			System.out.println("IOException occured");
			b.printStackTrace();
		} catch (Exception c) {
			c.printStackTrace();
		}
	}

	/**
	 * Extracts a zip file specified by the zipFilePath to a directory specified by
	 * destDirectory (will be created if does not exists)
	 * 
	 * @param zipFilePath
	 * @param destDirectory
	 * @throws IOException
	 */
	private static void unzip(String zipFilePath, String destDirectory) throws IOException {
		File destDir = new File(destDirectory);
		if (!destDir.exists()) {
			destDir.mkdir();
		}
		ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
		ZipEntry entry = zipIn.getNextEntry();
		// iterates over entries in the zip file
		while (entry != null) {
			String filePath = destDirectory + File.separator + entry.getName();
			if (!entry.isDirectory()) {
				// if the entry is a file, extracts it
				extractFile(zipIn, filePath);
			} else {
				// if the entry is a directory, make the directory
				File dir = new File(filePath);
				dir.mkdir();
			}
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
		}
		zipIn.close();
	}

	/**
	 * Extracts a zip entry (file entry)
	 * 
	 * @param zipIn
	 * @param filePath
	 * @throws IOException
	 */
	private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
		byte[] bytesIn = new byte[BUFFER_SIZE];
		int read = 0;
		while ((read = zipIn.read(bytesIn)) != -1) {
			bos.write(bytesIn, 0, read);
		}
		bos.close();
	}
}
