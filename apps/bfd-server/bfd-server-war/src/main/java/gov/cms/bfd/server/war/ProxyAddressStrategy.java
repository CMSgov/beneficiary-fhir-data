package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.IncomingRequestAddressStrategy;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.ForwardedHeaderUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Equivalent of HAPI's {@link ApacheProxyAddressStrategy#forHttp()}, which can't be used because it
 * calls Spring APIs that were removed in Spring Framework 7.
 */
public class ProxyAddressStrategy extends IncomingRequestAddressStrategy {

  /** {@inheritDoc} */
  @Override
  public String determineServerBase(ServletContext servletContext, HttpServletRequest request) {
    String serverBase = super.determineServerBase(servletContext, request);
    ServletServerHttpRequest requestWrapper = new ServletServerHttpRequest(request);
    HttpHeaders headers = requestWrapper.getHeaders();
    UriComponentsBuilder uriBuilder =
        ForwardedHeaderUtils.adaptFromForwardedHeaders(requestWrapper.getURI(), headers);
    uriBuilder.replaceQuery(null);

    if (headers.getFirst(Constants.HEADER_X_FORWARDED_HOST) != null
        && headers.getFirst(Constants.HEADER_X_FORWARDED_PROTO) == null) {
      uriBuilder.scheme("http");
    }

    String path =
        Optional.ofNullable(headers.getFirst(Constants.HEADER_X_FORWARDED_PREFIX))
            .orElseGet(
                () ->
                    StringUtils.defaultIfBlank(
                        UriComponentsBuilder.fromUriString(serverBase).build().getPath(), ""));
    uriBuilder.replacePath(path);
    return uriBuilder.build().toUriString();
  }
}
