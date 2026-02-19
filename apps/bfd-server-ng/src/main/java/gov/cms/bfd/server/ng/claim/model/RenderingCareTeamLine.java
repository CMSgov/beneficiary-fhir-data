package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "prvdr_rndrng_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_rndrng_careteam_name"))
@AttributeOverride(name = "specialtyCode", column = @Column(name = "clm_rndrg_fed_prvdr_spclty_cd"))
public class RenderingCareTeamLine extends CareTeamBase {

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.RENDERING;
  }
}

// ITEM INSTITUTTIONAL SS
// prvdr_rndrg_prvdr_npi_num      character varying(10),
// bfd_prvdr_rndrg_careteam_name      character varying(135),
// clm_rndrg_prvdr_type_cd        character varying(3),
// clm_rndrg_fed_prvdr_spclty_cd  character varying(2),

// ITEM INSTITUTTIONAL NCH
// bfd_prvdr_rndrng_careteam_name character varying(135),
// clm_rndrg_fed_prvdr_spclty_cd  character varying(2),
// clm_rndrg_prvdr_type_cd        character varying(3),
// prvdr_rndrng_prvdr_npi_num  character varying(10),

// ITEM PROFESSIONAL SS
// prvdr_rndrng_prvdr_npi_num     character varying(10),
// prvdr_rndrng_type_cd           character varying(2),
// bfd_prvdr_rndrng_careteam_name     character varying(135),
// clm_rndrg_fed_prvdr_spclty_cd  character varying(2),
// clm_rndrg_prvdr_tax_num        character varying(10)    not null,
// geo_rndrg_ssa_state_cd         character varying(2),
// clm_rndrg_prvdr_type_cd        character varying(3),

// ITEM PROFESSIONAL NCH
// prvdr_rndrng_prvdr_npi_num     character varying(10),
// prvdr_rndrng_type_cd           character varying(2),
// bfd_prvdr_rndrng_careteam_name     character varying(135),
// clm_rndrg_fed_prvdr_spclty_cd  character varying(2),
// clm_rndrg_prvdr_tax_num        character varying(10),
// geo_rndrg_ssa_state_cd         character varying(2),
// clm_rndrg_prvdr_type_cd         character varying(3),
