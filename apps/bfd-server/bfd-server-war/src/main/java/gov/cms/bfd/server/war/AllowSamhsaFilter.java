package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.PROP_SAMHSA_ALLOWED_DNS;
import static gov.cms.bfd.server.war.commons.CommonTransformerUtils.IS_SAMHSA_ALLOWED;

import gov.cms.bfd.server.war.commons.ClientCertificateUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/***
 * test.
 */
@Component("AllowSamhsaFilterBean")
public class AllowSamhsaFilter extends OncePerRequestFilter {

  /** test. */
  private final List<String> samhsaAllowedDns;

  /**
   * test.
   *
   * @param samhsaAllowedDns allowed DNs
   */
  public AllowSamhsaFilter(
      @Value("${" + PROP_SAMHSA_ALLOWED_DNS + ":" + "CN=client-local-dev" + "}")
          String samhsaAllowedDns) {
    super();
    this.samhsaAllowedDns = Arrays.stream(samhsaAllowedDns.split("\\|")).toList();
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain chain)
      throws ServletException, IOException {
    String distinguishedName =
        ClientCertificateUtils.getClientSslPrincipalDistinguishedName(request);
    request.setAttribute(
        IS_SAMHSA_ALLOWED,
        distinguishedName != null && samhsaAllowedDns.contains(distinguishedName));
    chain.doFilter(request, response);
  }
}
