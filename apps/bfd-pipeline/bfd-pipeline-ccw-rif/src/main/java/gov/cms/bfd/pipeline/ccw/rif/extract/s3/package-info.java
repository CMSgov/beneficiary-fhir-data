/** Handles monitoring Amazon's S3 for new files, and firing events for them. */
@XmlSchema(
    namespace = XmlNamespace.BB_RIF,
    xmlns = {@XmlNs(prefix = "bbr", namespaceURI = XmlNamespace.BB_RIF)},
    elementFormDefault = XmlNsForm.QUALIFIED)
package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.XmlNamespace;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
