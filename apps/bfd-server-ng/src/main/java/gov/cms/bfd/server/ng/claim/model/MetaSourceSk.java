package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** The meta source of the claim. */
@Getter
@AllArgsConstructor
public enum MetaSourceSk {
  /** 1 - DDPS. */
  DDPS(1, "D", "DDPS"),
  /** 7 - NCH. */
  NCH(7, "N", "NCH"),
  /** 1001 - MCS. */
  MCS(1001, "M", "MCS"),
  /** 1002 - VMS. */
  VMS(1002, "V", "VMS"),
  /** 1003 - FISS. */
  FISS(1003, "F", "FISS");

  private final int sourceSk;
  private final String prefix;
  private final String display;

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
}
