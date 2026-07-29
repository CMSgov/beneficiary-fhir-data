package gov.cms.bfd.server.ng;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Typed representation of cert partner configuration loaded from properties. */
@Data
@Component
@ConfigurationProperties("bfd.nonsensitive")
public class CertPartnersConfiguration {
  private CertPartnerProperties certPartners = new CertPartnerProperties();
  private List<Partner> partners;

  /**
   * Map for certificate alias mapped to their partner name.
   *
   * @return Map
   */
  public Map<String, String> getPartnerNamesByCertificateAlias() {
    if (partners == null) {
      this.partners =
          new Gson()
              .fromJson(
                  certPartners.getPartnerCertificateJson(),
                  new TypeToken<List<Partner>>() {}.getType());
    }
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

  /** bfd.nonsensitive.cert_partners properties. */
  @Data
  private static class CertPartnerProperties {
    private String partnerCertificateJson = "[]";
  }

  /** Partner certificate configuration from certs.yml. */
  @Data
  private static class Partner {
    private String name;

    @SerializedName("certificate_aliases")
    private List<String> certificateAliases = new ArrayList<>();
  }
}
