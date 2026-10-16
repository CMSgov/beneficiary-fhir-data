package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

/** Main entity representing the beneficiary table. */
@Entity
@Getter
@Table(name = "beneficiary", schema = "idr")
@SQLRestriction(ValidBeneficiary.SQL_RESTRICTION)
@Immutable
public class Beneficiary extends BeneficiaryBase {}
