package gov.cms.bfd.server.ng.coverage.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;

/** Represents the composite primary key for the {@link BeneficiaryMappedEnrollment} entity. */
@EqualsAndHashCode
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Embeddable
public class BeneficiaryEnrollmentId implements Serializable, Comparable<BeneficiaryEnrollmentId> {

    @Column(name = "bene_sk")
    private Long beneSk;

    @Column(name = "bene_enrlmt_bgn_dt")
    private LocalDate enrollmentBeginDate;

    @Column(name = "bene_enrlmt_pgm_type_cd")
    private String enrollmentProgramTypeCode;

    @Override
    public int compareTo(@NotNull BeneficiaryEnrollmentId o) {
        return Comparator.comparing((BeneficiaryEnrollmentId id) -> id.beneSk)
                .thenComparing(id -> id.enrollmentBeginDate, Comparator.reverseOrder())
                .thenComparing(id -> id.enrollmentProgramTypeCode)
                .compare(this, o);
    }
}
