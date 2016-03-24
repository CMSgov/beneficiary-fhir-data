package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import javax.jdo.AttributeConverter;

/**
 * A JDO {@link AttributeConverter} for {@link ClaimType} fields.
 */
public final class ClaimTypeConverter implements AttributeConverter<ClaimType, String> {
	/**
	 * @see javax.jdo.AttributeConverter#convertToAttribute(java.lang.Object)
	 */
	@Override
	public ClaimType convertToAttribute(String datastoreValue) {
		return ClaimType.getClaimTypeByCode(datastoreValue);
	}

	/**
	 * @see javax.jdo.AttributeConverter#convertToDatastore(java.lang.Object)
	 */
	@Override
	public String convertToDatastore(ClaimType attributeValue) {
		return attributeValue.getCode();
	}
}
