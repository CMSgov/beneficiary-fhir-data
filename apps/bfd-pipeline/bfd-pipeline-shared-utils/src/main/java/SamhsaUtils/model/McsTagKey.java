package SamhsaUtils.model;

import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
@Getter
public class McsTagKey implements Serializable {
    private RdaMcsClaim clm_id;
    private TagCode code;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McsTagKey mcsTagKey)) return false;
        return this.clm_id.getIdrClmHdIcn().equals(mcsTagKey.clm_id.getIdrClmHdIcn())
                && this.code.name().equals(mcsTagKey.code.name());
    }

    @Override
    public int hashCode() {
        return Objects.hash(clm_id.getIdrClmHdIcn(), code.name());
    }
}
