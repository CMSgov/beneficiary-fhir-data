/** Contains the model classes for the RIF file format data. */
@XmlSchema(
    namespace = XmlNamespace.BB_RIF,
    xmlns = {@XmlNs(prefix = "bbr", namespaceURI = XmlNamespace.BB_RIF)},
    elementFormDefault = XmlNsForm.QUALIFIED)
package gov.cms.bfd.model.rif;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
