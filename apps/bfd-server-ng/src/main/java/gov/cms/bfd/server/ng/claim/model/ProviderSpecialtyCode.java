package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

/** Maps internal provider specialty codes to NUCC taxonomy codes. */
@Getter
@AllArgsConstructor
@SuppressWarnings({"java:S115", "java:S1192"})
public enum ProviderSpecialtyCode {
  /** 1 - 208D00000X. */
  _1("1", "208D00000X"),
  /** 2 - 208600000X. */
  _2("2", "208600000X"),
  /** 3 - 207K00000X. */
  _3("3", "207K00000X"),
  /** 4 - 207Y00000X. */
  _4("4", "207Y00000X"),
  /** 5 - 207L00000X. */
  _5("5", "207L00000X"),
  /** 6 - 207RC0000X. */
  _6("6", "207RC0000X"),
  /** 7 - 207N00000X. */
  _7("7", "207N00000X"),
  /** 8 - 207Q00000X. */
  _8("8", "207Q00000X"),
  /** 9 - 208VP0014X. */
  _9("9", "208VP0014X"),
  /** 10 - 207RG0100X. */
  _10("10", "207RG0100X"),
  /** 11 - 207R00000X. */
  _11("11", "207R00000X"),
  /** 12 - 204D00000X. */
  _12("12", "204D00000X"),
  /** 13 - 2084N0400X. */
  _13("13", "2084N0400X"),
  /** 14 - 207T00000X. */
  _14("14", "207T00000X"),
  /** 63 - 335V00000X. */
  _63("63", "335V00000X"),
  /** 64 - 231H00000X. */
  _64("64", "231H00000X"),
  /** 65 - 225100000X. */
  _65("65", "225100000X"),
  /** 66 - 207RR0500X. */
  _66("66", "207RR0500X"),
  /** 67 - 225X00000X. */
  _67("67", "225X00000X"),
  /** 68 - 103TC0700X. */
  _68("68", "103TC0700X"),
  /** 69 - 291U00000X. */
  _69("69", "291U00000X"),
  /** 70 - 193200000X. */
  _70("70", "193200000X"),
  /** 71 - 133V00000X. */
  _71("71", "133V00000X"),
  /** 72 - 208VP0000X. */
  _72("72", "208VP0000X"),
  /** 74 - 261QX0203X. */
  _74("74", "261QX0203X"),
  /** 76 - 2086S0129X. */
  _76("76", "2086S0129X"),
  /** 77 - 2086S0129X. */
  _77("77", "2086S0129X"),
  /** 78 - 2086S0129X. */
  _78("78", "2086S0129X"),
  /** 79 - 207RA0401X. */
  _79("79", "207RA0401X"),
  /** 80 - 1041C0700X. */
  _80("80", "1041C0700X"),
  /** 81 - 207RC0200X. */
  _81("81", "207RC0200X"),
  /** 82 - 207RH0000X. */
  _82("82", "207RH0000X"),
  /** 83 - 207RH0003X. */
  _83("83", "207RH0003X"),
  /** 84 - 2083P0901X. */
  _84("84", "2083P0901X"),
  /** 85 - 204E00000X. */
  _85("85", "204E00000X"),
  /** 86 - 2084N0600X. */
  _86("86", "2084N0600X"),
  /** 89 - 364S00000X. */
  _89("89", "364S00000X"),
  /** 90 - 207RX0202X. */
  _90("90", "207RX0202X"),
  /** 91 - 2086X0206X. */
  _91("91", "2086X0206X"),
  /** 92 - 2085R0001X. */
  _92("92", "2085R0001X"),
  /** 93 - 207P00000X. */
  _93("93", "207P00000X"),
  /** 94 - 2085R0204X. */
  _94("94", "2085R0204X"),
  /** 96 - 156FX1800X. */
  _96("96", "156FX1800X"),
  /** 97 - 363A00000X. */
  _97("97", "363A00000X"),
  /** 98 - 207VX0201X. */
  _98("98", "207VX0201X"),
  /** A0 - 282N00000X. */
  _A0("A0", "282N00000X"),
  /** A1 - 314000000X. */
  _A1("A1", "314000000X"),
  /** A2 - 313M00000X. */
  _A2("A2", "313M00000X"),
  /** A3 - 313M00000X. */
  _A3("A3", "313M00000X"),
  /** A4 - 251E00000X. */
  _A4("A4", "251E00000X"),
  /** A5 - 333600000X. */
  _A5("A5", "333600000X"),
  /** A6 - 332B00000X. */
  _A6("A6", "332B00000X"),
  /** A7 - 332B00000X. */
  _A7("A7", "332B00000X"),
  /** A8 - 332B00000X. */
  _A8("A8", "332B00000X");

  private final String code;
  private final String taxonomyCode;

  static ProviderSpecialtyCode fromCode(String code) {
    return Arrays.stream(values())
        .collect(Collectors.toMap(ProviderSpecialtyCode::getCode, e -> e))
        .get(code);
  }

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return provider specialty code
   */
  public static Optional<ProviderSpecialtyCode> fromCodeOptional(String code) {
    return Optional.ofNullable(fromCode(code));
  }

  CodeableConcept toFhir() {
    var codeableConcept =
        new CodeableConcept(
            new Coding()
                .setSystem(SystemUrls.NUCC_PROVIDER_TAXONOMY)
                .setCode(String.valueOf(code)));
    codeableConcept.addCoding(
        new Coding()
            .setSystem(SystemUrls.CMS_CLM_PRVDR_SPCLTY_CD)
            .setCode(String.valueOf(taxonomyCode)));
    return codeableConcept;
  }
}
