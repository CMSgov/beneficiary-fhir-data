package SamhsaUtils.model;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class FissTagKey implements Serializable {
    private RdaFissClaim clm_id;
    private TagCode code;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FissTagKey fissTagKey)) return false;
        return this.clm_id.getClaimId().equals(fissTagKey.getClm_id().getClaimId())
                && this.code.name().equals(fissTagKey.code.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(clm_id.getClaimId(), code.name());
    }
}
