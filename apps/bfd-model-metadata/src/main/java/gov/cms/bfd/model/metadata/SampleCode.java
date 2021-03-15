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
    RifLayout rifBeneficiariesLayout =
        new RifLayoutBuilder("rif_beneficiaries", "RIF: Beneficiaries")
            .groupedColumn(RifBeneficiaryFields.BENE_ID)
            .build();

    RifToJpaMappingBuilder rifBeneficiariesToJpaBuilder = new RifToJpaMappingBuilder();
    RifToJpaMappingBuilderEntity rifBeneficiariesToJpaBuilderEntity =
        rifBeneficiariesToJpaBuilder.outputEntity("gov.cms.bfd.model.rif.Beneficiary");
    rifBeneficiariesToJpaBuilderEntity
        .addField("beneficiaryId")
        .selectValue(String.class, "BENE_ID");
    RifToJpaMapping rifBeneficiariesToJpa = rifBeneficiariesToJpaBuilder.build();

    RifLayout rifClaimsCarrierLayout =
        new RifLayoutBuilder("rif_claims_carrier", "RIF: Carrier Claims")
            .groupedColumn(RifCarrierClaimFields.CLM_ID)
            .groupedColumn(RifCarrierClaimFields.BENE_ID)
            .ungroupedColumn(RifCarrierClaimFields.LINE_NUM)
            .build();

    RifToJpaMappingBuilder rifClaimsCarrierToJpaBuilder = new RifToJpaMappingBuilder();
    RifToJpaMappingBuilderEntity rifClaimsCarrierToJpaBuilderEntity =
        rifClaimsCarrierToJpaBuilder.outputEntity("gov.cms.bfd.model.rif.CarrierClaim");
    rifClaimsCarrierToJpaBuilderEntity.addField("claimId").selectValue(String.class, "CLM_ID");
    rifClaimsCarrierToJpaBuilderEntity
        .addField("beneficiaryId")
        .selectValue(String.class, "BENE_ID");
    rifClaimsCarrierToJpaBuilderEntity.addField("lines"); // TODO value

    RifToJpaMappingBuilder rifClaimsCarrierLinesToJpaBuilder = new RifToJpaMappingBuilder();
    RifToJpaMappingBuilderEntity rifClaimsCarrierLinesToJpaBuilderEntity =
        rifClaimsCarrierLinesToJpaBuilder.outputEntity("gov.cms.bfd.model.rif.CarrierClaimLine");
    rifClaimsCarrierLinesToJpaBuilderEntity.addField("parentClaim"); // TODO value
    rifClaimsCarrierLinesToJpaBuilderEntity
        .addField("lineNumber")
        .selectValue(Integer.class, "LINE_NUM"); // TODO select is wrong -- need descent

    RifToJpaMapping rifClaimsCarrierToJpaBuilder = rifClaimsCarrierToJpaBuilderEntity.build();
  }
}
