package gov.cms.bfd.pipeline.bridge.model;

import gov.cms.bfd.pipeline.bridge.etl.Parser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Simple class for storing commonly used beneficiary lookup data for use with transformation logic.
 */
@Getter
@RequiredArgsConstructor
public class BeneficiaryData {

  public static final String MBI_NUM = "MBI_NUM";
  public static final String BENE_ID = "BENE_ID";
  public static final String BENE_CRNT_HIC_NUM = "BENE_CRNT_HIC_NUM";

  private final String beneId;
  private final String mbi;
  private final String hicNo;

  /**
   * Creates a single {@link BeneficiaryData} object from the given {@link Parser.Data}.
   *
   * @param data The extracted {@link Parser.Data}.
   * @return A {@link BeneficiaryData} built from the given {@link Parser.Data}.
   */
  public static BeneficiaryData fromData(Parser.Data<String> data) {
    String beneId = data.get(BENE_ID).orElse(null);
    String mbi = data.get(MBI_NUM).orElse(null);
    String hicNo = data.get(BENE_CRNT_HIC_NUM).orElse(null);

    return new BeneficiaryData(beneId, mbi, hicNo);
  }
}
