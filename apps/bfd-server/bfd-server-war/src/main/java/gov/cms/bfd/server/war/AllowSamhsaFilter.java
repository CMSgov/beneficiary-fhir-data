package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_ALLOWED_CERT_ALIASES_JSON;
import static gov.cms.bfd.server.war.commons.CommonTransformerUtils.SHOULD_FILTER_SAMHSA;

import com.google.gson.Gson;
import gov.cms.bfd.server.war.commons.ClientCertificateUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/***
 * test.
 */
@Component("AllowSamhsaFilterBean")
public class AllowSamhsaFilter extends OncePerRequestFilter {

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AllowSamhsaFilter.class);

  /** List of allowed certificate serial numbers. */
  private final List<BigInteger> samhsaAllowedSerialNumbers;

  /**
   * Creates a new {@link AllowSamhsaFilter}.
   *
   * @param samhsaAllowedCertAliasesJson list of certificate aliases to identify clients that are
   *     allowed to see SAMHSA data
   * @param keyStore server key store
   */
  public AllowSamhsaFilter(
      @Value("${" + SSM_PATH_SAMHSA_ALLOWED_CERT_ALIASES_JSON + "}")
          String samhsaAllowedCertAliasesJson,
      @Qualifier("serverTrustStore") KeyStore keyStore) {
    super();
    Gson deserializer = new Gson();
    String[] samhsaAllowedCertAliases =
        deserializer.fromJson(samhsaAllowedCertAliasesJson, String[].class);
    this.samhsaAllowedSerialNumbers =
        Arrays.stream(samhsaAllowedCertAliases)
            .map(allowedCert -> getCertSerialNumber(keyStore, allowedCert))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
  }

  /**
   * Gets the serial number from the certificate alias.
   *
   * @param keyStore server key store
   * @param allowedCertAlias certificate alias
   * @return serial number
   */
  private static BigInteger getCertSerialNumber(KeyStore keyStore, String allowedCertAlias) {
    try {
      X509Certificate cert = ((X509Certificate) keyStore.getCertificate(allowedCertAlias));
      if (cert == null) {
        LOGGER.error(
            "Certificate {} was configured to allow SAMHSA, but was not found", allowedCertAlias);
        return null;
      }
      return cert.getSerialNumber();
    } catch (KeyStoreException e) {
      LOGGER.error("Error loading keystore", e);
      return null;
    }
  }

  /** {@inheritDoc} */
  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      @NotNull FilterChain chain)
      throws ServletException, IOException {
    BigInteger serialNumber = ClientCertificateUtils.getClientSslSerialNumber(request);
    // Set the attribute on the request so the transformers can check for this property
    request.setAttribute(
        SHOULD_FILTER_SAMHSA,
        serialNumber == null || !samhsaAllowedSerialNumbers.contains(serialNumber));
    chain.doFilter(request, response);
  }
}
