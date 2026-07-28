package gov.cms.bfd.server.ng;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Typed representation of cert configuration loaded from certs.yml. */
@Data
@Component
@ConfigurationProperties
public class CertConfiguration {
  private List<Partner> partners = new ArrayList<>();

  /**
   * Map for certificate alias mapped to their partner name.
   *
   * @return Map
   */
  public Map<String, String> getPartnerNamesByCertificateAlias() {
    return partners.stream()
        .flatMap(
            partner ->
                partner.getCertificateAliases().stream()
                    .filter(Objects::nonNull)
                    .map(alias -> Map.entry(alias, partner.getName())))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, Map.Entry::getValue, (existing, ignored) -> existing));
  }

  /** Partner certificate configuration from certs.yml. */
  @Data
  private static class Partner {
    private String name;
    private List<String> certificateAliases = new ArrayList<>();
  }
}
