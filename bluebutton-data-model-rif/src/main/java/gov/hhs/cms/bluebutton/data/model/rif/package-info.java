/**
 * Contains the model classes for the RIF file format data.
 */
@XmlSchema(namespace = XmlNamespace.BB_RIF, xmlns = {
		@XmlNs(prefix = "bbr", namespaceURI = XmlNamespace.BB_RIF) }, elementFormDefault = XmlNsForm.QUALIFIED)
@RifLayoutsGenerator(spreadsheetResource = "rif-layout-and-fhir-mapping.xlsx", beneficiarySheet = "Beneficiary", beneficiaryHistorySheet = "Beneficiary History", beneficiaryHistoryTempSheet = "Beneficiary History Temp", medicareBeneficiaryIdSheet = "Medicare Beneficiary Id", pdeSheet = "PDE", carrierSheet = "Carrier", inpatientSheet = "Inpatient", outpatientSheet = "Outpatient", hhaSheet = "HHA", dmeSheet = "DME", hospiceSheet = "Hospice", snfSheet = "SNF")
package gov.hhs.cms.bluebutton.data.model.rif;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

import gov.hhs.cms.bluebutton.data.model.codegen.annotations.RifLayoutsGenerator;
