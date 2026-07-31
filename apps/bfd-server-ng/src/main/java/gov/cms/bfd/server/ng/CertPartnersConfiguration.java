package gov.cms.bfd.server.ng;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Typed representation of cert partner configuration loaded from properties. */
@Data
@Component
@ConfigurationProperties("bfd.nonsensitive")
public class CertPartnersConfiguration {
  private CertPartnerProperties certPartners = new CertPartnerProperties();

  @Getter(lazy = true)
  private final Map<String, String> partnerNamesByCertificateAlias =
      getPartnerNamesByCertificateAliasInternal();

  private Map<String, String> getPartnerNamesByCertificateAliasInternal() {
    return certPartners.getPartnerCertificate().entrySet().stream()
        .flatMap(
            entry -> {
              var partner = new Gson().fromJson(entry.getValue(), Partner.class);
              return partner.getCertificateAliases().stream()
                  .filter(Objects::nonNull)
                  .map(alias -> Map.entry(alias, partner.getName()));
            })
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /** bfd.nonsensitive.cert_partners properties. */
  @Data
  private static class CertPartnerProperties {
    private Map<String, String> partnerCertificate = new HashMap<>();
  }

  /** Partner certificate configuration from cert_partners properties. */
  @Data
  private static class Partner {
    private String name;

    private List<String> certificateAliases = new ArrayList<>();
  }
}
