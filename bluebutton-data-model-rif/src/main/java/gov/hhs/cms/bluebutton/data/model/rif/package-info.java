/**
 * Contains the model classes for the RIF file format data.
 */
@XmlSchema(namespace = XmlNamespace.BB_RIF, xmlns = {
		@XmlNs(prefix = "bbr", namespaceURI = XmlNamespace.BB_RIF) }, elementFormDefault = XmlNsForm.QUALIFIED)
@RifLayoutsGenerator(spreadsheetResource = "rif-layout-and-fhir-mapping.xlsx", beneficiarySheet = "Beneficiary", carrierSheet = "Carrier")
package gov.hhs.cms.bluebutton.data.model.rif;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;

import gov.hhs.cms.bluebutton.data.model.codegen.annotations.RifLayoutsGenerator;
