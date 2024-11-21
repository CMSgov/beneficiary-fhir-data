// package gov.cms.bfd.server.war.stu3.providers;
//
// import ca.uhn.fhir.rest.api.server.RequestDetails;
// import ca.uhn.fhir.rest.api.server.ResponseDetails;
// import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
// import org.hl7.fhir.dstu3.model.CapabilityStatement;
// import org.hl7.fhir.dstu3.model.ContactDetail;
// import org.hl7.fhir.dstu3.model.Coding;
// import org.hl7.fhir.dstu3.model.Meta;
// import org.hl7.fhir.dstu3.model.UriType;
//
// public class RemoveXmlFormatInterceptor extends ResponseInterceptorAdapter {
/// **/
//    @Override
//    public Object interceptResponse(RequestDetails theRequestDetails, ResponseDetails
// theResponseDetails) {
//        // If the response is a CapabilityStatement, modify it
//        if (theResponseDetails.getResponseResource() instanceof CapabilityStatement) {
//            CapabilityStatement capabilityStatement = (CapabilityStatement)
// theResponseDetails.getClass();
//
//            // Remove XML from the "format" list
//            capabilityStatement.getFormat().removeIf(format ->
// format.equals("application/fhir+xml"));
//
//            // Return the modified capability statement
//            theResponseDetails.setResponse(capabilityStatement);
//        }
//        return super.interceptResponse(theRequestDetails, theResponseDetails);
//    }
// }
