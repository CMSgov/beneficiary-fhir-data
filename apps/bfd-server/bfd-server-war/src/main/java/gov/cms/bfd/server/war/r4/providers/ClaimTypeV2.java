package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaim_;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.DMEClaim_;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HHAClaim_;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaim_;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.InpatientClaim_;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim_;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.PartDEvent_;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaim_;
import gov.cms.bfd.server.war.commons.ClaimTypeTransformerV2;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/**
 * Enumerates the various Beneficiary FHIR Data Server (BFD) claim types that are supported by
 * {@link R4ExplanationOfBenefitResourceProvider}.
 */
public enum ClaimTypeV2 {
  CARRIER(
      CarrierClaim.class,
      CarrierClaim_.claimId,
      CarrierClaim_.beneficiaryId,
      (entity) -> ((CarrierClaim) entity).getDateThrough(),
      CarrierClaimTransformerV2::transform,
      CarrierClaim_.lines),

  DME(
      DMEClaim.class,
      DMEClaim_.claimId,
      DMEClaim_.beneficiaryId,
      (entity) -> ((DMEClaim) entity).getDateThrough(),
      DMEClaimTransformerV2::transform,
      DMEClaim_.lines),

  PDE(
      PartDEvent.class,
      PartDEvent_.eventId,
      PartDEvent_.beneficiaryId,
      (entity) -> ((PartDEvent) entity).getPrescriptionFillDate(),
      PartDEventTransformerV2::transform),

  INPATIENT(
      InpatientClaim.class,
      InpatientClaim_.claimId,
      InpatientClaim_.beneficiaryId,
      (entity) -> ((InpatientClaim) entity).getDateThrough(),
      InpatientClaimTransformerV2::transform,
      InpatientClaim_.lines),

  OUTPATIENT(
      OutpatientClaim.class,
      OutpatientClaim_.claimId,
      OutpatientClaim_.beneficiaryId,
      (entity) -> ((OutpatientClaim) entity).getDateThrough(),
      OutpatientClaimTransformerV2::transform,
      OutpatientClaim_.lines),

  HOSPICE(
      HospiceClaim.class,
      HospiceClaim_.claimId,
      HospiceClaim_.beneficiaryId,
      (entity) -> ((HospiceClaim) entity).getDateThrough(),
      HospiceClaimTransformerV2::transform,
      HospiceClaim_.lines),

  SNF(
      SNFClaim.class,
      SNFClaim_.claimId,
      SNFClaim_.beneficiaryId,
      (entity) -> ((SNFClaim) entity).getDateThrough(),
      SNFClaimTransformerV2::transform,
      SNFClaim_.lines),

  HHA(
      HHAClaim.class,
      HHAClaim_.claimId,
      HHAClaim_.beneficiaryId,
      (entity) -> ((HHAClaim) entity).getDateThrough(),
      HHAClaimTransformerV2::transform,
      HHAClaim_.lines);

  private final Class<?> entityClass;
  private final SingularAttribute<?, ?> entityIdAttribute;
  private final SingularAttribute<?, ?> entityBeneficiaryIdAttribute;
  private final Function<Object, LocalDate> serviceEndAttributeFunction;
  private final ClaimTypeTransformerV2 transformer;
  private final Collection<PluralAttribute<?, ?, ?>> entityLazyAttributes;

  /**
   * Enum constant constructor.
   *
   * @param entityClass the value to use for {@link #getEntityClass()}
   * @param entityIdAttribute the value to u e for {@link #getEntityIdAttribute()}
   * @param entityBeneficiaryIdAttribute the value to use for {@link
   *     #getEntityBeneficiaryIdAttribute()}
   * @param serviceEndAttributeFunction the service end attribute function
   * @param transformer the value to use for {@link #getTransformer()}
   * @param entityLazyAttributes the value to use for {@link #getEntityLazyAttributes()}
   */
  private ClaimTypeV2(
      Class<?> entityClass,
      SingularAttribute<?, ?> entityIdAttribute,
      SingularAttribute<?, ?> entityBeneficiaryIdAttribute,
      Function<Object, LocalDate> serviceEndAttributeFunction,
      ClaimTypeTransformerV2 transformer,
      PluralAttribute<?, ?, ?>... entityLazyAttributes) {
    this.entityClass = entityClass;
    this.entityIdAttribute = entityIdAttribute;
    this.entityBeneficiaryIdAttribute = entityBeneficiaryIdAttribute;
    this.serviceEndAttributeFunction = serviceEndAttributeFunction;
    this.transformer = transformer;
    this.entityLazyAttributes =
        entityLazyAttributes != null
            ? Collections.unmodifiableCollection(Arrays.asList(entityLazyAttributes))
            : Collections.emptyList();
  }

  /**
   * @return the JPA {@link Entity} {@link Class} used to store instances of this {@link
   *     ClaimTypeV2} in the database
   */
  public Class<?> getEntityClass() {
    return entityClass;
  }

  /** @return the JPA {@link Entity} field used as the entity's {@link Id} */
  public SingularAttribute<?, ?> getEntityIdAttribute() {
    return entityIdAttribute;
  }

  /**
   * @return the JPA {@link Entity} field that is a (foreign keyed) reference to {@link
   *     Beneficiary#getBeneficiaryId()}
   */
  public SingularAttribute<?, ?> getEntityBeneficiaryIdAttribute() {
    return entityBeneficiaryIdAttribute;
  }

  /**
   * @return the {@link Function} to use to retrieve the {@link LocalDate} to use for service date
   *     filter
   */
  public Function<Object, LocalDate> getServiceEndAttributeFunction() {
    return serviceEndAttributeFunction;
  }

  /**
   * @return the {@link ClaimTypeTransformerV2} to use to transform the JPA {@link Entity} instances
   *     into FHIR {@link ExplanationOfBenefit} instances
   */
  public ClaimTypeTransformerV2 getTransformer() {
    return transformer;
  }

  /**
   * @return the {@link PluralAttribute}s in the JPA {@link Entity} that are {@link FetchType#LAZY}
   */
  public Collection<PluralAttribute<?, ?, ?>> getEntityLazyAttributes() {
    return entityLazyAttributes;
  }

  /**
   * @param claimTypeText the lower-cased {@link ClaimTypeV2#name()} value to parse back into a
   *     {@link ClaimTypeV2}
   * @return the {@link ClaimTypeV2} represented by the specified {@link String}
   */
  public static Optional<ClaimTypeV2> parse(String claimTypeText) {
    for (ClaimTypeV2 claimType : ClaimTypeV2.values()) {
      if (claimType.name().toLowerCase().equals(claimTypeText)) {
        return Optional.of(claimType);
      }
    }
    return Optional.empty();
  }
}
