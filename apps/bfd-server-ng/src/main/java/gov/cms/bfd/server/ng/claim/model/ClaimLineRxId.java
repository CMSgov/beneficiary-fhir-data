package gov.cms.bfd.server.ng.claim.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class ClaimLineRxId implements Serializable {
    @Column(name = "clm_uniq_id", insertable = false, updatable = false)
    private long claimUniqueId;

    @Column(name = "clm_line_num", insertable = false, updatable = false)
    private int claimLineNumber;
}
