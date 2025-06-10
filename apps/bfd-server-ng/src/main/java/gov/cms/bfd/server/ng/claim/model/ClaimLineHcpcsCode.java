package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hl7.fhir.r4.model.Coding;

import java.util.Optional;
import java.util.regex.Pattern;

@Embeddable
public class ClaimLineHcpcsCode {
  private static final Pattern IS_INTEGER = Pattern.compile("\\d+");

  @Column(name = "clm_line_hcpcs_cd")
  private Optional<String> hcpcsCode;

  Optional<Coding> toFhir() {
    if (hcpcsCode.isEmpty()) {
      return Optional.empty();
    }
    var hcpcs = hcpcsCode.get();
    if (startsWithInt(hcpcs)) {
      return Optional.of(new Coding().setSystem(SystemUrls.AMA_CPT).setCode(hcpcs));
    } else {
      return Optional.of(new Coding().setSystem(SystemUrls.CMS_HCPCS).setCode(hcpcs));
    }
  }

  private boolean startsWithInt(String hcpcs) {
    return IS_INTEGER.matcher(hcpcs.substring(0, 1)).matches();
  }
}
