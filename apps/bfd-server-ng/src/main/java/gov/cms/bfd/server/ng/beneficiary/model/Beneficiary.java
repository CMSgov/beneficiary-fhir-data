package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "valid_beneficiary", schema = "idr")
public class Beneficiary extends BeneficiaryBase {}
