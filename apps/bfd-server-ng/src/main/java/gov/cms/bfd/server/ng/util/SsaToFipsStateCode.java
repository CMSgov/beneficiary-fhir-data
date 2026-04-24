package gov.cms.bfd.server.ng.util;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Optional;

/** Utility for converting SSA state codes to FIPS state codes. */
public class SsaToFipsStateCode {
  private SsaToFipsStateCode() {}

  private static final Map<String, String> SSA_TO_FIPS =
      Map.ofEntries(
          entry("01", "01"),
          entry("02", "02"),
          entry("03", "04"),
          entry("04", "05"),
          entry("05", "06"),
          entry("06", "08"),
          entry("07", "09"),
          entry("08", "10"),
          entry("09", "11"),
          entry("10", "12"),
          entry("11", "13"),
          entry("12", "15"),
          entry("13", "16"),
          entry("14", "17"),
          entry("15", "18"),
          entry("16", "19"),
          entry("17", "20"),
          entry("18", "21"),
          entry("19", "22"),
          entry("20", "23"),
          entry("21", "24"),
          entry("22", "25"),
          entry("23", "26"),
          entry("24", "27"),
          entry("25", "28"),
          entry("26", "29"),
          entry("27", "30"),
          entry("28", "31"),
          entry("29", "32"),
          entry("30", "33"),
          entry("31", "34"),
          entry("32", "35"),
          entry("33", "36"),
          entry("34", "37"),
          entry("35", "38"),
          entry("36", "39"),
          entry("37", "40"),
          entry("38", "41"),
          entry("39", "42"),
          entry("40", "72"),
          entry("41", "44"),
          entry("42", "45"),
          entry("43", "46"),
          entry("44", "47"),
          entry("45", "48"),
          entry("46", "49"),
          entry("47", "50"),
          entry("48", "78"),
          entry("49", "51"),
          entry("50", "53"),
          entry("51", "54"),
          entry("52", "55"),
          entry("53", "56"),
          entry("64", "60"),
          entry("65", "66"),
          entry("97", "69"),
          entry("98", "66"),
          entry("99", "60"));

  /**
   * Converts an SSA state code to its corresponding FIPS state code.
   *
   * @param ssaCode the SSA state code to convert
   * @return the FIPS state code, or empty if no mapping exists
   */
  public static Optional<String> toFips(String ssaCode) {
    return Optional.ofNullable(SSA_TO_FIPS.get(ssaCode));
  }
}
