package gov.hhs.cms.bluebutton.data.codebook.file.pde;

import java.io.IOException;

import gov.hhs.cms.bluebutton.data.codebook.util.PdfToTxtUtil;
import gov.hhs.cms.bluebutton.data.codebook.util.TxtToXMLUtil;


/** 
 * @author Cassidy.Shay
 * @version 1.0.0
 * Descriptions:  Parses the pde codebook PDF that is save locally from 
 *                https://www.ccwdata.org/documents/10280/19022436/codebook-pde.pdf
 *                and generates an XML file from.  You need to set the PDF, TXT and XML
 *                variables and then run the main method to execute.
 */

public class PdePdfToXmlUtil {

	// Path to PDF to Parse
	public static final String PDF = "C:/TEMP/fhir/codebook-pde.pdf";
	// Path to working text file
	public static final String TXT = "C:/TEMP/fhir/codebook-pde.txt";
	// Path to XML file that will be generated from the PDF file
	public static final String XML = "C:/TEMP/fhir/codebook-pde.xml";

	public static void main(String[] args) throws IOException {
		
		// First, convert PDF to TXT file
		new PdfToTxtUtil().convertPdfToTxt(PDF, TXT);
		
		// Parse TXT file and generate XML file from it's data
		new TxtToXMLUtil().covertCodebookTxtToXml(TXT, XML);
		
	}
}
