package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim;
import gov.hhs.cms.bluebutton.data.model.rif.DMEClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HHAClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim;
import gov.hhs.cms.bluebutton.data.model.rif.HospiceClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.InpatientClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim;
import gov.hhs.cms.bluebutton.data.model.rif.OutpatientClaim_;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent;
import gov.hhs.cms.bluebutton.data.model.rif.PartDEvent_;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim;
import gov.hhs.cms.bluebutton.data.model.rif.SNFClaim_;

/**
 * Enumerates the various Blue Button claim types that are supported by
 * {@link ExplanationOfBenefitResourceProvider}.
 */
public enum ClaimType {
	CARRIER(CarrierClaim.class, CarrierClaim_.claimId, CarrierClaim_.beneficiaryId, CarrierClaimTransformer::transform,
			CarrierClaim_.lines),
	
	DME(DMEClaim.class, DMEClaim_.claimId, DMEClaim_.beneficiaryId, DMEClaimTransformer::transform, DMEClaim_.lines),
	
	HHA(HHAClaim.class, HHAClaim_.claimId, HHAClaim_.beneficiaryId, HHAClaimTransformer::transform, HHAClaim_.lines),

	HOSPICE(HospiceClaim.class, HospiceClaim_.claimId, HospiceClaim_.beneficiaryId, HospiceClaimTransformer::transform,
			HospiceClaim_.lines),

	INPATIENT(InpatientClaim.class, InpatientClaim_.claimId, InpatientClaim_.beneficiaryId,
			InpatientClaimTransformer::transform,
			InpatientClaim_.lines),

	OUTPATIENT(OutpatientClaim.class, OutpatientClaim_.claimId, OutpatientClaim_.beneficiaryId,
			OutpatientClaimTransformer::transform,
			OutpatientClaim_.lines),

	PDE(PartDEvent.class, PartDEvent_.eventId, PartDEvent_.beneficiaryId, PartDEventTransformer::transform),

	SNF(SNFClaim.class, SNFClaim_.claimId, SNFClaim_.beneficiaryId, SNFClaimTransformer::transform, SNFClaim_.lines);

	private final Class<?> entityClass;
	private final SingularAttribute<?, ?> entityIdAttribute;
	private final SingularAttribute<?, String> entityBeneficiaryIdAttribute;
	private final BiFunction<MetricRegistry, Object, ExplanationOfBenefit> transformer;
	private final Collection<PluralAttribute<?, ?, ?>> entityLazyAttributes;

	/**
	 * Enum constant constructor.
	 * 
	 * @param entityClass
	 *            the value to use for {@link #getEntityClass()}
	 * @param entityIdAttribute
	 *            the value to use for {@link #getEntityIdAttribute()}
	 * @param entityBeneficiaryIdAttribute
	 *            the value to use for {@link #getEntityBeneficiaryIdAttribute()}
	 * @param transformer
	 *            the value to use for {@link #getTransformer()}
	 * @param entityLazyAttributes
	 *            the value to use for {@link #getEntityLazyAttributes()}
	 */
	private ClaimType(Class<?> entityClass, SingularAttribute<?, ?> entityIdAttribute,
			SingularAttribute<?, String> entityBeneficiaryIdAttribute,
			BiFunction<MetricRegistry, Object, ExplanationOfBenefit> transformer,
			PluralAttribute<?, ?, ?>... entityLazyAttributes) {
		this.entityClass = entityClass;
		this.entityIdAttribute = entityIdAttribute;
		this.entityBeneficiaryIdAttribute = entityBeneficiaryIdAttribute;
		this.transformer = transformer;
		this.entityLazyAttributes = entityLazyAttributes != null
				? Collections.unmodifiableCollection(Arrays.asList(entityLazyAttributes))
				: Collections.emptyList();
	}

	/**
	 * @return the JPA {@link Entity} {@link Class} used to store instances of
	 *         this {@link ClaimType} in the database
	 */
	public Class<?> getEntityClass() {
		return entityClass;
	}

	/**
	 * @return the JPA {@link Entity} field used as the entity's {@link Id}
	 */
	public SingularAttribute<?, ?> getEntityIdAttribute() {
		return entityIdAttribute;
	}

	/**
	 * @return the JPA {@link Entity} field that is a (foreign keyed) reference to
	 *         {@link Beneficiary#getBeneficiaryId()}
	 */
	public SingularAttribute<?, String> getEntityBeneficiaryIdAttribute() {
		return entityBeneficiaryIdAttribute;
	}

	/**
	 * @return the {@link Function} to use to transform the JPA {@link Entity}
	 *         instances into FHIR {@link ExplanationOfBenefit} instances
	 */
	public BiFunction<MetricRegistry, Object, ExplanationOfBenefit> getTransformer() {
		return transformer;
	}

	/**
	 * @return the {@link PluralAttribute}s in the JPA {@link Entity} that are
	 *         {@link FetchType#LAZY}
	 */
	public Collection<PluralAttribute<?, ?, ?>> getEntityLazyAttributes() {
		return entityLazyAttributes;
	}

	/**
	 * @param claimTypeText
	 *            the lower-cased {@link ClaimType#name()} value to parse back
	 *            into a {@link ClaimType}
	 * @return the {@link ClaimType} represented by the specified {@link String}
	 */
	public static Optional<ClaimType> parse(String claimTypeText) {
		for (ClaimType claimType : ClaimType.values())
			if (claimType.name().toLowerCase().equals(claimTypeText))
				return Optional.of(claimType);
		return Optional.empty();
	}
}
