package gov.cms.bfd.model.rda;

import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
public class PreAdjMcsDetail {
  private String idrDtlStatus;
  private LocalDate idrDtlFromDate;
  private LocalDate idrDtlToDate;
  private String idrProcCode;
  private String idrModOne;
  private String idrModTwo;
  private String idrModThree;
  private String idrModFour;
  private String idrDtlDiagIcdType;
  private String idrDtlPrimaryDiagCode;
  private String idrKPosLnameOrg;
  private String idrKPosFname;
  private String idrKPosMname;
  private String idrKPosAddr1;
  private String idrKPosAddr2_1st;
  private String idrKPosAddr2_2nd;
  private String idrKPosCity;
  private String idrKPosState;
  private String idrKPosZip;
  private Instant lastUpdated;
}
