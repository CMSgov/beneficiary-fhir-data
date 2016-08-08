/**
 * Handles monitoring Amazon's S3 for new files, and firing events for them.
 */
@XmlSchema(namespace = XmlNamespace.BB_API, xmlns = {
		@XmlNs(prefix = "rps", namespaceURI = XmlNamespace.BB_API) }, elementFormDefault = XmlNsForm.QUALIFIED)
package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
