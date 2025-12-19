package gov.cms.bfd.server.ng.claim.model;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Claim audit trail status codes mapped from CLM_AUDT_TRL_STUS_CD. */
@Getter
@AllArgsConstructor
public enum ClaimAuditTrailStatusCode {

  /** Maps CLM_AUDT_TRL_STUS_CD A → queued. */
  A("A", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD B → queued. */
  B("B", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD C → partial. */
  C("C", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** Maps CLM_AUDT_TRL_STUS_CD D → complete. */
  D("D", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD E → complete. */
  E("E", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD F → complete. */
  F("F", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD G → complete. */
  G("G", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD J → queued. */
  J("J", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD K → queued. */
  K("K", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD L → partial. */
  L("L", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** Maps CLM_AUDT_TRL_STUS_CD M → complete. */
  M("M", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD N → complete. */
  N("N", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD P → complete. */
  P("P", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD Q → complete. */
  Q("Q", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD R → complete. */
  R("R", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD U → complete. */
  U("U", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD V → complete. */
  V("V", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD W → complete. */
  W("W", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD X → complete. */
  X("X", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD Y → complete. */
  Y("Y", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD Z → complete. */
  Z("Z", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 1 → queued. */
  _1("1", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 2 → queued. */
  _2("2", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 3 → partial. */
  _3("3", ExplanationOfBenefit.RemittanceOutcome.PARTIAL),

  /** Maps CLM_AUDT_TRL_STUS_CD 4 → complete. */
  _4("4", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 5 → complete. */
  _5("5", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 6 → error. */
  _6("6", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 8 → complete. */
  _8("8", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 9 → complete. */
  _9("9", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 00 → complete. */
  _00("00", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 01 → complete. */
  _01("01", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 02 → complete. */
  _02("02", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 03 → complete. */
  _03("03", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 04 → complete. */
  _04("04", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 05 → error. */
  _05("05", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 06 → error. */
  _06("06", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 07 → complete. */
  _07("07", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 08 → complete. */
  _08("08", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 09 → complete. */
  _09("09", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 10 → complete. */
  _10("10", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 11 → queued. */
  _11("11", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 12 → queued. */
  _12("12", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 13 → complete. */
  _13("13", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 14 → complete. */
  _14("14", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 15 → complete. */
  _15("15", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 16 → complete. */
  _16("16", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 17 → queued. */
  _17("17", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 18 → queued. */
  _18("18", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 19 → queued. */
  _19("19", ExplanationOfBenefit.RemittanceOutcome.QUEUED),

  /** Maps CLM_AUDT_TRL_STUS_CD 20 → error. */
  _20("20", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 21 → complete. */
  _21("21", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 22 → complete. */
  _22("22", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 23 → error. */
  _23("23", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 24 → complete. */
  _24("24", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 25 → complete. */
  _25("25", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 26 → complete. */
  _26("26", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 27 → complete. */
  _27("27", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 28 → complete. */
  _28("28", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 29 → error. */
  _29("29", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 30 → error. */
  _30("30", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 31 → "" (no outcome). */
  _31("31", null),

  /** Maps CLM_AUDT_TRL_STUS_CD 32 → "" (no outcome). */
  _32("32", null),

  /** Maps CLM_AUDT_TRL_STUS_CD 33 → complete. */
  _33("33", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 34 → error. */
  _34("34", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 35 → complete. */
  _35("35", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 36 → complete. */
  _36("36", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 37 → error. */
  _37("37", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 38 → complete. */
  _38("38", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 39 → complete. */
  _39("39", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 40 → complete. */
  _40("40", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 41 → complete. */
  _41("41", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 42 → complete. */
  _42("42", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 43 → complete. */
  _43("43", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 44 → complete. */
  _44("44", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 45 → error. */
  _45("45", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 46 → complete. */
  _46("46", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 47 → complete. */
  _47("47", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 48 → complete. */
  _48("48", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 49 → complete. */
  _49("49", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 50 → complete. */
  _50("50", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 51 → complete. */
  _51("51", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 52 → error. */
  _52("52", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 53 → complete. */
  _53("53", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 54 → complete. */
  _54("54", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 55 → complete. */
  _55("55", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 56 → complete. */
  _56("56", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 57 → complete. */
  _57("57", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 58 → error. */
  _58("58", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 59 → complete. */
  _59("59", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 60 → error. */
  _60("60", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 61 → complete. */
  _61("61", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 62 → error. */
  _62("62", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 63 → complete. */
  _63("63", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 64 → complete. */
  _64("64", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 65 → complete. */
  _65("65", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 66 → complete. */
  _66("66", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 67 → complete. */
  _67("67", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 68 → complete. */
  _68("68", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 69 → complete. */
  _69("69", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 70 → complete. */
  _70("70", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 71 → complete. */
  _71("71", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 72 → complete. */
  _72("72", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 73 → complete. */
  _73("73", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 74 → complete. */
  _74("74", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 75 → complete. */
  _75("75", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 76 → complete. */
  _76("76", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 77 → complete. */
  _77("77", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 78 → "". */
  _78("78", null),

  /** Maps CLM_AUDT_TRL_STUS_CD 79 → "". */
  _79("79", null),

  /** Maps CLM_AUDT_TRL_STUS_CD 80 → complete. */
  _80("80", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 81 → complete. */
  _81("81", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 82 → "". */
  _82("82", null),

  /** Maps CLM_AUDT_TRL_STUS_CD 83 → complete. */
  _83("83", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 84 → complete. */
  _84("84", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 85 → complete. */
  _85("85", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 86 → complete. */
  _86("86", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 87 → complete. */
  _87("87", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 88 → error. */
  _88("88", ExplanationOfBenefit.RemittanceOutcome.ERROR),

  /** Maps CLM_AUDT_TRL_STUS_CD 89 → complete. */
  _89("89", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 90 → complete. */
  _90("90", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 91 → complete. */
  _91("91", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 92 → complete. */
  _92("92", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 94 → complete. */
  _94("94", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 95 → complete. */
  _95("95", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 96 → complete. */
  _96("96", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 97 → complete. */
  _97("97", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 98 → complete. */
  _98("98", ExplanationOfBenefit.RemittanceOutcome.COMPLETE),

  /** Maps CLM_AUDT_TRL_STUS_CD 99 → complete. */
  _99("99", ExplanationOfBenefit.RemittanceOutcome.COMPLETE);

  private final String code;
  private final ExplanationOfBenefit.RemittanceOutcome outcome;

  /**
   * Convert from the database code.
   *
   * @param code database value
   * @return matching enum constant (if any)
   */
  public static Optional<ClaimAuditTrailStatusCode> tryFromCode(String code) {
    return Arrays.stream(values()).filter(v -> v.code.equals(code)).findFirst();
  }
}
