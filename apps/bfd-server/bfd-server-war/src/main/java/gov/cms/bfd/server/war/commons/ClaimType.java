package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaim_;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim_;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim_;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim_;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim_;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim_;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.PartDEvent_;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim_;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
import gov.cms.bfd.model.rif.samhsa.HhaTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.model.rif.samhsa.InpatientTag;
import gov.cms.bfd.model.rif.samhsa.OutpatientTag;
import gov.cms.bfd.model.rif.samhsa.SnfTag;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider} {@link
 * gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider}.
 */
public enum ClaimType {
  /** Represents the carrier claim type. */
  CARRIER(
      CarrierClaim.class,
      CarrierClaim_.claimId,
      CarrierClaim_.beneficiaryId,
      (entity) -> ((CarrierClaim) entity).getDateThrough(),
      CarrierTag.class.getName(),
      CarrierClaim_.lines),
  /** Represents the DME claim type. */
  DME(
      DMEClaim.class,
      DMEClaim_.claimId,
      DMEClaim_.beneficiaryId,
      (entity) -> ((DMEClaim) entity).getDateThrough(),
      DmeTag.class.getName(),
      DMEClaim_.lines),
  /** Represents the hha claim type. */
  HHA(
      HHAClaim.class,
      HHAClaim_.claimId,
      HHAClaim_.beneficiaryId,
      (entity) -> ((HHAClaim) entity).getDateThrough(),
      HhaTag.class.getName(),
      HHAClaim_.lines),
  /** Represents the hospice claim type. */
  HOSPICE(
      HospiceClaim.class,
      HospiceClaim_.claimId,
      HospiceClaim_.beneficiaryId,
      (entity) -> ((HospiceClaim) entity).getDateThrough(),
      HospiceTag.class.getName(),
      HospiceClaim_.lines),
  /** Represents the inpatient claim type. */
  INPATIENT(
      InpatientClaim.class,
      InpatientClaim_.claimId,
      InpatientClaim_.beneficiaryId,
      (entity) -> ((InpatientClaim) entity).getDateThrough(),
      InpatientTag.class.getName(),
      InpatientClaim_.lines),
  /** Represents the outpatient claim type. */
  OUTPATIENT(
      OutpatientClaim.class,
      OutpatientClaim_.claimId,
      OutpatientClaim_.beneficiaryId,
      (entity) -> ((OutpatientClaim) entity).getDateThrough(),
      OutpatientTag.class.getName(),
      OutpatientClaim_.lines),
  /** Represents the PDE claim type. */
  PDE(
      PartDEvent.class,
      PartDEvent_.eventId,
      PartDEvent_.beneficiaryId,
      (entity) -> ((PartDEvent) entity).getPrescriptionFillDate(),
      null),
  /** Represents the SNF claim type. */
  SNF(
      SNFClaim.class,
      SNFClaim_.claimId,
      SNFClaim_.beneficiaryId,
      (entity) -> ((SNFClaim) entity).getDateThrough(),
      SnfTag.class.getName(),
      SNFClaim_.lines);

  /** The entity class. */
  private final Class<?> entityClass;

  /** The entity id attribute. */
  private final SingularAttribute<?, Long> entityIdAttribute;

  /** The entity beneficiary id attribute. */
  private final SingularAttribute<?, Long> entityBeneficiaryIdAttribute;

  /** The service end attribute function. */
  private final Function<Object, LocalDate> serviceEndAttributeFunction;

  /** The entity lazy attributes. */
  private final Collection<PluralAttribute<?, ?, ?>> entityLazyAttributes;

  /** entity security Tag Type. */
  private final String entityTagType;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityIdAttribute the value to use for {@link #getEntityIdAttribute()}
   * @param entityBeneficiaryIdAttribute the value to use for {@link
   *     #getEntityBeneficiaryIdAttribute()}
   * @param serviceEndAttributeFunction the service end attribute function
   * @param entityTagType the entity security Tag Type
   * @param entityLazyAttributes the value to use for {@link #getEntityLazyAttributes()}
   */
  ClaimType(
      Class<?> entityClass,
      SingularAttribute<?, Long> entityIdAttribute,
      SingularAttribute<?, Long> entityBeneficiaryIdAttribute,
      Function<Object, LocalDate> serviceEndAttributeFunction,
      String entityTagType,
      PluralAttribute<?, ?, ?>... entityLazyAttributes) {
    this.entityClass = entityClass;
    this.entityIdAttribute = entityIdAttribute;
    this.entityBeneficiaryIdAttribute = entityBeneficiaryIdAttribute;
    this.serviceEndAttributeFunction = serviceEndAttributeFunction;
    this.entityTagType = entityTagType;
    this.entityLazyAttributes =
        entityLazyAttributes != null
            ? Collections.unmodifiableCollection(Arrays.asList(entityLazyAttributes))
            : Collections.emptyList();
  }

  /**
   * Gets the {@link #entityClass}.
   *
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link ClaimType}
   *     in the database
   */
  public Class<?> getEntityClass() {
    return entityClass;
  }

  /**
   * Gets the {@link #entityIdAttribute}.
   *
   * @return the JPA {@link Entity} field used as the entity's {@link Id}
   */
  public SingularAttribute<?, Long> getEntityIdAttribute() {
    return entityIdAttribute;
  }

  /**
   * Gets the {@link #entityBeneficiaryIdAttribute}.
   *
   * @return the JPA {@link Entity} field that is a (foreign keyed) reference to {@link
   *     Beneficiary#getBeneficiaryId()}
   */
  public SingularAttribute<?, Long> getEntityBeneficiaryIdAttribute() {
    return entityBeneficiaryIdAttribute;
  }

  /**
   * Gets the {@link #serviceEndAttributeFunction}.
   *
   * @return the {@link Function} to use to retrieve the {@link LocalDate} to use for service date
   *     filter
   */
  public Function<Object, LocalDate> getServiceEndAttributeFunction() {
    return serviceEndAttributeFunction;
  }

  /**
   * Gets the {@link #entityTagType}.
   *
   * @return the entity security Tag Type
   */
  public String getEntityTagType() {
    return entityTagType;
  }

  /**
   * Gets the {@link #entityLazyAttributes}.
   *
   * @return the {@link PluralAttribute}s in the JPA {@link Entity} that are {@link FetchType#LAZY}
   */
  public Collection<PluralAttribute<?, ?, ?>> getEntityLazyAttributes() {
    return entityLazyAttributes;
  }

  /**
   * Gets the claim type that matches the specified claim type text, if any.
   *
   * @param claimTypeText the lower-cased {@link ClaimType#name()} value to parse back into a {@link
   *     ClaimType}
   * @return the {@link ClaimType} represented by the specified {@link String}
   */
  public static Optional<ClaimType> parse(String claimTypeText) {
    for (ClaimType claimType : ClaimType.values()) {
      if (claimType.name().toLowerCase().equals(claimTypeText)) {
        return Optional.of(claimType);
      }
    }
    return Optional.empty();
  }
}
