package gov.cms.bfd.server.ng.claim.model.rx.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Claim table. Suppress SonarQube Monster Class warning that dependencies to other class should be
 * reduced from 21 to the max 20. Ignore. Class itself is relatively short in lines of code.
 */
@Getter
@Entity
@Table(name = "claim_rx", schema = "idr")
public class ClaimCmsRx extends ClaimRxBase {}
