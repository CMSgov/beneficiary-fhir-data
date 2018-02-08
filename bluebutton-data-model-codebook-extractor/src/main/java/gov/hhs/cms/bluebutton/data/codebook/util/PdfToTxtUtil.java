package gov.hhs.cms.bluebutton.data.codebook.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class PdfToTxtUtil {

	public void convertPdfToTxt(String pdfFilePath, String txtFilePath) throws IOException {

		// Covert from PDF to TXT
		FileWriter txtWriter = null;
		BufferedWriter bufferedTxtWriter = null;
		PdfReader pdfReader = null;

		try {
			txtWriter = new FileWriter(txtFilePath);

			bufferedTxtWriter = new BufferedWriter(txtWriter);

			pdfReader = new PdfReader(pdfFilePath);

			int pages = pdfReader.getNumberOfPages();

			for (int page = 1; page < pages; page++) {
		        
				bufferedTxtWriter.write(new String(PdfTextExtractor.getTextFromPage(pdfReader, page).getBytes(Charset.forName("UTF-8"))));
				bufferedTxtWriter.newLine();
			}

		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if(pdfReader != null) {
					pdfReader.close();
					pdfReader = null;
				}
				if(bufferedTxtWriter != null) {
					bufferedTxtWriter.close();
					bufferedTxtWriter = null;
				}
				if(txtWriter != null) {
					txtWriter.close();
					txtWriter = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
