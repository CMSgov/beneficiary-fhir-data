package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

/** Claim pricing locality codes. */
public sealed interface ClaimPricingLocalityCode
    permits ClaimPricingLocalityCode.Valid, ClaimPricingLocalityCode.Invalid {

  /**
   * Gets the code value.
   *
   * @return the code
   */
  String getCode();

  /**
   * Gets the display value.
   *
   * @return the display
   */
  String getDisplay();

  /**
   * Convert from a database code.
   *
   * @param code database code
   * @return claim pricing locality code or empty Optional if code is null or blank
   */
  static Optional<ClaimPricingLocalityCode> tryFromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(
        Arrays.stream(Valid.values())
            .filter(v -> v.code.equals(code))
            .map(v -> (ClaimPricingLocalityCode) v)
            .findFirst()
            .orElseGet(() -> new Invalid(code)));
  }

  /**
   * Maps enum/record to FHIR spec.
   *
   * @return extension
   */
  default Extension toFhir() {
    return new Extension(SystemUrls.EXT_CLM_PRCNG_LCLTY_CD_URL)
        .setValue(new Coding(SystemUrls.SYS_CLM_PRCNG_LCLTY_CD, getCode(), getDisplay()));
  }

  /** Enum for all known, valid codes. */
  @AllArgsConstructor
  @Getter
  enum Valid implements ClaimPricingLocalityCode {
    /** 1 - ALABAMA. */
    _1("1", "ALABAMA"),
    /** 2 - ALASKA. */
    _2("2", "ALASKA"),
    /** 3 - ARIZONA. */
    _3("3", "ARIZONA"),
    /** 4 - ARKANSAS. */
    _4("4", "ARKANSAS"),
    /** 5 - ANAHEIM/SANTA ANA, CA. */
    _5("5", "ANAHEIM/SANTA ANA, CA"),
    /** 6 - LOS ANGELES, CA. */
    _6("6", "LOS ANGELES, CA"),
    /** 7 - MARIN/NAPA/SOLANO, CA. */
    _7("7", "MARIN/NAPA/SOLANO, CA"),
    /** 8 - OAKLAND/BERKELEY, CA. */
    _8("8", "OAKLAND/BERKELEY, CA"),
    /** 9 - REST OF CALIFORNIA. */
    _9("9", "REST OF CALIFORNIA"),
    /** 10 - SAN FRANCISCO, CA. */
    _10("10", "SAN FRANCISCO, CA"),
    /** 11 - SAN MATEO, CA. */
    _11("11", "SAN MATEO, CA"),
    /** 12 - SANTA CLARA, CA. */
    _12("12", "SANTA CLARA, CA"),
    /** 13 - VENTURA, CA. */
    _13("13", "VENTURA, CA"),
    /** 14 - COLORADO. */
    _14("14", "COLORADO"),
    /** 15 - CONNECTICUT. */
    _15("15", "CONNECTICUT"),
    /** 16 - DC + MD/VA SUBURBS. */
    _16("16", "DC + MD/VA SUBURBS"),
    /** 17 - DELAWARE. */
    _17("17", "DELAWARE"),
    /** 18 - FORT LAUDERDALE, FL. */
    _18("18", "FORT LAUDERDALE, FL"),
    /** 19 - MIAMI, FL. */
    _19("19", "MIAMI, FL"),
    /** 20 - REST OF FLORIDA. */
    _20("20", "REST OF FLORIDA"),
    /** 21 - ATLANTA, GA. */
    _21("21", "ATLANTA, GA"),
    /** 22 - REST OF GEORGIA. */
    _22("22", "REST OF GEORGIA"),
    /** 23 - HAWAII. */
    _23("23", "HAWAII"),
    /** 24 - IDAHO. */
    _24("24", "IDAHO"),
    /** 25 - CHICAGO, IL. */
    _25("25", "CHICAGO, IL"),
    /** 26 - EAST ST. LOUIS, IL. */
    _26("26", "EAST ST. LOUIS, IL"),
    /** 27 - REST OF ILLINOIS. */
    _27("27", "REST OF ILLINOIS"),
    /** 28 - SUBURBAN CHICAGO, IL. */
    _28("28", "SUBURBAN CHICAGO, IL"),
    /** 29 - INDIANA. */
    _29("29", "INDIANA"),
    /** 30 - IOWA. */
    _30("30", "IOWA"),
    /** 31 - KANSAS. */
    _31("31", "KANSAS"),
    /** 32 - KENTUCKY. */
    _32("32", "KENTUCKY"),
    /** 33 - NEW ORLEANS, LA. */
    _33("33", "NEW ORLEANS, LA"),
    /** 34 - REST OF LOUISIANA. */
    _34("34", "REST OF LOUISIANA"),
    /** 35 - REST OF MAINE. */
    _35("35", "REST OF MAINE"),
    /** 36 - SOUTHERN MAINE. */
    _36("36", "SOUTHERN MAINE"),
    /** 37 - BALTIMORE/SURR. CNTYS, MD. */
    _37("37", "BALTIMORE/SURR. CNTYS, MD"),
    /** 38 - REST OF MARYLAND. */
    _38("38", "REST OF MARYLAND"),
    /** 39 - METROPOLITAN BOSTON. */
    _39("39", "METROPOLITAN BOSTON"),
    /** 40 - REST OF MASSACHUSETTS. */
    _40("40", "REST OF MASSACHUSETTS"),
    /** 41 - DETROIT, MI. */
    _41("41", "DETROIT, MI"),
    /** 42 - REST OF MICHIGAN. */
    _42("42", "REST OF MICHIGAN"),
    /** 43 - MINNESOTA. */
    _43("43", "MINNESOTA"),
    /** 44 - MISSISSIPPI. */
    _44("44", "MISSISSIPPI"),
    /** 45 - METROPOLITAN KANSAS CITY, MO. */
    _45("45", "METROPOLITAN KANSAS CITY, MO"),
    /** 46 - METROPOLITAN ST. LOUIS, MO. */
    _46("46", "METROPOLITAN ST. LOUIS, MO"),
    /** 47 - REST OF MISSOURI. */
    _47("47", "REST OF MISSOURI"),
    /** 48 - MONTANA. */
    _48("48", "MONTANA"),
    /** 49 - NEBRASKA. */
    _49("49", "NEBRASKA"),
    /** 50 - NEVADA. */
    _50("50", "NEVADA"),
    /** 51 - NEW HAMPSHIRE. */
    _51("51", "NEW HAMPSHIRE"),
    /** 52 - NORTHERN NJ. */
    _52("52", "NORTHERN NJ"),
    /** 53 - REST OF NEW JERSEY. */
    _53("53", "REST OF NEW JERSEY"),
    /** 54 - NEW MEXICO. */
    _54("54", "NEW MEXICO"),
    /** 55 - MANHATTAN, NY. */
    _55("55", "MANHATTAN, NY"),
    /** 56 - NYC SUBURBS/LONG I., NY. */
    _56("56", "NYC SUBURBS/LONG I., NY"),
    /** 57 - POUGHKPSIE/N NYC SUBURBS, NY. */
    _57("57", "POUGHKPSIE/N NYC SUBURBS, NY"),
    /** 58 - QUEENS, NY. */
    _58("58", "QUEENS, NY"),
    /** 59 - REST OF NEW YORK. */
    _59("59", "REST OF NEW YORK"),
    /** 60 - NORTH CAROLINA. */
    _60("60", "NORTH CAROLINA"),
    /** 61 - NORTH DAKOTA. */
    _61("61", "NORTH DAKOTA"),
    /** 62 - OHIO. */
    _62("62", "OHIO"),
    /** 63 - OKLAHOMA. */
    _63("63", "OKLAHOMA"),
    /** 64 - PORTLAND, OR. */
    _64("64", "PORTLAND, OR"),
    /** 65 - REST OF OREGON. */
    _65("65", "REST OF OREGON"),
    /** 66 - METROPOLITAN PHILADELPHIA, PA. */
    _66("66", "METROPOLITAN PHILADELPHIA, PA"),
    /** 67 - REST OF PENNSYLVANIA. */
    _67("67", "REST OF PENNSYLVANIA"),
    /** 68 - PUERTO RICO. */
    _68("68", "PUERTO RICO"),
    /** 69 - RHODE ISLAND. */
    _69("69", "RHODE ISLAND"),
    /** 70 - SOUTH CAROLINA. */
    _70("70", "SOUTH CAROLINA"),
    /** 71 - SOUTH DAKOTA. */
    _71("71", "SOUTH DAKOTA"),
    /** 72 - TENNESSEE. */
    _72("72", "TENNESSEE"),
    /** 73 - AUSTIN, TX. */
    _73("73", "AUSTIN, TX"),
    /** 74 - BEAUMONT, TX. */
    _74("74", "BEAUMONT, TX"),
    /** 75 - BRAZORIA, TX. */
    _75("75", "BRAZORIA, TX"),
    /** 76 - DALLAS, TX. */
    _76("76", "DALLAS, TX"),
    /** 77 - FORT WORTH, TX. */
    _77("77", "FORT WORTH, TX"),
    /** 78 - GALVESTON, TX. */
    _78("78", "GALVESTON, TX"),
    /** 79 - HOUSTON, TX. */
    _79("79", "HOUSTON, TX"),
    /** 80 - REST OF TEXAS. */
    _80("80", "REST OF TEXAS"),
    /** 81 - UTAH. */
    _81("81", "UTAH"),
    /** 82 - VERMONT. */
    _82("82", "VERMONT"),
    /** 83 - VIRGIN ISLANDS. */
    _83("83", "VIRGIN ISLANDS"),
    /** 84 - VIRGINIA. */
    _84("84", "VIRGINIA"),
    /** 85 - REST OF WASHINGTON. */
    _85("85", "REST OF WASHINGTON"),
    /** 86 - SEATTLE (KING CNTY), WA. */
    _86("86", "SEATTLE (KING CNTY), WA"),
    /** 87 - WEST VIRGINIA. */
    _87("87", "WEST VIRGINIA"),
    /** 88 - WISCONSIN 89 WYOMING. */
    _88("88", "WISCONSIN 89 WYOMING");

    private final String code;
    private final String display;
  }

  /** Captures unknown/invalid codes. */
  record Invalid(String code) implements ClaimPricingLocalityCode {
    @Override
    public String getDisplay() {
      return "";
    }

    @Override
    public String getCode() {
      return code;
    }
  }
}
