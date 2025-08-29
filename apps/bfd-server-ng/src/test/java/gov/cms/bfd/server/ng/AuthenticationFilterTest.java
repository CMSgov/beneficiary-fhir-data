package gov.cms.bfd.server.ng;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.cms.bfd.server.ng.filter.AuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AuthenticationFilterTest {
  @Mock Environment environment;
  @Mock Configuration configuration;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock FilterChain filterChain;

  @Test
  void testNotAllowedPaths() throws ServletException, IOException {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"aws"});
    when(request.getRequestURI()).thenReturn("/v3/Fhir/Patient");
    var filter = new AuthenticationFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);
    verify(response).sendError(401, "Missing or invalid certificate header.");
  }

  @Test
  void testNotAllowedPathsWithInvalidCert() throws ServletException, IOException {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"aws"});
    when(request.getRequestURI()).thenReturn("/v3/Fhir/Patient");
    when(request.getHeader("X-Amzn-Mtls-Clientcert")).thenReturn("badcert");
    when(configuration.getClientCertsToAliases()).thenReturn(Map.of("goodcert", "alias"));
    var filter = new AuthenticationFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);
    verify(response).sendError(401, "Missing or invalid certificate header.");
  }

  @Test
  void testNotAllowedPathsWithAuthDisabled() throws ServletException, IOException {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"local"});
    when(request.getRequestURI()).thenReturn("/v3/fhir/Patient");
    var filter = new AuthenticationFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);
    verify(response, never()).sendError(any(Integer.class));
    verify(response, never()).sendError(any(Integer.class), any(String.class));
    verify(filterChain).doFilter(request, response);
  }

  @Test
  void testAuthSuccess() throws ServletException, IOException {
    when(environment.getActiveProfiles()).thenReturn(new String[] {"aws"});
    when(request.getRequestURI()).thenReturn("/v3/fhir/Patient");
    when(request.getHeader("X-Amzn-Mtls-Clientcert")).thenReturn("goodcert");
    when(configuration.getClientCertsToAliases()).thenReturn(Map.of("goodcert", "alias"));
    var filter = new AuthenticationFilter(new CertificateUtil(configuration, environment));
    filter.doFilter(request, response, filterChain);
    verify(response, never()).sendError(any(Integer.class));
    verify(response, never()).sendError(any(Integer.class), any(String.class));
    verify(filterChain).doFilter(request, response);
    verify(request).setAttribute("CLIENT_CERT_ALIAS", "alias");
  }
}
