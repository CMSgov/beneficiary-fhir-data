package gov.cms.bfd.model.metadata;

import gov.cms.bfd.model.metadata.ccw.rif.RifBeneficiaryFields;
import gov.cms.bfd.model.metadata.ccw.rif.RifCarrierClaimFields;

/** Just some sample code to prove out this whole mad science project. */
public final class SampleCode {
  /**
   * Run the sample, such as it is.
   *
   * @param args (not used)
   */
  public static void main(String[] args) {
    RifToJpaMappingBuilder rifMappingBuilder = new RifToJpaMappingBuilder();

    RifLayout rifBeneficiariesLayout =
        new RifLayoutBuilder("rif_beneficiaries", "RIF: Beneficiaries")
            .groupedColumn(RifBeneficiaryFields.BENE_ID)
            .build();

    RifToJpaMappingBuilderEntity beneficiariesMapping =
        rifMappingBuilder.addRootEntityMapping(
            rifBeneficiariesLayout, "gov.cms.bfd.model.rif.Beneficiary");
    beneficiariesMapping.addField(
        "beneficiaryId", beneficiariesMapping.selectValue(RifBeneficiaryFields.BENE_ID));

    RifLayout rifClaimsCarrierLayout =
        new RifLayoutBuilder("rif_claims_carrier", "RIF: Carrier Claims")
            .groupedColumn(RifCarrierClaimFields.CLM_ID)
            .groupedColumn(RifCarrierClaimFields.BENE_ID)
            .ungroupedColumn(RifCarrierClaimFields.LINE_NUM)
            .build();

    RifToJpaMappingBuilderEntity claimsCarrierLineMapping =
        rifMappingBuilder.addChildEntityMapping("gov.cms.bfd.model.rif.CarrierClaimLine");
    claimsCarrierLineMapping.addField(
        "parentClaim", claimsCarrierLineMapping.selectGroupParentEntity());
    claimsCarrierLineMapping.addField(
        "lineNumber", claimsCarrierLineMapping.selectValue(RifCarrierClaimFields.LINE_NUM));

    RifToJpaMappingBuilderEntity claimsCarrierMapping =
        rifMappingBuilder.addRootEntityMapping(
            rifClaimsCarrierLayout, "gov.cms.bfd.model.rif.CarrierClaim");
    claimsCarrierMapping.addField(
        "claimId", claimsCarrierMapping.selectValue(RifCarrierClaimFields.CLM_ID));
    claimsCarrierMapping.addField(
        "beneficiaryId", claimsCarrierMapping.selectValue(RifCarrierClaimFields.BENE_ID));
    claimsCarrierMapping.addField(
        "lines", claimsCarrierMapping.selectGroupedRows(claimsCarrierLineMapping));

    RifToJpaMapping rifMapping = rifMappingBuilder.build();
  }
}
