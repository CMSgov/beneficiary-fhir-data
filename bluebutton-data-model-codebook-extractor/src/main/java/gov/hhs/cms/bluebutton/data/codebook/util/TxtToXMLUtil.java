package gov.hhs.cms.bluebutton.data.codebook.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;

import gov.hhs.cms.bluebutton.data.codebook.object.Code;
import gov.hhs.cms.bluebutton.data.codebook.object.Value;
import gov.hhs.cms.bluebutton.data.codebook.object.ValueGroup;

public class TxtToXMLUtil {
	
	private Boolean proceed = false;

	public void covertCodebookTxtToXml(String txtFileName, String xmlFileName) throws IOException {
		
		String currentLine = null;
		String previousLine = null;
		FileWriter destWriter = new FileWriter(xmlFileName);
		BufferedWriter bufferedWriter = new BufferedWriter(destWriter);
		bufferedWriter.write("<codes>");
		bufferedWriter.newLine();

		try (Scanner scanner = new Scanner(new File(txtFileName))) {

			while (scanner.hasNext()) {
				currentLine = getNextLine(scanner);
				if (currentLine == null) {

				} else {
					Code code = new Code();

					if (currentLine.indexOf("LABEL:") > -1) {
						code.setName(StringUtils.trimToEmpty(previousLine));
						code.setLabel(StringUtils.trimToEmpty(currentLine.substring(6)));

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("DESCRIPTION:") > -1) {
								code.setDescription("<p>" + StringUtils.trimToEmpty(currentLine.substring(12)));
								proceed = false;
							} else {
								code.setLabel(code.getLabel() + " " + StringUtils.trimToEmpty(currentLine));
							}
						}

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("SHORT NAME:") > -1) {
								
								if(!StringUtils.endsWith(code.getDescription(), "</p>")) {
									code.setDescription(code.getDescription() + "</p>");
								}
								
								code.setShortName(StringUtils.trimToEmpty(currentLine.substring(11)));
								proceed = false;
							} else {
								if(StringUtils.endsWith(StringUtils.trimToEmpty(currentLine), ".")) {
									code.setDescription(code.getDescription() + " " + StringUtils.trimToEmpty(currentLine) + "</p>");
								} else {
									if(StringUtils.endsWith(code.getDescription(), "</p>")) {
										code.setDescription(code.getDescription() + "<p>" + StringUtils.trimToEmpty(currentLine));
									} else {
										code.setDescription(code.getDescription() + " " + StringUtils.trimToEmpty(currentLine));
									}
								}
							}
						}

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("LONG NAME:") > -1) {
								code.setLongName(StringUtils.trimToEmpty(currentLine.substring(10)));
								proceed = false;
							} else {
								code.setShortName(code.getShortName() + " " + StringUtils.trimToEmpty(currentLine));
							}
						}

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("TYPE:") > -1) {
								code.setType(StringUtils.trimToEmpty(currentLine.substring(5)));
								proceed = false;
							} else {
								code.setLongName(code.getLongName() + " " + StringUtils.trimToEmpty(currentLine));
							}
						}

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("LENGTH:") > -1) {
								code.setLength(StringUtils.trimToEmpty(currentLine.substring(7)));
								proceed = false;
							} else {
								code.setType(code.getType() + " " + StringUtils.trimToEmpty(currentLine));
							}
						}

						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("SOURCE:") > -1) {
								code.setSource(StringUtils.trimToEmpty(currentLine.substring(7)));
								proceed = false;
							} else {
								code.setLength(code.getLength() + " " + StringUtils.trimToEmpty(currentLine));
							}
						}

						if(code.getName().equals("REV_CNTR_PRCNG_IND_CD")) {
							System.out.println("Stop here");
						}
						
						proceed = true;
						boolean source = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (currentLine.indexOf("VALUES:") > -1) {
								source = false;
								if (StringUtils.contains(currentLine, "XXX.XX")) {
									code.setValueFormat("XXX.XX");
								} else {
									populateValue(scanner, currentLine, code);
								}
								proceed = false;
							} else if (source) {
								code.setSource(code.getSource() + " " + StringUtils.trimToEmpty(currentLine));
							} 
						}
						
						proceed = true;
						while (proceed && scanner.hasNext()) {
							currentLine = getNextLine(scanner);
							if (StringUtils.isBlank(currentLine)) {
								proceed = false;
							} else {
								if(StringUtils.endsWith(StringUtils.trimToEmpty(currentLine), ".")) {
									code.setComment(code.getComment() + " " + StringUtils.trimToEmpty(currentLine) + "</p>");
								} else {
									if(StringUtils.endsWith(code.getComment(), "</p>")) {
										code.setComment(code.getComment() + "<p>" + StringUtils.trimToEmpty(currentLine));
									} else {
										code.setComment(code.getComment() + " " + StringUtils.trimToEmpty(currentLine));
									}
								}
							}
						}
						
						if(!StringUtils.endsWith(code.getComment(), "</p>")) {
							code.setComment(code.getComment() + "</p>");
						}
						
						bufferedWriter.write(code.toString());
						bufferedWriter.newLine();
					}
				}

				previousLine = currentLine;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			bufferedWriter.write("  </codes>");
			bufferedWriter.newLine();
			bufferedWriter.write("</codebook>");
			bufferedWriter.newLine();
			bufferedWriter.close();
			destWriter.close();
		}
	}
	
	private String getNextLine(Scanner scanner) {
		
		String line = null;
		boolean isValidLine = true;
		
		while(isValidLine && scanner.hasNext()) {
			line = scanner.nextLine();
			
			if (line == null) {
				line = "";
				isValidLine = false;
			} else if (line.indexOf("^ Back to TOC ^") > -1 || line.indexOf("CMS Chronic Conditions Data Warehouse (CCW) – Codebook ") > -1 || line.indexOf("Master Beneficiary Summary File (MBSF) with Medicare Part A, B, C & D ") > -1 || line.indexOf("Version 1.0  Page") > -1) {
				// Skip these lines
			} else if (line.indexOf("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~") > -1  || line.indexOf("==========================================") > -1) {
				// Treat these lines as blanks
				line = "";
				isValidLine = false;
			} else {
				//Use these lines
				isValidLine = false;
			}
		}
		
		if(line == null) {
			return "";
		} else {
			return line;
		}
	}
	
	private void populateValue(Scanner scanner, String currentLine, Code code) {
		
		ValueGroup valueGroup = null;
		Value value = null;
		boolean previousLineBlank = false;
		
		valueGroup = new ValueGroup();
		
		if(StringUtils.contains(currentLine, "=")) {
			
			value = new Value();
			value.setValue(StringUtils.trimToEmpty(StringUtils.substringBefore(currentLine.substring(7), "=")));
			value.setDescription(StringUtils.trimToEmpty(StringUtils.substringAfter(currentLine.substring(7), "=")));
			
			processValuesDown(currentLine, scanner, code, value, previousLineBlank, valueGroup);
			
		} else if (currentLine.indexOf("COMMENT:") > -1) {
			code.setComment("<p>" + StringUtils.trimToEmpty(currentLine.substring(8)));
			proceed = false;
		} else {

			valueGroup.setDescription(StringUtils.trimToEmpty(currentLine.substring(7)));
			
			while (proceed && scanner.hasNextLine()) {
				currentLine = getNextLine(scanner);
				
				if (currentLine.indexOf("COMMENT:") > -1) {
					code.setComment("<p>" + StringUtils.trimToEmpty(currentLine.substring(8)));
					if(value != null) {
						valueGroup.getValues().add(value);
						value = null;
					}
					code.getValueGroups().add(valueGroup);
					proceed = false;
				} else if (StringUtils.contains(currentLine, "=")) {
					
					value = new Value();
					value.setValue(StringUtils.trimToEmpty(StringUtils.substringBefore(currentLine, "=")));
					value.setDescription(StringUtils.trimToEmpty(StringUtils.substringAfter(currentLine, "=")));
					
					processValuesDown(currentLine, scanner, code, value, previousLineBlank, valueGroup);
					
				} else {
					valueGroup.setDescription(valueGroup.getDescription() + " " + StringUtils.trimToEmpty(currentLine));
				}
			}
		}
	}
	
	private void processValuesDown(String currentLine, Scanner scanner, Code code, Value value, boolean previousLineBlank, ValueGroup valueGroup) {
		while (proceed && scanner.hasNext()) {
			currentLine = getNextLine(scanner);
			
			if (currentLine.indexOf("COMMENT:") > -1) {
				code.setComment("<p>" + StringUtils.trimToEmpty(currentLine.substring(8)));
				if(value != null) {
					valueGroup.getValues().add(value);
					value = null;
				}
				code.getValueGroups().add(valueGroup);
				proceed = false;
			} else if (StringUtils.contains(currentLine, "=")) {
				if(value != null) {
					valueGroup.getValues().add(value);
					value = null;
				}
				
				value = new Value();
				value.setValue(StringUtils.trimToEmpty(StringUtils.substringBefore(currentLine, "=")));
				value.setDescription(StringUtils.trimToEmpty(StringUtils.substringAfter(currentLine, "=")));

				previousLineBlank = false;
			} else if (previousLineBlank && StringUtils.isNotBlank(currentLine)) { // Previous line is blank and current line does not have an = or COMMENT, we assume it is a new value group
				if(value != null) {
					valueGroup.getValues().add(value);
					value = null;
					code.getValueGroups().add(valueGroup);
				}
				
				valueGroup = new ValueGroup();
				valueGroup.setDescription(StringUtils.trimToEmpty(currentLine));
				value = new Value();
			
				previousLineBlank = false;
			}else if (StringUtils.isBlank(currentLine)) {  // mark as a blank line, could be new page or new value group
				previousLineBlank = true;
			} else {
				if(StringUtils.isBlank(value.getDescription())) {
					valueGroup.setDescription(valueGroup.getDescription() + " " + StringUtils.trimToEmpty(currentLine));
				} else {
					value.setDescription(value.getDescription() + " " + StringUtils.trimToEmpty(currentLine));
				}
				previousLineBlank = false;
			}

		}
	}

}
