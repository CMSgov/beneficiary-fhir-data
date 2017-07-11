package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseHasExtensions;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Contains shared constants and methods used to transform CCW JPA entities
 * (e.g. {@link Beneficiary}) into FHIR resources (e.g. {@link Patient}).
 */
final class TransformerUtils {
	/**
	 * @param localDate
	 *            the {@link LocalDate} to convert
	 * @return a {@link Date} version of the specified {@link LocalDate}
	 */
	static Date convertToDate(LocalDate localDate) {
		/*
		 * We use the system TZ here to ensure that the date doesn't shift at
		 * all, as FHIR will just use this as an unzoned Date (I think, and if
		 * not, it's almost certainly using the same TZ as this system).
		 */
		return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * <p>
	 * Adds an {@link Extension} to the specified {@link DomainResource}.
	 * {@link Extension#getValue()} will be set to a {@link CodeableConcept}
	 * containing a single {@link Coding}, with the specified system and code.
	 * </p>
	 * <p>
	 * Data Architecture Note: The {@link CodeableConcept} might seem extraneous
	 * -- why not just add the {@link Coding} directly to the {@link Extension}?
	 * The main reason for doing it this way is consistency: this is what FHIR
	 * seems to do everywhere.
	 * </p>
	 * 
	 * @param fhirElement
	 *            the FHIR element to add the {@link Extension} to
	 * @param extensionUrl
	 *            the {@link Extension#getUrl()} to use
	 * @param codingSystem
	 *            the {@link Coding#getSystem()} to use
	 * @param codingCode
	 *            the {@link Coding#getCode()} to use
	 */
	static void addExtensionCoding(IBaseHasExtensions fhirElement, String extensionUrl, String codingSystem,
			String codingCode) {
		IBaseExtension<?, ?> extension = fhirElement.addExtension();
		extension.setUrl(extensionUrl);
		extension.setValue(new Coding());

		CodeableConcept codeableConcept = new CodeableConcept();
		extension.setValue(codeableConcept);

		Coding coding = codeableConcept.addCoding();
		coding.setSystem(codingSystem).setCode(codingCode);
	}
}
