package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;

/** Entity representing the beneficiary XREF table. */
@Entity
@Table(name = "beneficiary_xref", schema = "idr")
public class BeneficiaryXref {

    @Column(name = "bene_sk")
    private long beneSk;

    @Column(name = "bene_xref_sk")
    private long beneXrefSk;

    @Column(name = "bene_hicn_num")
    private String beneHicnNum;

    @Column(name = "bene_kill_cred_cd")
    private String beneKillCred;

    @Column(name = "idr_updt_ts")
    private ZonedDateTime idrUpdateTimestamp;

    @Column(name = "src_rec_ctre_ts")
    private ZonedDateTime srcRecCtreTimestamp;

    @Column(name = "bfd_created_ts")
    private ZonedDateTime bfdCreatedTimestamp;

    @Column(name = "bfd_updated_ts")
    private ZonedDateTime bfdUpdatedTimestamp;
}
