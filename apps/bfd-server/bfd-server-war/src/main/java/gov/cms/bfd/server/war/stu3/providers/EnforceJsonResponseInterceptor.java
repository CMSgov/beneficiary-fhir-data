// package gov.cms.bfd.server.war.stu3.providers;
//
// import ca.uhn.fhir.interceptor.api.Hook;
// import ca.uhn.fhir.interceptor.api.Pointcut;
// import ca.uhn.fhir.rest.api.EncodingEnum;
// import ca.uhn.fhir.rest.api.server.RequestDetails;
// import ca.uhn.fhir.rest.api.server.ResponseDetails;
// import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
// import ca.uhn.fhir.rest.server.interceptor.ServerOperationInterceptorAdapter;
//
// public class EnforceJsonResponseInterceptor {
//
//    @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
//    public void checkResponseFormat(RequestDetails request) {
//
//        request.getResponse().setHeader();
//    }
// }
