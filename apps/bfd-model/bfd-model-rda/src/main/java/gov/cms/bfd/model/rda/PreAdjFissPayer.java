package gov.cms.bfd.model.rda;

import java.math.BigDecimal;
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
public class PreAdjFissPayer {
  public enum PayerType {
    BeneZ,
    Insured
  }

  private PayerType payerType;
  private String payersId;
  private String payersName;
  private String relInd;
  private String assignInd;
  private String providerNumber;
  private String adjDcnIcn;
  private BigDecimal priorPmt;
  private BigDecimal estAmtDue;

  // BeneZ only
  private String beneRel;

  // BeneZ only
  private String beneLastName;

  // BeneZ only
  private String beneFirstName;

  // BeneZ only
  private String beneMidInit;

  // BeneZ only
  private String beneSsnHic;

  // Insured only
  private String insuredRel;

  // Insured only
  private String insuredName;

  // Insured only
  private String insuredSsnHic;

  private String insuredGroupName;

  // Insured only
  private String insuredGroupNbr;

  // BeneZ only
  private LocalDate beneDob;

  // BeneZ only
  private String beneSex;

  private String treatAuthCd;
  private String insuredSex;
  private String insuredRelX12;

  // Insured only
  private LocalDate insuredDob;

  // Insured only
  private String insuredDobText;

  private Instant lastUpdated;
}
