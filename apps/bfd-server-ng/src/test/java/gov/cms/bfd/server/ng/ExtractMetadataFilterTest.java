package gov.cms.bfd.server.ng;

import static org.mockito.Mockito.*;

import gov.cms.bfd.server.ng.filter.ExtractMetadataFilter;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class ExtractMetadataFilterTest {
  @Mock Environment environment;
  @Mock Configuration configuration;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock FilterChain filterChain;

  @Test
  void testPatientMatchMissingHeaders() throws Exception {
    when(request.getRequestURI()).thenReturn("/v3/Fhir/Patient/$idi-match");
    var filter = new ExtractMetadataFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);

    verify(response)
        .sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Missing Required Headers: X-CLIENT-IP, X-CLIENT-NAME, X-CLIENT-ID");
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void testPatientMatchWithHeaders() throws Exception {
    when(request.getRequestURI()).thenReturn("/v3/Fhir/Patient/$idi-match");
    when(request.getHeader("X-Amzn-Mtls-Clientcert")).thenReturn("goodClientcert");
    when(request.getHeader(LoggerConstants.CLIENT_IP_HEADER)).thenReturn("127.0.0.1");
    when(request.getHeader(LoggerConstants.CLIENT_NAME_HEADER)).thenReturn("test-client");
    when(request.getHeader(LoggerConstants.CLIENT_ID_HEADER)).thenReturn("client-123");

    var filter = new ExtractMetadataFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);

    verify(response, never())
        .sendError(
            HttpServletResponse.SC_BAD_REQUEST,
            "Missing Required Headers: X-CLIENT-IP, X-CLIENT-NAME, X-CLIENT-ID");
    verify(filterChain).doFilter(request, response);
  }
}
