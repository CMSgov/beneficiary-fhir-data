package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;

/** The meta source of the claim. */
@Getter
@AllArgsConstructor
public enum MetaSourceSk {
  /** 1 - DDPS. */
  DDPS(1, "D", "DDPS", IdrConstants.SYSTEM_TYPE_DDPS),
  /** 7 - NCH. */
  NCH(7, "N", "NCH", IdrConstants.SYSTEM_TYPE_NCH),
  /** 1001 - MCS. */
  MCS(1001, "M", "MCS", IdrConstants.SYSTEM_TYPE_SHARED),
  /** 1002 - VMS. */
  VMS(1002, "V", "VMS", IdrConstants.SYSTEM_TYPE_SHARED),
  /** 1003 - FISS. */
  FISS(1003, "F", "FISS", IdrConstants.SYSTEM_TYPE_SHARED);

  private final int sourceSk;
  private final String prefix;
  private final String display;
  private final String systemType;

  /**
   * Convert from a database code.
   *
   * @param sourceSk database code
   * @return meta source id
   */
  public static MetaSourceSk tryFromSourceSk(Integer sourceSk) {
    return Arrays.stream(values())
        .filter(v -> v.sourceSk == sourceSk)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown meta source: " + sourceSk));
  }

  /**
   * Convert from a source name.
   *
   * @param source source name
   * @return meta source
   */
  public static Optional<MetaSourceSk> tryFromDisplay(String source) {
    return Arrays.stream(values()).filter(v -> v.display.equalsIgnoreCase(source)).findFirst();
  }

  /**
   * Represents the FHIR system type for this claim source.
   *
   * @return the FHIR system type as an optional Coding
   */
  public Optional<Coding> toFhirSystemType() {
    String type =
        switch (this) {
          case DDPS -> IdrConstants.SYSTEM_TYPE_DDPS;
          case NCH -> IdrConstants.SYSTEM_TYPE_NCH;
          case MCS, VMS, FISS -> IdrConstants.SYSTEM_TYPE_SHARED;
        };

    return Optional.of(new Coding().setSystem(SystemUrls.BLUE_BUTTON_SYSTEM_TYPE).setCode(type));
  }
}
