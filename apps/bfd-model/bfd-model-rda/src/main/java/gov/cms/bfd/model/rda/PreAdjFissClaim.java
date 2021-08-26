package gov.cms.bfd.model.rda;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

/** Java bean for the FissClaimsJson table's claim column */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class PreAdjFissClaim {
  private String dcn;
  private Long sequenceNumber;
  private String hicNo;
  private char currStatus;
  private char currLoc1;
  private String currLoc2;
  private String medaProvId;
  private String medaProv_6;
  private BigDecimal totalChargeAmount;
  private LocalDate receivedDate;
  private LocalDate currTranDate;
  private String admitDiagCode;
  private String principleDiag;
  private String npiNumber;
  private String mbi;
  private String mbiHash;
  private String fedTaxNumber;
  private Instant lastUpdated;
  private String pracLocAddr1;
  private String pracLocAddr2;
  private String pracLocCity;
  private String pracLocState;
  private String pracLocZip;
  private LocalDate stmtCovFromDate;
  private LocalDate stmtCovToDate;
  private String lobCd;

  public enum ServTypeCdMapping {
    Normal,
    Clinic,
    SpecialFacility,
    Unrecognized
  }

  private ServTypeCdMapping servTypeCdMapping;
  private String servTypeCd;

  private String freqCd;
  private String billTypCd;

  /**
   * String specifying the source of the data contained in this record. Generally this will be the
   * version string returned by the RDA API server but when populating data from mock server it will
   * also include information about the mode the server was running in.
   */
  private String apiSource;

  @Builder.Default private List<PreAdjFissProcCode> procCodes = new ArrayList<>();
  @Builder.Default private List<PreAdjFissDiagnosisCode> diagCodes = new ArrayList<>();
  @Builder.Default private List<PreAdjFissPayer> payers = new ArrayList<>();
}
