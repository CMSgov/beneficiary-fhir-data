package gov.cms.bfd.server.ng;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.cms.bfd.server.ng.filter.AllowedUriFilter;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AllowedUriFilterTest {
  @Mock Configuration configuration;
  @Mock Configuration.Nonsensitive nonsensitive;
  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock FilterChain filterChain;
  @Mock Environment environment;

  @Test
  void testDisableUriAllowed() throws ServletException, IOException {
    when(request.getRequestURI()).thenReturn("/v3/fhir/Patient");
    when(nonsensitive.getDisabledUris()).thenReturn(List.of("/v3/fhir/Patient"));
    when(nonsensitive.getInternalCertificateAliases()).thenReturn(List.of("goodcert"));
    when(configuration.getNonsensitive()).thenReturn(nonsensitive);

    var certificateUtil = spy(new CertificateUtil(configuration, environment));
    doReturn(Optional.of("goodcert")).when(certificateUtil).getAliasAttribute(any());
    var filter = new AllowedUriFilter(certificateUtil, configuration);
    filter.doFilter(request, response, filterChain);
    verify(response, never()).sendError(any(Integer.class));
    verify(response, never()).sendError(any(Integer.class), any(String.class));
    verify(filterChain).doFilter(request, response);
  }
}
