/** Contains the model classes for the RIF file format data. */
@XmlSchema(
    namespace = XmlNamespace.BB_RIF,
    xmlns = {@XmlNs(prefix = "bbr", namespaceURI = XmlNamespace.BB_RIF)},
    elementFormDefault = XmlNsForm.QUALIFIED)
@RifLayoutsGenerator(
    spreadsheetResource = "rif-layout-and-fhir-mapping.xlsx",
    beneficiarySheet = "Beneficiary",
    beneficiaryHistorySheet = "Beneficiary History",
    medicareBeneficiaryIdSheet = "Medicare Beneficiary Id",
    pdeSheet = "PDE",
    carrierSheet = "Carrier",
    inpatientSheet = "Inpatient",
    outpatientSheet = "Outpatient",
    hhaSheet = "HHA",
    dmeSheet = "DME",
    hospiceSheet = "Hospice",
    snfSheet = "SNF")
package gov.cms.bfd.model.rif;

import gov.cms.bfd.model.codegen.annotations.RifLayoutsGenerator;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
