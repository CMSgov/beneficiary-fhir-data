package gov.cms.bfd.server.war.r4.providers;

import static gov.cms.bfd.server.war.commons.CommonTransformerUtils.convertToDate;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.google.common.base.Strings;
import gov.cms.bfd.model.codebook.data.CcwCodebookMissingVariable;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimColumn;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaimColumn;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaimColumn;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimColumn;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.C4BBInstutionalClaimSubtypes;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.LinkBuilder;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.RaceCategory;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants.CurrencyIdentifier;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationDiscriminator;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationStatus;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimInstitutionalCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimPharmacyTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.instance.model.api.IAnyResource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitBalanceComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ExplanationOfBenefitStatus;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ProcedureComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.RemittanceOutcome;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.TotalComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.Use;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.UnsignedIntType;
import org.hl7.fhir.r4.model.codesystems.ExBenefitcategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Contains shared methods used to transform CCW JPA entities (e.g. {@link Beneficiary}) into FHIR
 * resources (e.g. {@link Patient}).
 */
public final class TransformerUtilsV2 {
  private static final Logger LOGGER = LoggerFactory.getLogger(TransformerUtilsV2.class);

  /** Tracks the NPI codes that have already had code lookup failures. */
  private static final String NPI_ORG_DISPLAY_DEFAULT = "UNKNOWN";

  /** Constant used to look up and identify an internal `contained` Organization resource. */
  private static final String PROVIDER_ORG_ID = "provider-org";

  /** Constant for finding a provider org reference. */
  private static final String PROVIDER_ORG_REFERENCE = "#" + PROVIDER_ORG_ID;

  /**
   * Creates a {@link CodeableConcept} from the specified system and code.
   *
   * @param codingSystem the {@link Coding#getSystem()} to use
   * @param codingCode the {@link Coding#getCode()} to use
   * @return a {@link CodeableConcept} with the specified {@link Coding}
   */
  static CodeableConcept createCodeableConcept(String codingSystem, String codingCode) {
    return createCodeableConcept(codingSystem, null, null, codingCode);
  }

  /**
   * Creates a {@link CodeableConcept} from the specified system, display, and code.
   *
   * @param codingSystem the {@link Coding#getSystem()} to use
   * @param codingVersion the {@link Coding#getVersion()} to use
   * @param codingDisplay the {@link Coding#getDisplay()} to use
   * @param codingCode the {@link Coding#getCode()} to use
   * @return a {@link CodeableConcept} with the specified {@link Coding}
   */
  static CodeableConcept createCodeableConcept(
      String codingSystem, String codingVersion, String codingDisplay, String codingCode) {
    CodeableConcept codeableConcept = new CodeableConcept();

    /*
     * Due to meeting CARIN conformance, an additional coding with the ICD-10-Medicare system URL
     * must be added. A coding with the ICD-10 system URL will still be present for backwards compatibility.
     * See JIRA ticket: https://jira.cms.gov/browse/BFD-1895
     */
    if (codingSystem.equals(IcdCode.CODING_SYSTEM_ICD_10)) {
      addCodingToCodeableConcept(
          codeableConcept,
          IcdCode.CODING_SYSTEM_ICD_10_MEDICARE,
          codingVersion,
          codingDisplay,
          codingCode);
    }
    addCodingToCodeableConcept(
        codeableConcept, codingSystem, codingVersion, codingDisplay, codingCode);
    return codeableConcept;
  }

  /**
   * Creates a {@link Coding} from an R4 {@link CodeableConcept}.
   *
   * @param codeableConcept the {@link CodeableConcept} to use
   * @param codingSystem the {@link Coding#getSystem()} to use
   * @param codingVersion the {@link Coding#getVersion()} to use
   * @param codingDisplay the {@link Coding#getDisplay()} to use
   * @param codingCode the {@link Coding#getCode()} to use
   */
  static void addCodingToCodeableConcept(
      CodeableConcept codeableConcept,
      String codingSystem,
      String codingVersion,
      String codingDisplay,
      String codingCode) {
    Coding coding = codeableConcept.addCoding().setSystem(codingSystem).setCode(codingCode);
    if (codingVersion != null) {
      coding.setVersion(codingVersion);
    }
    if (codingDisplay != null) {
      coding.setDisplay(codingDisplay);
    }
  }

  /**
   * Used for creating Identifier references for Organizations and Facilities.
   *
   * @param type the identifier type
   * @param identifierValue the {@link Identifier#getValue()} to use in {@link
   *     Reference#getIdentifier()}
   * @return a {@link Reference} with the specified {@link Identifier}
   */
  static Reference createIdentifierReference(
      C4BBOrganizationIdentifierType type, String identifierValue) {
    return new Reference()
        .setIdentifier(
            new Identifier()
                .setType(createCodeableConcept(type.getSystem(), type.toCode()))
                .setValue(identifierValue))
        .setDisplay(CommonTransformerUtils.retrieveNpiCodeDisplay(identifierValue));
  }

  /**
   * Used for creating Identifier references for Organizations and Facilities.
   *
   * @param type the identifier type
   * @param identifierValue the {@link Identifier#getValue()} to use in {@link
   *     Reference#getIdentifier()}
   * @return a {@link Reference} with the specified {@link Identifier}
   */
  static Reference createIdentifierReference(
      C4BBPractitionerIdentifierType type, String identifierValue) {
    return new Reference()
        .setIdentifier(
            new Identifier()
                .setType(createCodeableConcept(type.getSystem(), type.toCode()))
                .setValue(identifierValue))
        .setDisplay(CommonTransformerUtils.retrieveNpiCodeDisplay(identifierValue));
  }

  /**
   * Used for creating Identifier references for Practitioners.
   *
   * @param type the {@link C4BBPractitionerIdentifierType} to use in {@link
   *     Reference#getIdentifier()}
   * @param value the {@link Identifier#getValue()} to use in {@link Reference#getIdentifier()}
   * @return a {@link Reference} with the specified {@link Identifier}
   */
  static Reference createPractitionerIdentifierReference(
      C4BBPractitionerIdentifierType type, String value) {
    Reference response =
        new Reference()
            .setIdentifier(
                new Identifier()
                    .setType(
                        new CodeableConcept()
                            .addCoding(
                                new Coding(type.getSystem(), type.toCode(), type.getDisplay())))
                    .setValue(value));

    // If this is an NPI perform the extra lookup
    if (C4BBPractitionerIdentifierType.NPI.equals(type)) {
      response.setDisplay(CommonTransformerUtils.retrieveNpiCodeDisplay(value));
    }
    return response;
  }

  /**
   * Used for creating Identifier references for Practitioners.
   *
   * @param type the {@link C4BBPractitionerIdentifierType} to use in {@link
   *     Reference#getIdentifier()}
   * @param value the {@link Identifier#getValue()} to use in {@link Reference#getIdentifier()}
   * @param npiOrgDisplay the npi org Display
   * @return a {@link Reference} with the specified {@link Identifier}
   */
  static Reference createPractitionerIdentifierReferenceWithNpiOrg(
      C4BBPractitionerIdentifierType type, String value, Optional<String> npiOrgDisplay) {
    Reference response =
        new Reference()
            .setIdentifier(
                new Identifier()
                    .setType(
                        new CodeableConcept()
                            .addCoding(
                                new Coding(type.getSystem(), type.toCode(), type.getDisplay())))
                    .setValue(value));

    // If this is an NPI perform the extra lookup
    if (C4BBPractitionerIdentifierType.NPI.equals(type)) {
      response.setDisplay(npiOrgDisplay.orElse(NPI_ORG_DISPLAY_DEFAULT));
    }
    return response;
  }

  /**
   * Creates a reference to the cms organization.
   *
   * @return a Reference to the {@link Organization} for CMS, which will only be valid if
   *     upsertSharedData has been run
   */
  static Reference createReferenceToCms() {
    return new Reference("Organization?name=" + urlEncode(TransformerConstants.COVERAGE_ISSUER));
  }

  /**
   * Checks if the specified combination of system and code exists as a Coding within the supplied
   * {@link CodeableConcept}.
   *
   * @param concept the {@link CodeableConcept} to check
   * @param codingSystem the {@link Coding#getSystem()} to match
   * @param codingCode the {@link Coding#getCode()} to match
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(CodeableConcept concept, String codingSystem, String codingCode) {
    return isCodeInConcept(concept, codingSystem, null, codingCode);
  }

  /**
   * Checks if the specified combination of version, system, and code exists as a Coding within the
   * supplied {@link CodeableConcept}.
   *
   * @param concept the {@link CodeableConcept} to check
   * @param codingSystem the {@link Coding#getSystem()} to match
   * @param codingVersion the coding version
   * @param codingCode the {@link Coding#getCode()} to match
   * @return <code>true</code> if the specified {@link CodeableConcept} contains the specified
   *     {@link Coding}, <code>false</code> if it does not
   */
  static boolean isCodeInConcept(
      CodeableConcept concept, String codingSystem, String codingVersion, String codingCode) {
    return concept.getCoding().stream()
        .anyMatch(
            c -> {
              if (!codingSystem.equals(c.getSystem())) return false;
              if (codingVersion != null && !codingVersion.equals(c.getVersion())) return false;
              return codingCode.equals(c.getCode());
            });
  }

  /**
   * Creates a new identifier.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param identifierValue the value to use for {@link Identifier#getValue()} for the resulting
   *     {@link Identifier}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionIdentifier(
      CcwCodebookInterface ccwVariable, Optional<String> identifierValue) {
    if (!identifierValue.isPresent()) {
      throw new IllegalArgumentException();
    }

    Identifier identifier = createIdentifier(ccwVariable, identifierValue.get());
    String extensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Extension extension = new Extension(extensionUrl, identifier);

    return extension;
  }

  /**
   * Creates a new identifier.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param identifierValue the value to use for {@link Identifier#getValue()} for the resulting
   *     {@link Identifier}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionIdentifier(
      CcwCodebookInterface ccwVariable, String identifierValue) {
    return createExtensionIdentifier(ccwVariable, Optional.of(identifierValue));
  }

  /**
   * Creates a new identifier.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param identifierValue the value to use for {@link Identifier#getValue()} for the resulting
   *     {@link Identifier}
   * @return the output {@link Identifier}
   */
  static Identifier createIdentifier(CcwCodebookInterface ccwVariable, String identifierValue) {
    if (identifierValue == null) {
      throw new IllegalArgumentException();
    }
    Identifier identifier =
        new Identifier()
            .setSystem(CCWUtils.calculateVariableReferenceUrl(ccwVariable))
            .setValue(identifierValue);
    return identifier;
  }

  /**
   * Converts a value from the {@link C4BBSupportingInfoType} enumeration into a {@link Coding}.
   *
   * @param slice the {@link C4BBSupportingInfoType} being mapped
   * @return the resulting {@link Coding}
   */
  static Coding createC4BBSupportingInfoCoding(C4BBSupportingInfoType slice) {
    return new Coding(slice.getSystem(), slice.toCode(), slice.getDisplay());
  }

  /**
   * Helper function to create a {@link CodeableConcept} from a {@link C4BBClaimIdentifierType}.
   * Since this type only has one value this uses a hardcoded value.
   *
   * @return the codeable concept
   */
  static CodeableConcept createC4BBClaimCodeableConcept() {
    return new CodeableConcept()
        .setCoding(
            Arrays.asList(
                new Coding(
                    C4BBClaimIdentifierType.UC.getSystem(),
                    C4BBClaimIdentifierType.UC.toCode(),
                    C4BBClaimIdentifierType.UC.getDisplay())));
  }

  /**
   * Helper function to create the {@link Identifier} for the specified {@link CodeableConcept}.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param identifierValue the value to use for {@link Identifier#getValue()} for the resulting
   *     {@link Identifier}
   * @return the output {@link Identifier}
   */
  static Identifier createClaimIdentifier(
      CcwCodebookInterface ccwVariable, String identifierValue) {
    if (identifierValue == null) {
      throw new IllegalArgumentException();
    }

    Identifier identifier =
        new Identifier()
            .setSystem(CCWUtils.calculateVariableReferenceUrl(ccwVariable))
            .setValue(identifierValue)
            .setType(createC4BBClaimCodeableConcept());

    return identifier;
  }

  /**
   * Creates a claim identifier.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param identifierValue the value to use for {@link Identifier#getValue()} for the resulting
   *     {@link Identifier}
   * @return the output {@link Identifier}
   */
  static Identifier createClaimIdentifier(CcwCodebookInterface ccwVariable, Long identifierValue) {
    if (identifierValue == null) {
      throw new IllegalArgumentException();
    }

    Identifier identifier =
        new Identifier()
            .setSystem(CCWUtils.calculateVariableReferenceUrl(ccwVariable))
            .setValue(identifierValue.toString())
            .setType(createC4BBClaimCodeableConcept());

    return identifier;
  }

  /**
   * Helper function to create the valueDate for the specified {@link Extension}.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param dateYear the value to use for {@link Coding#getCode()} for the resulting {@link Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionDate(CcwCodebookInterface ccwVariable, BigDecimal dateYear) {
    Extension extension = null;
    try {
      String stringDate = String.format("%04d", dateYear.intValue());
      DateType dateYearValue = new DateType(stringDate);
      String extensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
      extension = new Extension(extensionUrl, dateYearValue);
    } catch (DataFormatException e) {
      throw new InvalidRifValueException(
          String.format("Unable to create DateType with reference year: '%s'.", dateYear), e);
    }
    return extension;
  }

  /**
   * Helper function to create the valueDate for the specified {@link Extension}.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param date the value to use for {@link Extension#getValue()} for the resulting {@link
   *     Extension}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionDate(CcwCodebookInterface ccwVariable, LocalDate date) {
    Extension extension = null;
    Objects.requireNonNull(date);
    try {
      String stringDate = date.toString();
      DateType dateValue = new DateType(stringDate);
      String extensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
      extension = new Extension(extensionUrl, dateValue);
    } catch (DataFormatException e) {
      throw new InvalidRifValueException(
          String.format("Unable to create DateType with date: '%s'.", date), e);
    }
    return extension;
  }

  /**
   * Creates an extension for a ccw variable with the specified quantity.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param quantityValue the value to use for {@link Coding#getCode()} for the resulting {@link
   *     Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionQuantity(
      CcwCodebookInterface ccwVariable, Optional<? extends Number> quantityValue) {
    if (!quantityValue.isPresent()) {
      throw new IllegalArgumentException();
    }

    Quantity quantity;
    if (quantityValue.get() instanceof BigDecimal) {
      quantity = new Quantity().setValue((BigDecimal) quantityValue.get());
    } else {
      throw new BadCodeMonkeyException();
    }

    String extensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Extension extension = new Extension(extensionUrl, quantity);

    return extension;
  }

  /**
   * Creates an extension for a ccw variable with the specified quantity.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param quantityValue the value to use for {@link Coding#getCode()} for the resulting {@link
   *     Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to represent the
   *     specified input values
   */
  static Extension createExtensionQuantity(CcwCodebookInterface ccwVariable, Number quantityValue) {
    return createExtensionQuantity(ccwVariable, Optional.of(quantityValue));
  }

  /**
   * Sets the {@link Quantity} fields related to the unit for the amount: {@link
   * Quantity#getSystem()}, {@link Quantity#getCode()}, and {@link Quantity#getUnit()}.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} for the unit coding
   * @param unitCode the value to use for {@link Quantity#getCode()}
   * @param rootResource the root FHIR {@link IAnyResource} that is being mapped
   * @param quantity the {@link Quantity} to modify
   */
  static void setQuantityUnitInfo(
      CcwCodebookInterface ccwVariable,
      Optional<?> unitCode,
      IAnyResource rootResource,
      Quantity quantity) {
    if (!unitCode.isPresent()) {
      return;
    }
    quantity.setSystem(CCWUtils.calculateVariableReferenceUrl(ccwVariable));

    String unitCodeString;
    if (unitCode.get() instanceof String) {
      unitCodeString = (String) unitCode.get();
    } else if (unitCode.get() instanceof Character) {
      unitCodeString = ((Character) unitCode.get()).toString();
    } else {
      throw new IllegalArgumentException();
    }
    quantity.setCode(unitCodeString);

    Optional<String> unit =
        CommonTransformerUtils.calculateCodingDisplay(rootResource, ccwVariable, unitCodeString);
    if (unit.isPresent()) {
      quantity.setUnit(unit.get());
    }
  }

  /**
   * Creates an extension coding for the specified ccw variable and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Extension}
   *     will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()} for the resulting {@link Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to a new {@link
   *     Coding} to represent the specified input values
   */
  static Extension createExtensionCoding(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Optional<?> code) {
    if (!code.isPresent()) {
      throw new IllegalArgumentException();
    }

    Coding coding = createCoding(rootResource, ccwVariable, code.get());
    String extensionUrl = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    Extension extension = new Extension(extensionUrl, coding);

    return extension;
  }

  /**
   * Adds a care team extension to the supplied careTeamComponent if there is not already an
   * extension for the supplied extensionValue and extensionValue is not empty.
   *
   * @param codebookVariable the codebook variable to make the reference url
   * @param extensionValue the value for the extension, typically sourced from the claimLine
   * @param careTeamComponent the care team component to look for the extension in
   * @param eob the eob
   */
  public static void addCareTeamExtension(
      CcwCodebookVariable codebookVariable,
      Optional<?> extensionValue,
      ExplanationOfBenefit.CareTeamComponent careTeamComponent,
      ExplanationOfBenefit eob) {

    // If our extension value is an empty optional or empty/null string, nothing to add
    if (extensionValue.isEmpty() || Strings.isNullOrEmpty(String.valueOf(extensionValue.get()))) {
      return;
    }

    String valueAsString = String.valueOf(extensionValue.get());

    addCareTeamExtension(codebookVariable, valueAsString, careTeamComponent, eob);
  }

  /**
   * Adds a care team extension to the supplied careTeamComponent if there is not already an
   * extension for the supplied extensionValue and extensionValue is not empty.
   *
   * @param codebookVariable the codebook variable to make the reference url
   * @param extensionValue the value for the extension, typically sourced from the claimLine
   * @param careTeamComponent the care team component to look for the extension in
   * @param eob the eob
   */
  public static void addCareTeamExtension(
      CcwCodebookVariable codebookVariable,
      char extensionValue,
      ExplanationOfBenefit.CareTeamComponent careTeamComponent,
      ExplanationOfBenefit eob) {
    // If our extension value is empty/null, nothing to add
    if (Strings.isNullOrEmpty(String.valueOf(extensionValue))) {
      return;
    }

    String valueAsString = String.valueOf(extensionValue);

    addCareTeamExtension(codebookVariable, valueAsString, careTeamComponent, eob);
  }

  /**
   * Adds a care team extension to the supplied careTeamComponent if there is not already an
   * extension for the supplied extensionValue.
   *
   * <p>This method is kept private to dissuade the unpacking of optionals at the caller level; use
   * the methods above for optional/char values so that we can do validation within the util method
   * and keep it out of the calling code. If we have mandatory string values, this can be opened up,
   * but should be noted the values should be passed in as-is from the line, not transformed prior
   * to the call.
   *
   * @param codebookVariable the codebook variable to make the reference url
   * @param extensionValue the value for the extension, typically sourced from the claimLine
   * @param careTeamComponent the care team component to look for the extension in
   * @param eob the eob
   */
  private static void addCareTeamExtension(
      CcwCodebookVariable codebookVariable,
      String extensionValue,
      ExplanationOfBenefit.CareTeamComponent careTeamComponent,
      ExplanationOfBenefit eob) {
    String referenceUrl = getReferenceUrl(codebookVariable);
    boolean hasExtension =
        careTeamHasMatchingExtension(careTeamComponent, referenceUrl, extensionValue);

    // If the extension doesnt exist, add it
    if (!hasExtension) {
      careTeamComponent.addExtension(createExtensionCoding(eob, codebookVariable, extensionValue));
    }
  }

  /**
   * Creates an extension coding for the specified ccw variable, year-month, and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Extension}
   *     will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param yearMonth the year month
   * @param code the value to use for {@link Coding#getCode()} for the resulting {@link Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to a new {@link
   *     Coding} to represent the specified input values
   */
  static Extension createExtensionCoding(
      IAnyResource rootResource,
      CcwCodebookInterface ccwVariable,
      String yearMonth,
      Optional<?> code) {
    if (!code.isPresent()) {
      throw new IllegalArgumentException();
    }
    Coding coding = createCoding(rootResource, ccwVariable, yearMonth, code.get());
    String extensionUrl =
        String.format("%s/%s", CCWUtils.calculateVariableReferenceUrl(ccwVariable), yearMonth);
    Extension extension = new Extension(extensionUrl, coding);

    return extension;
  }

  /**
   * Creates an extension coding for the specified ccw variable and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Extension}
   *     will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()} for the resulting {@link Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to a new {@link
   *     Coding} to represent the specified input values
   */
  static Extension createExtensionCoding(
      IAnyResource rootResource, Optional<CcwCodebookInterface> ccwVariable, Optional<?> code) {
    if (!ccwVariable.isPresent()) {
      throw new IllegalArgumentException();
    }
    return createExtensionCoding(rootResource, ccwVariable.get(), code);
  }

  /**
   * Creates an extension coding for the specified ccw variable and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Extension}
   *     will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()} for the resulting {@link Coding}
   * @return the output {@link Extension}, with {@link Extension#getValue()} set to a new {@link
   *     Coding} to represent the specified input values
   */
  static Extension createExtensionCoding(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Object code) {
    // Jumping through hoops to cope with overloaded method:
    Optional<?> codeOptional = code instanceof Optional ? (Optional<?>) code : Optional.of(code);
    return createExtensionCoding(rootResource, ccwVariable, codeOptional);
  }

  /**
   * Creates a {@link CodeableConcept} from the specified ccw variable and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link
   *     CodeableConcept} will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()} for the resulting (single) {@link
   *     Coding}, wrapped within the resulting {@link CodeableConcept}
   * @return the output {@link CodeableConcept} for the specified input values
   */
  static CodeableConcept createCodeableConcept(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Optional<?> code) {
    if (code.isEmpty()) {
      throw new IllegalArgumentException();
    }

    Coding coding = createCoding(rootResource, ccwVariable, code.get());
    CodeableConcept concept = new CodeableConcept();
    concept.addCoding(coding);

    return concept;
  }

  /**
   * Adds a qualification {@link CodeableConcept} to the given careTeam component, if the input code
   * optional is not empty. If the code is empty, returns with no effect. Can safely be called to
   * add qualification only if the value is present.
   *
   * @param careTeam the care team to add the
   * @param rootResource the root resource to use for the coding
   * @param ccwVariable the ccw variable to use for the coding
   * @param code an optional to create the {@link CodeableConcept} from; if empty, method returns
   *     with no action taken
   */
  static void addCareTeamQualification(
      CareTeamComponent careTeam,
      IAnyResource rootResource,
      CcwCodebookInterface ccwVariable,
      Optional<?> code) {
    // While the original code was written in such a way that implies this optional wont be empty,
    // its still an optional, so dont bother adding anything if it happens to be empty
    if (code.isEmpty()) {
      return;
    }

    Coding coding = createCoding(rootResource, ccwVariable, code.get());
    CodeableConcept concept = new CodeableConcept();
    concept.addCoding(coding);

    careTeam.setQualification(concept);
  }

  /**
   * Creates a {@link CodeableConcept} from the specified ccw variable and code.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link
   *     CodeableConcept} will be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()} for the resulting (single) {@link
   *     Coding}, wrapped within the resulting {@link CodeableConcept}
   * @return the output {@link CodeableConcept} for the specified input values
   */
  static CodeableConcept createCodeableConcept(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Object code) {
    // Jumping through hoops to cope with overloaded method:
    Optional<?> codeOptional = code instanceof Optional ? (Optional<?>) code : Optional.of(code);
    return createCodeableConcept(rootResource, ccwVariable, codeOptional);
  }

  /**
   * Unlike {@link #createCodeableConcept(IAnyResource, CcwCodebookInterface, Optional)}, this
   * method creates a {@link CodeableConcept} that's intended for use as a field ID/discriminator:
   * the {@link Variable#getId()} will be used for the {@link Coding#getCode()}, rather than the
   * {@link Coding#getSystem()}.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link
   *     CodeableConcept} will be contained in
   * @param codingSystem the {@link Coding#getSystem()} to use
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @return the output {@link CodeableConcept} for the specified input values
   */
  private static CodeableConcept createCodeableConceptForFieldId(
      IAnyResource rootResource, String codingSystem, CcwCodebookInterface ccwVariable) {
    String code = CCWUtils.calculateVariableReferenceUrl(ccwVariable);

    Coding carinCoding =
        new Coding()
            .setCode("info")
            .setSystem(TransformerConstants.CARIN_SUPPORTING_INFO_TYPE)
            .setDisplay("Information");
    Coding cmsBBcoding = new Coding(codingSystem, code, ccwVariable.getVariable().getLabel());

    CodeableConcept categoryCodeableConcept = new CodeableConcept();
    categoryCodeableConcept.addCoding(carinCoding);
    categoryCodeableConcept.addCoding(cmsBBcoding);

    return categoryCodeableConcept;
  }

  /**
   * Creates a coding.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Coding} will
   *     be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()}
   * @return the output {@link Coding} for the specified input values
   */
  public static Coding createCoding(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Object code) {
    /*
     * The code parameter is an Object to avoid needing multiple copies of this and
     * related methods.
     * This if-else block is the price to be paid for that, though.
     */
    String codeString;
    if (code instanceof Character) {
      codeString = ((Character) code).toString();
    } else if (code instanceof String) {
      codeString = code.toString().trim();
    } else {
      throw new BadCodeMonkeyException("Unsupported: " + code);
    }
    String system = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    String display =
        (ccwVariable.getVariable().getValueGroups().isPresent())
            ? CommonTransformerUtils.calculateCodingDisplay(rootResource, ccwVariable, codeString)
                .orElse(null)
            : null;

    return new Coding(system, codeString, display);
  }

  /**
   * Creates a coding.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Coding} will
   *     be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param code the value to use for {@link Coding#getCode()}
   * @return the output {@link Coding} for the specified input values
   */
  public static Coding createCoding(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, Optional<?> code) {
    return createCoding(rootResource, ccwVariable, code.get());
  }

  /**
   * Creates an adjudication category codeable concept.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @return the {@link AdjudicationComponent#getCategory()} {@link CodeableConcept} to use for the
   *     specified {@link CcwCodebookInterface}
   */
  static CodeableConcept createAdjudicationCategory(CcwCodebookInterface ccwVariable) {
    /*
     * Adjudication.category is mapped a bit differently than other Codings/CodeableConcepts: they
     * all share the same Coding.system and use the CcwCodebookInterface reference URL as their
     * Coding.code. This looks weird, but makes it easy for API developers to find more information
     * about what the specific adjudication they're looking at means.
     */

    String conceptCode = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    CodeableConcept categoryConcept =
        createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, conceptCode);
    categoryConcept.getCodingFirstRep().setDisplay(ccwVariable.getVariable().getLabel());
    return categoryConcept;
  }

  /**
   * Creates an adjudication category codeable concept.
   *
   * @param ccwVariable the {@link CcwCodebookInterface} being mapped
   * @param carinAdjuCode the carin adju code
   * @param carinAdjuCodeDisplay the carin adju code display
   * @return the {@link AdjudicationComponent#getCategory()} {@link CodeableConcept} to use for the
   *     specified {@link CcwCodebookInterface}
   */
  static CodeableConcept createAdjudicationCategory(
      CcwCodebookInterface ccwVariable, String carinAdjuCode, String carinAdjuCodeDisplay) {
    /*
     * Adjudication.category is mapped a bit differently than other Codings/CodeableConcepts: they
     * all share the same Coding.system and use the CcwCodebookInterface reference URL as their
     * Coding.code. This looks weird, but makes it easy for API developers to find more information
     * about what the specific adjudication they're looking at means.
     */

    String conceptCode = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    CodeableConcept categoryConcept =
        createCodeableConcept(TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY, conceptCode);
    categoryConcept.getCodingFirstRep().setDisplay(ccwVariable.getVariable().getLabel());

    categoryConcept
        .addCoding()
        .setSystem(C4BBAdjudication.SUBMITTED.getSystem())
        .setCode(carinAdjuCode)
        .setDisplay(carinAdjuCodeDisplay);

    return categoryConcept;
  }

  /**
   * Helper function that finds or creates an {@link Address} object from `item.location`.
   *
   * @param item The {@ItemComponent} to find the {@link Address} in
   * @return The Address
   */
  private static Address getAddress(ItemComponent item) {
    // Create one if we don't have it
    if (!item.hasLocation()) {
      item.setLocation(new Address());
    }

    // We are assuming all locations are addresses
    if (!(item.getLocation() instanceof Address)) {
      throw new BadCodeMonkeyException();
    }

    return (Address) item.getLocation();
  }

  /**
   * Optionally adds State to a new or existing {@link Address} in an {@ItemComponent}.
   *
   * @param item {@ItemComponent} to add the State code to
   * @param state State code to add
   */
  static void addLocationState(ItemComponent item, Optional<String> state) {
    state.ifPresent(s -> getAddress(item).setState(s));
  }

  /**
   * Adds State to a new or existing {@link Address} in an {@ItemComponent}.
   *
   * @param item {@ItemComponent} to add the State code to
   * @param state State code to add
   */
  static void addLocationState(ItemComponent item, String state) {
    addLocationState(item, Optional.ofNullable(state));
  }

  /**
   * Optionally adds a ZIP code to a new or existing {@link Address} in an {@ItemComponent}.
   *
   * @param item {@ItemComponent} to add the State code to
   * @param zip The ZIP code to add
   */
  static void addLocationZipCode(ItemComponent item, Optional<String> zip) {
    zip.ifPresent(z -> getAddress(item).setPostalCode(z));
  }

  /**
   * Optionally adds an {@link AdjudicationComponent} to an {@link ItemComponent#getAdjudication()}.
   *
   * @param item {@link ItemComponent} to add the {@link AdjudicationComponent} to
   * @param adjudication Optional {@link AdjudicationComponent}
   */
  static void addAdjudication(ItemComponent item, Optional<AdjudicationComponent> adjudication) {
    adjudication.ifPresent(adj -> item.addAdjudication(adj));
  }

  /**
   * Optionally adds an {@link AdjudicationComponent} to an {@link
   * ExplanationOfBenefit#getAdjudication()}.
   *
   * @param eob {@link ExplanationOfBenefit} to add the {@link AdjudicationComponent} to
   * @param adjudication Optional {@link AdjudicationComponent}
   */
  static void addAdjudication(
      ExplanationOfBenefit eob, Optional<AdjudicationComponent> adjudication) {
    adjudication.ifPresent(adj -> eob.addAdjudication(adj));
  }

  /**
   * Optionally adds an {@link TotalComponent} to an {@link ExplanationOfBenefit#getTotal()}.
   *
   * @param eob {@link ExplanationOfBenefit} to add the {@link TotalComponent} to
   * @param total Optional {@link TotalComponent}
   */
  static void addTotal(ExplanationOfBenefit eob, Optional<TotalComponent> total) {
    total.ifPresent(t -> eob.addTotal(t));
  }

  /**
   * Optionally adds an {@link SupportingInformationComponent} to an {@link
   * ExplanationOfBenefit#getSupportingInfo()}.
   *
   * @param eob {@link ExplanationOfBenefit} to add the {@link TotalComponent} to
   * @param info the info
   */
  static void addInformation(
      ExplanationOfBenefit eob, Optional<SupportingInformationComponent> info) {
    info.ifPresent(i -> eob.addSupportingInfo(i));
  }

  /**
   * Optionally adds a National Drug Code (NDC) to the `item.productOrService` field.
   *
   * <p>This mapping is used for some EOB types but not all.
   *
   * @param item The {@link ItemComponent} to add the NDC to
   * @param nationalDrugCode The NDC value to add
   * @param drugCode the drug code
   */
  static void addNationalDrugCode(
      ItemComponent item, Optional<String> nationalDrugCode, String drugCode) {
    nationalDrugCode.ifPresent(
        code ->
            item.getProductOrService()
                .addExtension()
                .setUrl(TransformerConstants.CODING_NDC)
                .setValue(new Coding(TransformerConstants.CODING_NDC, code, drugCode)));
  }

  /**
   * Creates a C4BB Adjudication `adjudicationamounttype` {@link CodeableConcept} slice for use in
   * multiple places.
   *
   * <p>Also adds the CCW variable as an additional coding.
   *
   * @param ccwVariable The CCW Variable that represents what the amount is
   * @param code The C4BBAdjudication code that represents this amount
   * @return The created {@link AdjudicationComponent}
   */
  private static CodeableConcept createAdjudicationAmtSliceCategory(
      CcwCodebookInterface ccwVariable, C4BBAdjudication code) {
    return new CodeableConcept()
        // Indicate the required coding for CC4BB adjudicationamounttype slice
        .addCoding(new Coding(code.getSystem(), code.toCode(), code.getDisplay()))
        // Indicate the correct CCW variable
        .addCoding(
            new Coding(
                TransformerConstants.CODING_CCW_ADJUDICATION_CATEGORY,
                CCWUtils.calculateVariableReferenceUrl(ccwVariable),
                ccwVariable.getVariable().getLabel()));
  }

  /**
   * Creates a C4BB Adjudication `adjudicationamounttype` {@link CodeableConcept} slice for use in
   * multiple places. Does not add/require the CCW code, to keep in CARIN compliance in spots where
   * having multiple codes is illegal.
   *
   * @param code The C4BBAdjudication code that represents this amount
   * @return The created {@link AdjudicationComponent}
   */
  private static CodeableConcept createAdjudicationAmtSliceCategory(C4BBAdjudication code) {
    return new CodeableConcept()
        // Indicate the required coding for CC4BB adjudicationamounttype slice
        .addCoding(new Coding(code.getSystem(), code.toCode(), code.getDisplay()));
  }

  /**
   * Creates a C4BB Adjudication Status `C4BBPayerAdjudicationStatus` {@link CodeableConcept} slice
   * for use in multiple places. Does not add/require the CCW code, to keep in CARIN compliance in
   * spots where having multiple codes is illegal.
   *
   * @param code The C4BBAdjudicationStatus code that represents this amount
   * @return The created {@link AdjudicationComponent}
   */
  private static CodeableConcept createAdjudicationStatusAmtSliceCategory(
      C4BBAdjudicationStatus code) {
    return new CodeableConcept()
        .addCoding(new Coding(code.getSystem(), code.toCode(), code.getDisplay()));
  }

  /**
   * Optionally Creates an `adjudicationamounttype` {@link AdjudicationComponent} slice.
   *
   * @param ccwVariable The CCW Variable that represents what the amount is
   * @param code The C4BBAdjudication code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link AdjudicationComponent}
   */
  static Optional<AdjudicationComponent> createAdjudicationAmtSlice(
      CcwCodebookInterface ccwVariable, C4BBAdjudication code, Optional<BigDecimal> amount) {
    return amount.map(
        amt ->
            new AdjudicationComponent()
                .setCategory(createAdjudicationAmtSliceCategory(ccwVariable, code))
                .setAmount(createMoney(amt)));
  }

  /**
   * Optionally Creates an `adjudicationamounttype` {@link AdjudicationComponent} slice.
   *
   * @param ccwVariable The CCW Variable that represents what the amount is
   * @param code The C4BBAdjudication code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link AdjudicationComponent}
   */
  static Optional<AdjudicationComponent> createAdjudicationAmtSlice(
      CcwCodebookInterface ccwVariable, C4BBAdjudication code, BigDecimal amount) {
    return createAdjudicationAmtSlice(ccwVariable, code, Optional.of(amount));
  }

  /**
   * Optionally Creates an `denialreason` {@link AdjudicationComponent} slice.
   *
   * @param eob The base {@link ExplanationOfBenefit} resource
   * @param ccwVariable The CCW Variable that represents what the reason is
   * @param reasonCode The coded denial reason
   * @return The created {@link AdjudicationComponent}
   */
  static Optional<AdjudicationComponent> createAdjudicationDenialReasonSlice(
      ExplanationOfBenefit eob, CcwCodebookInterface ccwVariable, Optional<String> reasonCode) {
    return reasonCode.map(
        reason ->
            new AdjudicationComponent()
                // Set category for `denialreason` slice
                .setCategory(
                    new CodeableConcept()
                        .setCoding(
                            Arrays.asList(
                                new Coding(
                                    C4BBAdjudicationDiscriminator.DENIAL_REASON.getSystem(),
                                    C4BBAdjudicationDiscriminator.DENIAL_REASON.toCode(),
                                    C4BBAdjudicationDiscriminator.DENIAL_REASON.getDisplay()))))
                // Set BB coding for Reason
                .setReason(createCodeableConcept(eob, ccwVariable, reason)));
  }

  /**
   * Optionally Creates an `denialreason` {@link AdjudicationComponent} slice.
   *
   * @param eob The base {@link ExplanationOfBenefit} resource
   * @param ccwVariable The CCW Variable that represents what the reason is
   * @param reasonCode The coded denial reason
   * @return The created {@link AdjudicationComponent}
   */
  static Optional<AdjudicationComponent> createAdjudicationDenialReasonSlice(
      ExplanationOfBenefit eob, CcwCodebookInterface ccwVariable, String reasonCode) {
    return createAdjudicationDenialReasonSlice(eob, ccwVariable, Optional.of(reasonCode));
  }

  /**
   * Optionally Creates an `adjudicationamounttype` {@link TotalComponent} slice. This looks similar
   * to the code to generate the {@link AdjudicationComponent} slice of the same name, but
   * unfortunately can't be reused because they are different types.
   *
   * <p>Also adds the CCW variable as an additional coding.
   *
   * @param eob The base {@link ExplanationOfBenefit} resource
   * @param ccwVariable The CCW Variable that represents what the reason is
   * @param code The C4BBAdjudication code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link TotalComponent}
   */
  static Optional<TotalComponent> createTotalAdjudicationAmountSlice(
      ExplanationOfBenefit eob,
      CcwCodebookInterface ccwVariable,
      C4BBAdjudication code,
      Optional<BigDecimal> amount) {
    return amount.map(
        amt ->
            new TotalComponent()
                .setCategory(createAdjudicationAmtSliceCategory(ccwVariable, code))
                .setAmount(createMoney(amount)));
  }

  /**
   * Optionally Creates an `adjudicationamounttype` {@link TotalComponent} slice. This looks similar
   * to the code to generate the {@link AdjudicationComponent} slice of the same name, but
   * unfortunately can't be reused because they are different types.
   *
   * <p>Does not add/require the CCW variable as an additional coding, for situations where
   * including it breaks CARIN compliance.
   *
   * @param code The C4BBAdjudication code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link TotalComponent}
   */
  static Optional<TotalComponent> createTotalAdjudicationAmountSlice(
      C4BBAdjudication code, Optional<BigDecimal> amount) {
    return amount.map(
        amt ->
            new TotalComponent()
                .setCategory(createAdjudicationAmtSliceCategory(code))
                .setAmount(createMoney(amount)));
  }

  /**
   * Optionally Creates an `adjudication status amount` {@link TotalComponent} slice.
   *
   * @param code The C4BBAdjudicationStatus code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link TotalComponent}
   */
  public static Optional<TotalComponent> createTotalAdjudicationStatusAmountSlice(
      C4BBAdjudicationStatus code, Optional<BigDecimal> amount) {
    return amount.map(
        amt ->
            new TotalComponent()
                .setCategory(createAdjudicationStatusAmtSliceCategory(code))
                .setAmount(createMoney(amount)));
  }

  /**
   * Optionally creates an `admissionperiod` {@link SupportingInformationComponent} slice.
   *
   * @param eob the eob
   * @param periodStart Period start
   * @param periodEnd Period end
   * @return The created {@link SupportingInformationComponent}
   */
  static Optional<SupportingInformationComponent> createInformationAdmPeriodSlice(
      ExplanationOfBenefit eob, Optional<LocalDate> periodStart, Optional<LocalDate> periodEnd) {
    // Create a range if we can
    if (periodStart.isPresent() || periodEnd.isPresent()) {
      validatePeriodDates(periodStart, periodEnd);

      // Create the period
      Period period = new Period();
      periodStart.ifPresent(
          start -> period.setStart(convertToDate(start), TemporalPrecisionEnum.DAY));
      periodEnd.ifPresent(end -> period.setEnd(convertToDate(end), TemporalPrecisionEnum.DAY));

      int maxSequence =
          eob.getSupportingInfo().stream().mapToInt(i -> i.getSequence()).max().orElse(0);

      // Create the SupportingInfo element
      return Optional.of(
          new SupportingInformationComponent()
              .setSequence(maxSequence + 1)
              .setCategory(
                  new CodeableConcept()
                      .addCoding(
                          createC4BBSupportingInfoCoding(C4BBSupportingInfoType.ADMISSION_PERIOD)))
              .setTiming(period));
    } else {
      return Optional.empty();
    }
  }

  /**
   * Optionally creates an `clmrecvdate` {@link SupportingInformationComponent} slice.
   *
   * @param eob the eob
   * @param ccwVariable the ccw variable
   * @param date Claim received date
   * @return The created {@link SupportingInformationComponent}
   */
  static Optional<SupportingInformationComponent> createInformationRecievedDateSlice(
      ExplanationOfBenefit eob, CcwCodebookInterface ccwVariable, Optional<LocalDate> date) {
    return date.map(
        d -> {
          int maxSequence =
              eob.getSupportingInfo().stream().mapToInt(i -> i.getSequence()).max().orElse(0);

          // Create the SupportingInfo element
          return new SupportingInformationComponent()
              .setSequence(maxSequence + 1)
              .setCategory(
                  new CodeableConcept()
                      .addCoding(
                          createC4BBSupportingInfoCoding(C4BBSupportingInfoType.RECEIVED_DATE))
                      .addCoding(
                          new Coding(
                              TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY,
                              CCWUtils.calculateVariableReferenceUrl(ccwVariable),
                              ccwVariable.getVariable().getLabel())))
              .setTiming(new DateType(convertToDate(d)));
        });
  }

  /**
   * Optionally Creates an `adjudicationamounttype` {@link TotalComponent} slice. This looks similar
   * to the code to generate the {@link AdjudicationComponent} slice of the same name, but
   * unfortunately can't be reused because they are different types.
   *
   * @param eob The base {@link ExplanationOfBenefit} resource
   * @param ccwVariable The CCW Variable that represents what the reason is
   * @param code The C4BBAdjudication code that represents this amount
   * @param amount A dollar amount
   * @return The created {@link TotalComponent}
   */
  static Optional<TotalComponent> createTotalAdjudicationAmountSlice(
      ExplanationOfBenefit eob,
      CcwCodebookInterface ccwVariable,
      C4BBAdjudication code,
      BigDecimal amount) {
    return createTotalAdjudicationAmountSlice(eob, ccwVariable, code, Optional.of(amount));
  }

  /**
   * Creates a new {@link Reference} from the specified patient id.
   *
   * @param patientId the bene id value for the beneficiary to match
   * @return a {@link Reference} to the {@link Patient} resource that matches the specified
   *     parameters
   */
  static Reference referencePatient(Long patientId) {
    return new Reference(String.format("Patient/%d", patientId));
  }

  /**
   * Creates a new {@link Reference} from the specified {@link Beneficiary}.
   *
   * @param beneficiary the {@link Beneficiary} to generate a {@link Patient} {@link Reference} for
   * @return a {@link Reference} to the {@link Patient} resource for the specified {@link
   *     Beneficiary}
   */
  static Reference referencePatient(Beneficiary beneficiary) {
    return referencePatient(beneficiary.getBeneficiaryId());
  }

  /**
   * Sets the period end.
   *
   * @param period the {@link Period} to adjust
   * @param date the {@link LocalDate} to set the {@link Period#getEnd()} value with/to
   */
  static void setPeriodEnd(Period period, Optional<LocalDate> date) {
    date.ifPresent(value -> setPeriodEnd(period, value));
  }

  /**
   * Sets the period end.
   *
   * @param period the {@link Period} to adjust
   * @param date the {@link LocalDate} to set the {@link Period#getEnd()} value with/to
   */
  static void setPeriodEnd(Period period, LocalDate date) {
    period.setEnd(convertToDate(date), TemporalPrecisionEnum.DAY);
  }

  /**
   * Sets the period start.
   *
   * @param period the {@link Period} to adjust
   * @param date the {@link LocalDate} to set the {@link Period#getStart()} value with/to
   */
  static void setPeriodStart(Period period, Optional<LocalDate> date) {
    date.ifPresent(value -> setPeriodStart(period, value));
  }

  /**
   * Sets the period start.
   *
   * @param period the {@link Period} to adjust
   * @param date the {@link LocalDate} to set the {@link Period#getStart()} value with/to
   */
  static void setPeriodStart(Period period, LocalDate date) {
    period.setStart(convertToDate(date), TemporalPrecisionEnum.DAY);
  }

  /**
   * Creates a url-encoded version of the specified text.
   *
   * @param urlText the URL or URL portion to be encoded
   * @return a URL-encoded version of the specified text
   */
  static String urlEncode(String urlText) {
    try {
      return URLEncoder.encode(urlText, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new BadCodeMonkeyException(e);
    }
  }

  /**
   * Validate the from/thru dates to ensure the from date is before or the same as the thru date.
   *
   * @param dateFrom start date {@link LocalDate}
   * @param dateThrough through date {@link LocalDate} to verify
   */
  static void validatePeriodDates(LocalDate dateFrom, LocalDate dateThrough) {
    if (dateFrom == null || dateThrough == null) {
      return;
    }
    // FIXME see CBBD-236 (ETL service fails on some Hospice claims "From
    // date is after the Through Date")
    // We are seeing this scenario in production where the from date is
    // after the through date so we are just logging the error for now.
    if (dateFrom.isAfter(dateThrough)) {
      LOGGER.debug(
          String.format(
              "Error - From Date '%s' is after the Through Date '%s'", dateFrom, dateThrough));
    }
  }

  /**
   * Validate the from/thru dates to ensure the from date is before or the same as the thru date.
   *
   * @param dateFrom the date from
   * @param dateThrough the date through
   */
  static void validatePeriodDates(Optional<LocalDate> dateFrom, Optional<LocalDate> dateThrough) {
    if (dateFrom.isPresent() && dateThrough.isPresent()) {
      validatePeriodDates(dateFrom.get(), dateThrough.get());
    }
  }

  /**
   * Create a bundle from the entire search result.
   *
   * @param paging contains the {@link OffsetLinkBuilder} information
   * @param resources a list of {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, of which a portion or all will be added to the bundle based on the paging values
   * @param transactionTime date for the bundle
   * @return a {@link Bundle} of either {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, which may contain multiple matching resources, or may also be empty
   */
  public static Bundle createBundle(
      OffsetLinkBuilder paging, List<IBaseResource> resources, Instant transactionTime) {
    Bundle bundle = new Bundle();
    List<IBaseResource> resourcesSubList = resources;
    if (paging.isPagingRequested()) {
      /*
       * FIXME: Due to a bug in HAPI-FHIR described here
       * https://github.com/jamesagnew/hapi-fhir/issues/1074 paging for count=0 is not working
       * correctly.
       * TODO: Above bug should be fixed as of 1/1/20; re-investigate this
       */
      // If we have no resources, don't sublist anything since it causes indexing issues
      if (resources.size() > 0) {
        int endIndex = Math.min(paging.getStartIndex() + paging.getPageSize(), resources.size());
        // Throw a 400 if startIndex >= results, since we cant sublist with these values
        validateStartIndexSize(paging.getStartIndex(), resources.size());
        resourcesSubList = resources.subList(paging.getStartIndex(), endIndex);
      }
      bundle = TransformerUtilsV2.addResourcesToBundle(bundle, resourcesSubList);
      paging.setTotal(resources.size()).addLinks(bundle);
      // Add number of paginated resources to MDC logs
      LoggingUtils.logResourceCountToMdc(resourcesSubList.size());
    } else {
      bundle = TransformerUtilsV2.addResourcesToBundle(bundle, resources);

      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(resources.size());
    }

    /*
     * Dev Note: the Bundle's lastUpdated timestamp is the known last update time for the whole
     * database. Because the filterManager's tracking of this timestamp is lazily updated for
     * performance reason, the resources of the bundle may be after the filter manager's version of
     * the timestamp.
     */
    Instant maxBundleDate =
        resourcesSubList.stream()
            .map(r -> r.getMeta().getLastUpdated().toInstant())
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(transactionTime);
    bundle
        .getMeta()
        .setLastUpdated(
            transactionTime.isAfter(maxBundleDate)
                ? Date.from(transactionTime)
                : Date.from(maxBundleDate));
    bundle.setTotal(resources.size());
    return bundle;
  }

  /**
   * Validate the start index size is less than the total number of resources. If startIndex is
   * greater than or equal to the number of resources, throws an InvalidRequestException which will
   * bubble up and create a 400 error at the REST level via HAPI-FHIR.
   *
   * @param startIndex the start index from the paging
   * @param numResources the number of resources in the response
   */
  public static void validateStartIndexSize(int startIndex, int numResources) {
    // Throw a 400 if startIndex >= results, since we cant sublist with these values
    if (startIndex >= numResources) {
      throw new InvalidRequestException(
          String.format(
              "Value for startIndex (%d) must be less than than result size (%d)",
              startIndex, numResources));
    }
  }

  /**
   * Create a bundle from the entire search result.
   *
   * @param resources a list of {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, all of which will be added to the bundle
   * @param paging contains the {@link LinkBuilder} information to add to the bundle
   * @param transactionTime date for the bundle
   * @return a {@link Bundle} of either {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, which may contain multiple matching resources, or may also be empty
   */
  public static Bundle createBundle(
      List<IBaseResource> resources, LinkBuilder paging, Instant transactionTime) {
    Bundle bundle = new Bundle();
    TransformerUtilsV2.addResourcesToBundle(bundle, resources);
    paging.addLinks(bundle);
    bundle.setTotalElement(
        paging.isPagingRequested() ? new UnsignedIntType() : new UnsignedIntType(resources.size()));

    /*
     * Dev Note: the Bundle's lastUpdated timestamp is the known last update time for the whole
     * database. Because the filterManager's tracking of this timestamp is lazily updated for
     * performance reason, the resources of the bundle may be after the filter manager's version of
     * the timestamp.
     */
    Instant maxBundleDate =
        resources.stream()
            .map(r -> r.getMeta().getLastUpdated().toInstant())
            .filter(Objects::nonNull)
            .max(Instant::compareTo)
            .orElse(transactionTime);
    bundle
        .getMeta()
        .setLastUpdated(
            transactionTime.isAfter(maxBundleDate)
                ? Date.from(transactionTime)
                : Date.from(maxBundleDate));

    // Add number of resources to MDC logs
    LoggingUtils.logResourceCountToMdc(bundle.getTotal());

    return bundle;
  }

  /**
   * Adds resources to the specified bundle.
   *
   * @param bundle a {@link Bundle} to add the list of {@link ExplanationOfBenefit} resources to.
   * @param resources a list of either {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, of which a portion will be added to the bundle based on the paging values
   * @return a {@link Bundle} of {@link ExplanationOfBenefit}s, {@link Coverage}s, or {@link
   *     Patient}s, which may contain multiple matching resources, or may also be empty
   */
  public static Bundle addResourcesToBundle(Bundle bundle, List<IBaseResource> resources) {
    for (IBaseResource res : resources) {
      BundleEntryComponent entry = bundle.addEntry();
      entry.setResource((Resource) res);
    }
    return bundle;
  }

  /**
   * Creates a currency identifier extension.
   *
   * @param currencyIdentifier the {@link CurrencyIdentifier} indicating the currency of an {@link
   *     Identifier}
   * @return an {@link Extension} describing the currency of an {@link Identifier}
   */
  public static Extension createIdentifierCurrencyExtension(CurrencyIdentifier currencyIdentifier) {
    String system = TransformerConstants.CODING_SYSTEM_IDENTIFIER_CURRENCY;
    String code = "historic";
    String display = "Historic";
    if (currencyIdentifier.equals(CurrencyIdentifier.CURRENT)) {
      code = "current";
      display = "Current";
    }

    Coding currentValueCoding = new Coding(system, code, display);
    Extension currencyIdentifierExtension =
        new Extension(TransformerConstants.CODING_SYSTEM_IDENTIFIER_CURRENCY, currentValueCoding);

    return currencyIdentifierExtension;
  }

  /**
   * Sets the lastUpdated value in the resource.
   *
   * @param resource is the FHIR resource to set lastUpdate
   * @param lastUpdated is the lastUpdated value set. If not present, set the fallback lastUpdated.
   */
  public static void setLastUpdated(IAnyResource resource, Optional<Instant> lastUpdated) {
    resource
        .getMeta()
        .setLastUpdated(Date.from(lastUpdated.orElse(TransformerConstants.FALLBACK_LAST_UPDATED)));
  }

  /**
   * Sets the lastUpdated value in the resource if the passed in value is later than the current
   * value.
   *
   * @param resource is the FHIR resource to update
   * @param lastUpdated is the lastUpdated value from the entity
   */
  public static void updateMaxLastUpdated(IAnyResource resource, Optional<Instant> lastUpdated) {
    lastUpdated.ifPresent(
        newDate -> {
          Instant currentDate =
              resource.getMeta().getLastUpdated() != null
                  ? resource.getMeta().getLastUpdated().toInstant()
                  : null;
          if (currentDate != null && newDate.isAfter(currentDate)) {
            resource.getMeta().setLastUpdated(Date.from(newDate));
          }
        });
  }

  /**
   * Work around for https://github.com/jamesagnew/hapi-fhir/issues/1585. HAPI will fill in the
   * resource count as a total value when a Bundle has no total value.
   *
   * @param requestDetails of a resource provider
   */
  public static void workAroundHAPIIssue1585(RequestDetails requestDetails) {
    // The hack is to remove the _count parameter from theDetails so that total is not modified.
    Map<String, String[]> params = new HashMap<String, String[]>(requestDetails.getParameters());
    if (params.remove(Constants.PARAM_COUNT) != null) {
      // Remove _count parameter from the current request details
      requestDetails.setParameters(params);
    }
  }

  /**
   * Creates a {@link Reference} to a coverage resource.
   *
   * @param beneficiaryPatientId the bene ID value for the {@link Coverage#getBeneficiary()} value
   *     to match
   * @param coverageType the {@link MedicareSegment} value to match
   * @return a {@link Reference} to the {@link Coverage} resource
   */
  static Reference referenceCoverage(Long beneficiaryPatientId, MedicareSegment coverageType) {
    return new Reference(
        CommonTransformerUtils.buildCoverageId(coverageType, beneficiaryPatientId));
  }

  /**
   * Creates a coding.
   *
   * @param rootResource the root FHIR {@link IAnyResource} that the resultant {@link Coding} will
   *     be contained in
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @param yearMonth the value to use for {@link String} for yearMonth
   * @param code the value to use for {@link Coding#getCode()}
   * @return the output {@link Coding} for the specified input values
   */
  private static Coding createCoding(
      IAnyResource rootResource, CcwCodebookInterface ccwVariable, String yearMonth, Object code) {
    /*
     * The code parameter is an Object to avoid needing multiple copies of this and related methods.
     * This if-else block is the price to be paid for that, though.
     */
    String codeString;
    if (code instanceof Character) {
      codeString = ((Character) code).toString();
    } else if (code instanceof String) {
      codeString = code.toString().trim();
    } else {
      throw new BadCodeMonkeyException("Unsupported: " + code);
    }

    String system = CCWUtils.calculateVariableReferenceUrl(ccwVariable);
    String display =
        (ccwVariable.getVariable().getValueGroups().isPresent())
            ? CommonTransformerUtils.calculateCodingDisplay(rootResource, ccwVariable, codeString)
                .orElse(null)
            : null;

    return new Coding(system, codeString, display);
  }

  /**
   * Gets the claim type from the specified {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} to extract the claim type from
   * @return the {@link ClaimType}
   */
  static ClaimType getClaimType(ExplanationOfBenefit eob) {
    String type =
        eob.getType().getCoding().stream()
            .filter(c -> c.getSystem().equals(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE))
            .findFirst()
            .get()
            .getCode();
    return ClaimType.valueOf(type);
  }

  /**
   * Transforms the common group level header fields between all claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param claimId CLM_ID
   * @param beneficiaryId BENE_ID
   * @param claimType {@link ClaimType} to process
   * @param claimGroupId CLM_GRP_ID
   * @param coverageType {@link MedicareSegment}
   * @param dateFrom CLM_FROM_DT || SRVC_DT (For Part D Events)
   * @param dateThrough CLM_THRU_DT || SRVC_DT (For Part D Events)
   * @param paymentAmount CLM_PMT_AMT
   * @param finalAction FINAL_ACTION
   */
  static void mapEobCommonClaimHeaderData(
      ExplanationOfBenefit eob,
      Long claimId,
      Long beneficiaryId,
      ClaimType claimType,
      String claimGroupId,
      MedicareSegment coverageType,
      Optional<LocalDate> dateFrom,
      Optional<LocalDate> dateThrough,
      Optional<BigDecimal> paymentAmount,
      char finalAction) {

    // Claim Type + Claim ID => ExplanationOfBenefit.id
    eob.setId(buildEobId(claimType, claimId));

    // Current timestamp => Created
    eob.setCreated(new Date());

    // "claim" => ExplanationOfBenefit.use
    eob.setUse(Use.CLAIM);

    if (claimType.equals(ClaimType.PDE)) {
      // PDE_ID => ExplanationOfBenefit.identifier
      eob.addIdentifier(createClaimIdentifier(CcwCodebookVariable.PDE_ID, String.valueOf(claimId)));
    } else {
      // CLM_ID => ExplanationOfBenefit.identifier
      eob.addIdentifier(createClaimIdentifier(CcwCodebookVariable.CLM_ID, String.valueOf(claimId)));
    }

    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    eob.addIdentifier()
        .setSystem(TransformerConstants.IDENTIFIER_SYSTEM_BBAPI_CLAIM_GROUP_ID)
        .setValue(claimGroupId)
        .setType(createC4BBClaimCodeableConcept());

    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage (ref)
    // There is always just one insurance coverage documented, since they are hard coded by
    // claim type. If we get to a point where this is no longer hard coded, and we may have more
    // than one insurance coverage per claim, we may have
    // to additional logic to determine which insurance coverage(s) are used for adjudication.
    eob.addInsurance().setFocal(true).setCoverage(referenceCoverage(beneficiaryId, coverageType));

    // BENE_ID => ExplanationOfBenefit.patient (reference)
    eob.setPatient(referencePatient(beneficiaryId));

    // "insurer" => ExplanationOfBenefit.insurer
    eob.setInsurer(new Reference().setIdentifier(new Identifier().setValue("CMS")));

    // "outcome" => ExplanationOfBenefit.outcome
    eob.setOutcome(RemittanceOutcome.COMPLETE);

    // FINAL_ACTION => ExplanationOfBenefit.status
    switch (finalAction) {
      case 'F':
        eob.setStatus(ExplanationOfBenefitStatus.ACTIVE);
        break;

      case 'N':
        eob.setStatus(ExplanationOfBenefitStatus.CANCELLED);
        break;

      default:
        // unknown final action value
        throw new BadCodeMonkeyException();
    }

    // CLM_FROM_DT || SRVC_DT (For Part D Events) => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT || SRVC_DT (For Part D Events) => ExplanationOfBenefit.billablePeriod.end
    if (dateFrom.isPresent()) {
      validatePeriodDates(dateFrom, dateThrough);
      setPeriodStart(eob.getBillablePeriod(), dateFrom.get());
      setPeriodEnd(eob.getBillablePeriod(), dateThrough.get());
    }

    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    if (paymentAmount.isPresent()) {
      eob.getPayment().setAmount(createMoney(paymentAmount));
    }
  }

  /**
   * Creates a new {@link Money} from the specified amount value.
   *
   * @param amountValue the value to use for {@link Money#getValue()}
   * @return a new {@link Money} instance, with the specified {@link Money#getValue()}
   */
  static Money createMoney(Optional<? extends Number> amountValue) {
    if (!amountValue.isPresent()) {
      throw new IllegalArgumentException();
    }

    Money money = new Money().setCurrency(TransformerConstants.CODED_MONEY_USD);

    if (amountValue.get() instanceof BigDecimal) {
      money.setValue((BigDecimal) amountValue.get());
    } else {
      throw new BadCodeMonkeyException();
    }

    return money;
  }

  /**
   * Creates a new {@link Money} from the specified amount value.
   *
   * @param amountValue the value to use for {@link Money#getValue()}
   * @return a new {@link Money} instance, with the specified {@link Money#getValue()}
   */
  static Money createMoney(Number amountValue) {
    return createMoney(Optional.of(amountValue));
  }

  /**
   * Ensures that the specified {@link ExplanationOfBenefit} has the specified {@link
   * CareTeamComponent}, and links the specified {@link ItemComponent} to that {@link
   * CareTeamComponent} (via {@link ItemComponent#addCareTeamSequence(int)}).
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param eobItem the {@link ItemComponent} that should be linked to the {@link CareTeamComponent}
   * @param type the type
   * @param practitionerIdValue the {@link Identifier#getValue()} of the practitioner to reference
   *     in {@link CareTeamComponent#getProvider()}
   * @param roleSystem the role system
   * @param roleCode the role code
   * @param roleDisplay the role display
   * @return the {@link CareTeamComponent} that was created/linked
   */
  private static CareTeamComponent addCareTeamPractitioner(
      ExplanationOfBenefit eob,
      ItemComponent eobItem,
      C4BBPractitionerIdentifierType type,
      String practitionerIdValue,
      String roleSystem,
      String roleCode,
      String roleDisplay) {
    // Try to find a matching pre-existing entry.
    CareTeamComponent careTeamEntry =
        eob.getCareTeam().stream()
            .filter(ctc -> ctc.getProvider().hasIdentifier())
            .filter(ctc -> ctc.hasRole())
            .filter(
                ctc ->
                    ctc.getProvider().getIdentifier().getType().getCoding().stream()
                            .anyMatch(
                                c ->
                                    c.getSystem().equalsIgnoreCase(type.getSystem())
                                        && c.getCode().equalsIgnoreCase(type.toCode()))
                        && practitionerIdValue.equalsIgnoreCase(
                            ctc.getProvider().getIdentifier().getValue()))
            .filter(
                ctc ->
                    roleCode.equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getCode())
                        && roleSystem.equalsIgnoreCase(
                            ctc.getRole().getCodingFirstRep().getSystem()))
            .findAny()
            .orElse(null);

    // If no match was found, add one to the EOB.
    // <ID Value> => ExplanationOfBenefit.careTeam.provider
    if (careTeamEntry == null) {
      careTeamEntry = eob.addCareTeam();
      // addItem adds and returns, so we want size() not size() + 1 here
      careTeamEntry.setSequence(eob.getCareTeam().size());
      careTeamEntry.setProvider(createPractitionerIdentifierReference(type, practitionerIdValue));

      CodeableConcept careTeamRoleConcept = createCodeableConcept(roleSystem, roleCode);
      careTeamRoleConcept.getCodingFirstRep().setDisplay(roleDisplay);
      careTeamEntry.setRole(careTeamRoleConcept);
    }

    // care team entry is at eob level so no need to create item link id
    if (eobItem == null) {
      return careTeamEntry;
    }

    // ExplanationOfBenefit.careTeam.sequence => ExplanationOfBenefit.item.careTeamSequence
    if (!eobItem.getCareTeamSequence().contains(new PositiveIntType(careTeamEntry.getSequence()))) {
      eobItem.addCareTeamSequence(careTeamEntry.getSequence());
    }

    return careTeamEntry;
  }

  /**
   * Ensures that the specified {@link ExplanationOfBenefit} has the specified {@link
   * CareTeamComponent}, and links the specified {@link ItemComponent} to that {@link
   * CareTeamComponent} (via {@link ItemComponent#addCareTeamSequence(int)}).
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param type the type
   * @param practitionerIdValue the {@link Identifier#getValue()} of the practitioner to reference
   *     in {@link CareTeamComponent#getProvider()}
   * @param roleSystem the {@link String} role system to use for the care team system.
   * @param roleCode the {@link String} role code to use for the care team code.
   * @param roleDisplay the {@link String} role display to use for the care team display.
   * @param npiOrgDisplay the {@link Optional} npi org display to use for the care team npi org
   *     display.
   * @return the {@link CareTeamComponent} that was created/linked
   */
  private static CareTeamComponent addCareTeamPractitionerWithNpiOrg(
      ExplanationOfBenefit eob,
      C4BBPractitionerIdentifierType type,
      String practitionerIdValue,
      String roleSystem,
      String roleCode,
      String roleDisplay,
      Optional<String> npiOrgDisplay) {
    // Try to find a matching pre-existing entry.
    CareTeamComponent careTeamEntry =
        eob.getCareTeam().stream()
            .filter(ctc -> ctc.getProvider().hasIdentifier())
            .filter(ctc -> ctc.hasRole())
            .filter(
                ctc ->
                    ctc.getProvider().getIdentifier().getType().getCoding().stream()
                            .anyMatch(
                                c ->
                                    c.getSystem().equalsIgnoreCase(type.getSystem())
                                        && c.getCode().equalsIgnoreCase(type.toCode()))
                        && practitionerIdValue.equalsIgnoreCase(
                            ctc.getProvider().getIdentifier().getValue()))
            .filter(
                ctc ->
                    roleCode.equalsIgnoreCase(ctc.getRole().getCodingFirstRep().getCode())
                        && roleSystem.equalsIgnoreCase(
                            ctc.getRole().getCodingFirstRep().getSystem()))
            .findAny()
            .orElse(null);

    // If no match was found, add one to the EOB.
    // <ID Value> => ExplanationOfBenefit.careTeam.provider
    if (careTeamEntry == null) {
      careTeamEntry = eob.addCareTeam();
      // addItem adds and returns, so we want size() not size() + 1 here
      careTeamEntry.setSequence(eob.getCareTeam().size());
      careTeamEntry.setProvider(
          createPractitionerIdentifierReferenceWithNpiOrg(
              type, practitionerIdValue, npiOrgDisplay));

      CodeableConcept careTeamRoleConcept = createCodeableConcept(roleSystem, roleCode);
      careTeamRoleConcept.getCodingFirstRep().setDisplay(roleDisplay);
      careTeamEntry.setRole(careTeamRoleConcept);
    }

    return careTeamEntry;
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}. Unlike {@link #addInformation(ExplanationOfBenefit,
   * CcwCodebookInterface)}, this also sets the {@link SupportingInformationComponent#getCode()}
   * based on the values provided.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param categoryVariable {@link CcwCodebookInterface} to map to {@link
   *     SupportingInformationComponent#getCategory()}
   * @param codeSystemVariable the {@link CcwCodebookInterface} to map to the {@link
   *     Coding#getSystem()} used in the {@link SupportingInformationComponent#getCode()}
   * @param codeValue the value to map to the {@link Coding#getCode()} used in the {@link
   *     SupportingInformationComponent#getCode()}
   * @return the newly-added {@link SupportingInformationComponent} entry
   */
  static SupportingInformationComponent addInformationWithCode(
      ExplanationOfBenefit eob,
      CcwCodebookInterface categoryVariable,
      CcwCodebookInterface codeSystemVariable,
      Optional<?> codeValue) {
    SupportingInformationComponent infoComponent = addInformation(eob, categoryVariable);

    CodeableConcept infoCode =
        new CodeableConcept().addCoding(createCoding(eob, codeSystemVariable, codeValue));
    infoComponent.setCode(infoCode);

    return infoComponent;
  }

  /**
   * Adds the specified information slice to the specified {@link ExplanationOfBenefit} and returns
   * the {@link SupportingInformationComponent} that was added.
   *
   * @param eob the eob to add the slice to
   * @param slice the slice to add
   * @param categoryVariable the category variable (unused)
   * @param codeSystemVariable the system to use for information slice
   * @param codeValue the code value to use for information slice
   * @return the information slice added, or an empty {@link Optional} if the component could not be
   *     created
   */
  static Optional<SupportingInformationComponent> addInformationSliceWithCode(
      ExplanationOfBenefit eob,
      C4BBSupportingInfoType slice,
      CcwCodebookInterface categoryVariable,
      CcwCodebookInterface codeSystemVariable,
      Optional<?> codeValue) {
    if (codeValue.isPresent()) {
      SupportingInformationComponent infoComponent = addInformationSlice(eob, slice);

      CodeableConcept infoCode =
          new CodeableConcept().addCoding(createCoding(eob, codeSystemVariable, codeValue));
      infoComponent.setCode(infoCode);

      return Optional.of(infoComponent);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Adds the specified information slice to the specified {@link ExplanationOfBenefit} and returns
   * the {@link SupportingInformationComponent} that was added.
   *
   * @param eob the eob to add the slice to
   * @param slice the slice to add
   * @param categoryVariable the category variable
   * @param codeSystemVariable the system to use for information slice
   * @param codeValue the code value to use for information slice
   * @return the information slice added
   */
  static SupportingInformationComponent addInformationSliceWithCode(
      ExplanationOfBenefit eob,
      C4BBSupportingInfoType slice,
      CcwCodebookInterface categoryVariable,
      CcwCodebookInterface codeSystemVariable,
      Object codeValue) {
    // Must get a valid value passed in
    if (codeValue == null) {
      throw new BadCodeMonkeyException();
    }

    return addInformationSliceWithCode(
            eob, slice, categoryVariable, codeSystemVariable, Optional.of(codeValue))
        // Since we are passing in a valid value, we will get a response
        .get();
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}. Unlike {@link #addInformation(ExplanationOfBenefit,
   * CcwCodebookInterface)}, this also sets the {@link SupportingInformationComponent#getCode()}
   * based on the values provided.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param categoryVariable {@link CcwCodebookInterface} to map to {@link
   *     SupportingInformationComponent#getCategory()}
   * @param codeSystemVariable the {@link CcwCodebookInterface} to map to the {@link
   *     Coding#getSystem()} used in the {@link SupportingInformationComponent#getCode()}
   * @param date the value to map to the {@link SupportingInformationComponent#getTiming()}
   * @return the newly-added {@link SupportingInformationComponent} entry
   */
  static SupportingInformationComponent addInformationWithDate(
      ExplanationOfBenefit eob,
      CcwCodebookInterface categoryVariable,
      CcwCodebookInterface codeSystemVariable,
      Optional<LocalDate> date) {
    SupportingInformationComponent infoComponent = addInformation(eob, categoryVariable);

    if (!date.isPresent()) {
      throw new BadCodeMonkeyException();
    }

    return infoComponent.setTiming(new DateType(convertToDate(date.get())));
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}. Unlike {@link #addInformation(ExplanationOfBenefit,
   * CcwCodebookInterface)}, this also sets the {@link SupportingInformationComponent#getCode()}
   * based on the values provided.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param categoryVariable {@link CcwCodebookInterface} to map to {@link
   *     SupportingInformationComponent#getCategory()}
   * @param codeSystemVariable the {@link CcwCodebookInterface} to map to the {@link
   *     Coding#getSystem()} used in the {@link SupportingInformationComponent#getCode()}
   * @param codeValue the value to map to the {@link Coding#getCode()} used in the {@link
   *     SupportingInformationComponent#getCode()}
   * @return the newly-added {@link SupportingInformationComponent} entry
   */
  static SupportingInformationComponent addInformationWithCode(
      ExplanationOfBenefit eob,
      CcwCodebookInterface categoryVariable,
      CcwCodebookInterface codeSystemVariable,
      Object codeValue) {
    return addInformationWithCode(
        eob, categoryVariable, codeSystemVariable, Optional.of(codeValue));
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param slice the slice to add
   * @return the added slice
   */
  static SupportingInformationComponent addInformationSlice(
      ExplanationOfBenefit eob, C4BBSupportingInfoType slice) {
    return addInformation(eob)
        .setCategory(new CodeableConcept().addCoding(createC4BBSupportingInfoCoding(slice)));
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param categoryVariable {@link CcwCodebookInterface} to map to {@link
   *     SupportingInformationComponent#getCategory()}
   * @return the newly-added {@link SupportingInformationComponent} entry
   */
  static SupportingInformationComponent addInformation(
      ExplanationOfBenefit eob, CcwCodebookInterface categoryVariable) {
    return addInformation(eob)
        .setCategory(
            createCodeableConceptForFieldId(
                eob, TransformerConstants.CODING_BBAPI_INFORMATION_CATEGORY, categoryVariable));
  }

  /**
   * Returns a new {@link SupportingInformationComponent} that has been added to the specified
   * {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @return the newly-added {@link SupportingInformationComponent} entry
   */
  static SupportingInformationComponent addInformation(ExplanationOfBenefit eob) {
    int maxSequence =
        eob.getSupportingInfo().stream().mapToInt(i -> i.getSequence()).max().orElse(0);

    SupportingInformationComponent infoComponent = new SupportingInformationComponent();
    infoComponent.setSequence(maxSequence + 1);
    eob.getSupportingInfo().add(infoComponent);

    return infoComponent;
  }

  /**
   * Builds an id for an {@link ExplanationOfBenefit}.
   *
   * @param claimType the {@link ClaimType} to compute an {@link ExplanationOfBenefit#getId()} for
   * @param claimId the <code>claimId</code> field value (e.g. from {@link
   *     CarrierClaim#getClaimId()} to compute an {@link ExplanationOfBenefit#getId()} for
   * @return the {@link ExplanationOfBenefit#getId()} value to use for the specified <code>
   *     claimId     </code> value
   */
  public static String buildEobId(ClaimType claimType, Long claimId) {
    return String.format("%s-%d", claimType.name().toLowerCase(), claimId);
  }

  /**
   * Maps a blue button claim type to a FHIR claim type.
   *
   * @param eob the {@link CodeableConcept} that will get remapped
   * @param blueButtonClaimType the blue button {@link ClaimType} we are mapping from
   * @param ccwNearLineRecordIdCode if present, the blue button near line id code {@link
   *     Optional}&lt;{@link Character}&gt; gets remapped to a ccw record id code
   * @param ccwClaimTypeCode if present, the blue button claim type code {@link Optional}&lt;{@link
   *     String}&gt; gets remapped to a nch claim type code
   */
  static void mapEobType(
      ExplanationOfBenefit eob,
      ClaimType blueButtonClaimType,
      Optional<Character> ccwNearLineRecordIdCode,
      Optional<String> ccwClaimTypeCode) {

    // map blue button claim type code into a nch claim type
    // NCH_CLM_TYPE_CD => ExplanationOfBenefit.type.coding
    if (ccwClaimTypeCode.isPresent()) {
      eob.getType()
          .addCoding(createCoding(eob, CcwCodebookVariable.NCH_CLM_TYPE_CD, ccwClaimTypeCode));
    }

    // This Coding MUST always be present as it's the only one we can definitely map
    // for all 8 of our claim types.
    // EOB Type => ExplanationOfBenefit.type.coding
    eob.getType()
        .addCoding()
        .setSystem(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE)
        .setCode(blueButtonClaimType.name());

    // Map a Coding for FHIR's ClaimType coding system, if we can.
    org.hl7.fhir.r4.model.codesystems.ClaimType fhirClaimType;
    switch (blueButtonClaimType) {
      case PDE:
        fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.PHARMACY;
        break;

      case INPATIENT:
      case OUTPATIENT:
      case HOSPICE:
      case SNF:
      case DME:
        fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.INSTITUTIONAL;
        break;

      case CARRIER:
      case HHA:
        fhirClaimType = org.hl7.fhir.r4.model.codesystems.ClaimType.PROFESSIONAL;
        break;

      default:
        // All options on ClaimType are covered above, but this is there to appease linter
        throw new BadCodeMonkeyException("No match found for ClaimType");
    }

    // Claim Type => ExplanationOfBenefit.type.coding
    if (fhirClaimType != null) {
      eob.getType()
          .addCoding(
              new Coding(
                  fhirClaimType.getSystem(), fhirClaimType.toCode(), fhirClaimType.getDisplay()));
    }

    // map blue button near line record id to a ccw record id code
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    if (ccwNearLineRecordIdCode.isPresent()) {
      eob.addExtension(
          createExtensionCoding(
              eob, CcwCodebookVariable.NCH_NEAR_LINE_REC_IDENT_CD, ccwNearLineRecordIdCode));
    }
  }

  /**
   * Gets the unprefixed claim id.
   *
   * @param eob the {@link ExplanationOfBenefit} to extract the id from
   * @return the <code>claimId</code> field value (e.g. from {@link CarrierClaim#getClaimId()})
   */
  static String getUnprefixedClaimId(ExplanationOfBenefit eob) {
    for (Identifier i : eob.getIdentifier()) {
      if (i.getSystem().contains("clm_id") || i.getSystem().contains("pde_id")) {
        return i.getValue();
      }
    }

    throw new BadCodeMonkeyException("A claim ID was expected but none was found.");
  }

  /**
   * Adds EOB information to fields that are common between the Inpatient and SNF claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} that fields will be added to by this method
   * @param admissionTypeCd CLM_IP_ADMSN_TYPE_CD: a {@link Character} shared field representing the
   *     admission type cd for the claim
   * @param sourceAdmissionCd CLM_SRC_IP_ADMSN_CD: an {@link Optional}&lt;{@link Character}&gt;
   *     shared field representing the source admission cd for the claim
   * @param noncoveredStayFromDate NCH_VRFD_NCVRD_STAY_FROM_DT: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the non-covered stay from date for the claim
   * @param noncoveredStayThroughDate NCH_VRFD_NCVRD_STAY_THRU_DT: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the non-covered stay through date for the claim
   * @param coveredCareThroughDate NCH_ACTV_OR_CVRD_LVL_CARE_THRU: an {@link Optional}&lt;{@link
   *     LocalDate}&gt; shared field representing the covered stay through date for the claim
   * @param medicareBenefitsExhaustedDate NCH_BENE_MDCR_BNFTS_EXHTD_DT_I: an {@link
   *     Optional}&lt;{@link LocalDate}&gt; shared field representing the medicare benefits
   *     exhausted date for the claim
   * @param diagnosisRelatedGroupCd CLM_DRG_CD: an {@link Optional}&lt;{@link String}&gt; shared
   *     field representing the non-covered stay from date for the claim
   * @param fiClaimActionCd FI_CLM_ACTN_CD: a {@link Character} shared field representing the fiscal
   *     intermediary action cd for the claim
   */
  static void addCommonEobInformationInpatientSNF(
      ExplanationOfBenefit eob,
      Character admissionTypeCd,
      Optional<Character> sourceAdmissionCd,
      Optional<LocalDate> noncoveredStayFromDate,
      Optional<LocalDate> noncoveredStayThroughDate,
      Optional<LocalDate> coveredCareThroughDate,
      Optional<LocalDate> medicareBenefitsExhaustedDate,
      Optional<String> diagnosisRelatedGroupCd,
      Optional<Character> fiClaimActionCd) {

    // CLM_IP_ADMSN_TYPE_CD => ExplanationOfBenefit.supportingInfo.code
    addInformationWithCode(
        eob,
        CcwCodebookVariable.CLM_IP_ADMSN_TYPE_CD,
        CcwCodebookVariable.CLM_IP_ADMSN_TYPE_CD,
        admissionTypeCd);

    // CLM_SRC_IP_ADMSN_CD => ExplanationOfBenefit.supportingInfo.code
    if (sourceAdmissionCd.isPresent()) {
      addInformationWithCode(
          eob,
          CcwCodebookVariable.CLM_SRC_IP_ADMSN_CD,
          CcwCodebookVariable.CLM_SRC_IP_ADMSN_CD,
          sourceAdmissionCd);
    }

    // noncoveredStayFromDate & noncoveredStayThroughDate
    // NCH_VRFD_NCVRD_STAY_FROM_DT =>
    // ExplanationOfBenefit.supportingInfo.timingPeriod
    // NCH_VRFD_NCVRD_STAY_THRU_DT =>
    // ExplanationOfBenefit.supportingInfo.timingPeriod
    if (noncoveredStayFromDate.isPresent() || noncoveredStayThroughDate.isPresent()) {
      validatePeriodDates(noncoveredStayFromDate, noncoveredStayThroughDate);

      SupportingInformationComponent nchVrfdNcvrdStayInfo =
          addInformation(eob, CcwCodebookVariable.NCH_VRFD_NCVRD_STAY_FROM_DT);

      Period nchVrfdNcvrdStayPeriod = new Period();

      noncoveredStayFromDate.ifPresent(
          d -> nchVrfdNcvrdStayPeriod.setStart(convertToDate(d), TemporalPrecisionEnum.DAY));
      noncoveredStayThroughDate.ifPresent(
          d -> nchVrfdNcvrdStayPeriod.setEnd(convertToDate(d), TemporalPrecisionEnum.DAY));

      nchVrfdNcvrdStayInfo.setTiming(nchVrfdNcvrdStayPeriod);
    }

    // coveredCareThroughDate
    // NCH_ACTV_OR_CVRD_LVL_CARE_THRU =>
    // ExplanationOfBenefit.supportingInfo.timingDate
    if (coveredCareThroughDate.isPresent()) {
      SupportingInformationComponent nchActvOrCvrdLvlCareThruInfo =
          TransformerUtilsV2.addInformation(
              eob, CcwCodebookVariable.NCH_ACTV_OR_CVRD_LVL_CARE_THRU);
      nchActvOrCvrdLvlCareThruInfo.setTiming(
          new DateType(convertToDate(coveredCareThroughDate.get())));
    }

    // medicareBenefitsExhaustedDate
    // NCH_BENE_MDCR_BNFTS_EXHTD_DT_I =>
    // ExplanationOfBenefit.supportingInfo.timingDate
    if (medicareBenefitsExhaustedDate.isPresent()) {
      SupportingInformationComponent nchBeneMdcrBnftsExhtdDtIInfo =
          TransformerUtilsV2.addInformation(
              eob, CcwCodebookVariable.NCH_BENE_MDCR_BNFTS_EXHTD_DT_I);
      nchBeneMdcrBnftsExhtdDtIInfo.setTiming(
          new DateType(convertToDate(medicareBenefitsExhaustedDate.get())));
    }

    // diagnosisRelatedGroupCd
    // CLM_DRG_CD => ExplanationOfBenefit.supportingInfo
    diagnosisRelatedGroupCd.ifPresent(
        cd ->
            addInformationWithCode(
                eob, CcwCodebookVariable.CLM_DRG_CD, CcwCodebookVariable.CLM_DRG_CD, cd));

    // FI_CLM_ACTN_CD => ExplanationOfBenefit.extension
    fiClaimActionCd.ifPresent(
        value ->
            eob.addExtension(
                createExtensionCoding(eob, CcwCodebookVariable.FI_CLM_ACTN_CD, value)));
  }

  /**
   * Transforms the common group level data elements between the {@link CarrierClaim} and {@link
   * DMEClaim}* claim types to FHIR. The method parameter fields from {@link CarrierClaim} and
   * {@link DMEClaim}* are listed below and their corresponding RIF CCW fields (denoted in all CAPS
   * below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param carrierNumber CARR_NUM,
   * @param clinicalTrialNumber CLM_CLNCL_TRIL_NUM,
   * @param beneficiaryPartBDeductAmount CARR_CLM_CASH_DDCTBL_APLD_AMT,
   * @param paymentDenialCode CARR_CLM_PMT_DNL_CD,
   * @param referringPhysicianNpi RFR_PHYSN_NPI
   * @param referringPhysicianUpin RFR_PHYSN_UPIN
   * @param providerAssignmentIndicator CARR_CLM_PRVDR_ASGNMT_IND_SW,
   * @param providerPaymentAmount NCH_CLM_PRVDR_PMT_AMT,
   * @param beneficiaryPaymentAmount NCH_CLM_BENE_PMT_AMT,
   * @param submittedChargeAmount NCH_CARR_CLM_SBMTD_CHRG_AMT,
   * @param allowedChargeAmount NCH_CARR_CLM_ALOWD_AMT,
   * @param claimDispositionCode CLM_DISP_CD
   * @param claimCarrierControlNumber CARR_CLM_CNTL_NUM
   */
  static void mapEobCommonGroupCarrierDME(
      ExplanationOfBenefit eob,
      String carrierNumber,
      Optional<String> clinicalTrialNumber,
      BigDecimal beneficiaryPartBDeductAmount,
      String paymentDenialCode,
      Optional<String> referringPhysicianNpi,
      Optional<String> referringPhysicianUpin,
      Optional<Character> providerAssignmentIndicator,
      BigDecimal providerPaymentAmount,
      BigDecimal beneficiaryPaymentAmount,
      BigDecimal submittedChargeAmount,
      BigDecimal allowedChargeAmount,
      String claimDispositionCode,
      Optional<String> claimCarrierControlNumber) {

    // CARR_NUM => ExplanationOfBenefit.extension
    eob.addExtension(createExtensionIdentifier(CcwCodebookVariable.CARR_NUM, carrierNumber));

    // ?? Not mapped?
    if (claimCarrierControlNumber.isPresent()) {
      eob.addExtension(
          createExtensionIdentifier(
              CcwCodebookMissingVariable.CARR_CLM_CNTL_NUM, claimCarrierControlNumber.get()));
    }

    // CARR_CLM_PMT_DNL_CD => ExplanationOfBenefit.extension
    eob.addExtension(
        createExtensionCoding(eob, CcwCodebookVariable.CARR_CLM_PMT_DNL_CD, paymentDenialCode));

    // CLM_DISP_CD => ExplanationOfBenefit.disposition
    eob.setDisposition(claimDispositionCode);

    // RFR_PHYSN_NPI => ExplanationOfBenefit.referral.identifier
    referringPhysicianNpi.ifPresent(
        npi ->
            eob.setReferral(
                createPractitionerIdentifierReference(C4BBPractitionerIdentifierType.NPI, npi)));

    // RFR_PHYSN_NPI => ExplanationOfBenefit.careteam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.REFERRING,
        referringPhysicianNpi);

    // If we don't have an NPI, do we have a UPIN?
    if (!referringPhysicianNpi.isPresent()) {
      // RFR_PHYSN_UPIN => ExplanationOfBenefit.referral.identifier
      referringPhysicianUpin.ifPresent(
          upin ->
              eob.setReferral(
                  createPractitionerIdentifierReference(
                      C4BBPractitionerIdentifierType.UPIN, upin)));

      // RFR_PHYSN_NPI => ExplanationOfBenefit.careteam.provider
      addCareTeamMember(
          eob,
          C4BBPractitionerIdentifierType.UPIN,
          C4BBClaimProfessionalAndNonClinicianCareTeamRole.REFERRING,
          referringPhysicianUpin);
    }

    // CARR_CLM_PRVDR_ASGNMT_IND_SW => ExplanationOfBenefit.extension
    if (providerAssignmentIndicator.isPresent()) {
      eob.addExtension(
          createExtensionCoding(eob, CcwCodebookVariable.ASGMNTCD, providerAssignmentIndicator));
    }

    // CLM_CLNCL_TRIL_NUM => ExplanationOfBenefit.extension
    if (clinicalTrialNumber.isPresent()) {
      eob.addExtension(
          createExtensionIdentifier(CcwCodebookVariable.CLM_CLNCL_TRIL_NUM, clinicalTrialNumber));
    }

    // CARR_CLM_CASH_DDCTBL_APLD_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.CARR_CLM_CASH_DDCTBL_APLD_AMT, beneficiaryPartBDeductAmount);

    // NCH_CLM_PRVDR_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_CLM_PRVDR_PMT_AMT, providerPaymentAmount);

    // NCH_CLM_BENE_PMT_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_CLM_BENE_PMT_AMT, beneficiaryPaymentAmount);

    // NCH_CARR_CLM_SBMTD_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_CARR_CLM_SBMTD_CHRG_AMT, submittedChargeAmount);

    // NCH_CARR_CLM_ALOWD_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_CARR_CLM_ALOWD_AMT, allowedChargeAmount);
  }

  /**
   * Adds field values to the benefit balance component that are common between the Inpatient and
   * SNF claim types.
   *
   * @param eob the {@link ExplanationOfBenefit} to map the fields into
   * @param coinsuranceDayCount BENE_TOT_COINSRNC_DAYS_CNT: a {@link BigDecimal} shared field
   *     representing the coinsurance day count for the claim
   * @param nonUtilizationDayCount CLM_NON_UTLZTN_DAYS_CNT: a {@link BigDecimal} shared field
   *     representing the non-utilization day count for the claim
   * @param deductibleAmount NCH_BENE_IP_DDCTBL_AMT: a {@link BigDecimal} shared field representing
   *     the deductible amount for the claim
   * @param partACoinsuranceLiabilityAmount NCH_BENE_PTA_COINSRNC_LBLTY_AM: a {@link BigDecimal}
   *     shared field representing the part A coinsurance amount for the claim
   * @param bloodPintsFurnishedQty NCH_BLOOD_PNTS_FRNSHD_QTY: a {@link BigDecimal} shared field
   *     representing the blood pints furnished quantity for the claim
   * @param noncoveredCharge NCH_IP_NCVRD_CHRG_AMT: a {@link BigDecimal} shared field representing
   *     the non-covered charge for the claim
   * @param totalDeductionAmount NCH_IP_TOT_DDCTN_AMT: a {@link BigDecimal} shared field
   *     representing the total deduction amount for the claim
   * @param claimPPSCapitalDisproportionateShareAmt CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT: an {@link
   *     Optional}&lt;{@link BigDecimal}&gt; shared field representing the claim PPS capital
   *     disproportionate share amount for the claim
   * @param claimPPSCapitalExceptionAmount CLM_PPS_CPTL_EXCPTN_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital exception amount for the
   *     claim
   * @param claimPPSCapitalFSPAmount CLM_PPS_CPTL_FSP_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital FSP amount for the claim
   * @param claimPPSCapitalIMEAmount CLM_PPS_CPTL_IME_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital IME amount for the claim
   * @param claimPPSCapitalOutlierAmount CLM_PPS_CPTL_OUTLIER_AMT: an {@link Optional}&lt;{@link
   *     BigDecimal}&gt; shared field representing the claim PPS capital outlier amount for the
   *     claim
   * @param claimPPSOldCapitalHoldHarmlessAmount CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT: an {@link
   *     Optional}&lt;{@link BigDecimal}&gt; shared field representing the claim PPS old capital
   *     hold harmless amount for the claim
   */
  static void addCommonGroupInpatientSNF(
      ExplanationOfBenefit eob,
      BigDecimal coinsuranceDayCount,
      BigDecimal nonUtilizationDayCount,
      BigDecimal deductibleAmount,
      BigDecimal partACoinsuranceLiabilityAmount,
      BigDecimal bloodPintsFurnishedQty,
      BigDecimal noncoveredCharge,
      BigDecimal totalDeductionAmount,
      Optional<BigDecimal> claimPPSCapitalDisproportionateShareAmt,
      Optional<BigDecimal> claimPPSCapitalExceptionAmount,
      Optional<BigDecimal> claimPPSCapitalFSPAmount,
      Optional<BigDecimal> claimPPSCapitalIMEAmount,
      Optional<BigDecimal> claimPPSCapitalOutlierAmount,
      Optional<BigDecimal> claimPPSOldCapitalHoldHarmlessAmount) {

    // BENE_TOT_COINSRNC_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.BENE_TOT_COINSRNC_DAYS_CNT, coinsuranceDayCount);

    // CLM_NON_UTLZTN_DAYS_CNT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalInt(
        eob, CcwCodebookVariable.CLM_NON_UTLZTN_DAYS_CNT, nonUtilizationDayCount);

    // NCH_BENE_IP_DDCTBL_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_BENE_IP_DDCTBL_AMT, deductibleAmount);

    // NCH_BENE_PTA_COINSRNC_LBLTY_AMT =>
    // ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_BENE_PTA_COINSRNC_LBLTY_AMT, partACoinsuranceLiabilityAmount);

    // NCH_BLOOD_PNTS_FRNSHD_QTY =>
    // ExplanationOfBenefit.supportingInfo.valueQuantity
    SupportingInformationComponent nchBloodPntsFrnshdQtyInfo =
        addInformation(eob, CcwCodebookVariable.NCH_BLOOD_PNTS_FRNSHD_QTY);

    Quantity bloodPintsQuantity = new Quantity();
    bloodPintsQuantity.setValue(bloodPintsFurnishedQty);
    bloodPintsQuantity
        .setSystem(TransformerConstants.CODING_SYSTEM_UCUM)
        .setCode(TransformerConstants.CODING_SYSTEM_UCUM_PINT_CODE)
        .setUnit(TransformerConstants.CODING_SYSTEM_UCUM_PINT_DISPLAY);

    nchBloodPntsFrnshdQtyInfo.setValue(bloodPintsQuantity);

    // NCH_IP_NCVRD_CHRG_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_IP_NCVRD_CHRG_AMT, noncoveredCharge);

    // NCH_IP_TOT_DDCTN_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_IP_TOT_DDCTN_AMT, totalDeductionAmount);

    // CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT,
        claimPPSCapitalDisproportionateShareAmt);

    // CLM_PPS_CPTL_EXCPTN_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.CLM_PPS_CPTL_EXCPTN_AMT, claimPPSCapitalExceptionAmount);

    // CLM_PPS_CPTL_FSP_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.CLM_PPS_CPTL_FSP_AMT, claimPPSCapitalFSPAmount);

    // CLM_PPS_CPTL_IME_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.CLM_PPS_CPTL_IME_AMT, claimPPSCapitalIMEAmount);

    // CLM_PPS_CPTL_OUTLIER_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.CLM_PPS_CPTL_OUTLIER_AMT, claimPPSCapitalOutlierAmount);

    // CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob,
        CcwCodebookVariable.CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT,
        claimPPSOldCapitalHoldHarmlessAmount);
  }

  /**
   * Adds an adjudication total to the specified {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the adjudication total should be part of
   * @param categoryVariable the {@link CcwCodebookInterface} to map to the adjudication's <code>
   *          category</code>
   * @param amountValue the {@link Money#getValue()} for the adjudication total
   */
  static void addAdjudicationTotal(
      ExplanationOfBenefit eob,
      CcwCodebookInterface categoryVariable,
      Optional<? extends Number> amountValue) {

    if (amountValue.isPresent()) {
      String extensionUrl = CCWUtils.calculateVariableReferenceUrl(categoryVariable);
      Money adjudicationTotalAmount = createMoney(amountValue);
      Extension adjudicationTotalEextension = new Extension(extensionUrl, adjudicationTotalAmount);

      eob.addExtension(adjudicationTotalEextension);
    }
  }

  /**
   * Adds an item revenue Coding to the specified {@link ItemComponent}.
   *
   * @param item the item to add the Coding to
   * @param eob the eob used in calculating parts of the Coding
   * @param categoryVariable the category variable used to create the Coding
   * @param code the code value used to create the Coding; if missing, cannot add the Coding
   */
  static void addItemRevenue(
      ItemComponent item,
      ExplanationOfBenefit eob,
      CcwCodebookInterface categoryVariable,
      Optional<?> code) {
    code.ifPresent(
        c -> {
          item.getRevenue()
              // Add standard coding
              .addCoding(
                  new Coding()
                      .setSystem(TransformerConstants.NUBC_REVENUE_CODE_SYSTEM)
                      .setCode(String.valueOf(c)))
              // Also add BB coding
              .addCoding(createCoding(eob, categoryVariable, Optional.of(c)));
        });
  }

  /**
   * Adds an adjudication total to the specified {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the adjudication total should be part of
   * @param categoryVariable the {@link CcwCodebookInterface} to map to the adjudication's <code>
   *          category</code>
   * @param totalAmountValue the {@link Money#getValue()} for the adjudication total
   */
  static void addAdjudicationTotal(
      ExplanationOfBenefit eob, CcwCodebookInterface categoryVariable, Number totalAmountValue) {
    addAdjudicationTotal(eob, categoryVariable, Optional.of(totalAmountValue));
  }

  /**
   * Adds a benefit balance financial component to the specified {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link ExBenefitcategory} should be part
   *     of
   * @param benefitCategoryCode the code representing an {@link ExBenefitcategory}
   * @param financialType the {@link CcwCodebookInterface} to map to {@link
   *     BenefitComponent#getType()}
   * @return the new {@link BenefitBalanceComponent}, which will have already been added to the
   *     appropriate {@link ExplanationOfBenefit#getBenefitBalance()} entry
   */
  static BenefitComponent addBenefitBalanceFinancial(
      ExplanationOfBenefit eob,
      ExBenefitcategory benefitCategoryCode,
      CcwCodebookInterface financialType) {
    BenefitBalanceComponent eobPrimaryBenefitBalance =
        findOrAddBenefitBalance(eob, benefitCategoryCode);

    CodeableConcept financialTypeConcept =
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_BBAPI_BENEFIT_BALANCE_TYPE,
            CCWUtils.calculateVariableReferenceUrl(financialType));

    financialTypeConcept.getCodingFirstRep().setDisplay(financialType.getVariable().getLabel());

    BenefitComponent financialEntry = new BenefitComponent(financialTypeConcept);
    eobPrimaryBenefitBalance.getFinancial().add(financialEntry);

    return financialEntry;
  }

  /**
   * Adds a {@link BenefitComponent} that has the passed in amount encoded in {@link
   * BenefitComponent#getUsedMoney()}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link BenefitComponent} should be part of
   * @param financialType the {@link CcwCodebookInterface} to map to {@link
   *     BenefitComponent#getType()}
   * @param amt Money amount to map to {@link BenefitComponent#getUsedMoney()}
   * @return the new {@link BenefitComponent} which will have already been added to the appropriate
   *     {@link ExplanationOfBenefit#getBenefitBalance()} entry
   */
  static BenefitComponent addBenefitBalanceFinancialMedicalAmt(
      ExplanationOfBenefit eob, CcwCodebookInterface financialType, BigDecimal amt) {
    // "1" is the code for MEDICAL in ExBenefitcategory
    return addBenefitBalanceFinancial(eob, ExBenefitcategory._1, financialType)
        .setUsed(createMoney(amt));
  }

  /**
   * Optionally adds a {@link BenefitComponent} that has the passed in amount encoded in {@link
   * BenefitComponent#getUsedMoney()}*.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link BenefitComponent} should be part of
   * @param financialType the {@link CcwCodebookInterface} to map to {@link
   *     BenefitComponent#getType()}
   * @param amount Money amount to map to {@link BenefitComponent#getUsedMoney()}
   * @return the new {@link BenefitComponent} which will have already been added to the appropriate
   *     {@link ExplanationOfBenefit#getBenefitBalance()} entry. Returns Empty if the amount wasn't
   *     set.
   */
  static Optional<BenefitComponent> addBenefitBalanceFinancialMedicalAmt(
      ExplanationOfBenefit eob, CcwCodebookInterface financialType, Optional<BigDecimal> amount) {
    return amount.map(
        amt -> addBenefitBalanceFinancialMedicalAmt(eob, financialType, amount.get()));
  }

  /**
   * Adds a {@link BenefitComponent} that has the passed in amount encoded in {@link
   * BenefitComponent#getUsedUnsignedIntType()}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link BenefitComponent} should be part of
   * @param financialType the {@link CcwCodebookInterface} to map to {@link
   *     BenefitComponent#getType()}
   * @param amount integral amount to map to {@link BenefitComponent#getUsedUnsignedIntType()}
   * @return the new {@link BenefitComponent} which will have already been added to the appropriate
   *     {@link ExplanationOfBenefit#getBenefitBalance()} entry
   */
  static BenefitComponent addBenefitBalanceFinancialMedicalInt(
      ExplanationOfBenefit eob, CcwCodebookInterface financialType, BigDecimal amount) {
    // "1" is the code for MEDICAL in ExBenefitcategory
    return addBenefitBalanceFinancial(eob, ExBenefitcategory._1, financialType)
        // TODO: intValueExact() not working?
        .setUsed(new UnsignedIntType(amount.intValue()));
  }

  /**
   * Optionally adds a {@link BenefitComponent} that has the passed in amount encoded in {@link
   * BenefitComponent#getUsedUnsignedIntType()}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link BenefitComponent} should be part of
   * @param financialType the {@link CcwCodebookInterface} to map to {@link
   *     BenefitComponent#getType()}
   * @param value Integral amount to map to {@link BenefitComponent#getUsedUnsignedIntType()}
   * @return the new {@link BenefitComponent} which will have already been added to the appropriate
   *     {@link ExplanationOfBenefit#getBenefitBalance()} entry. Returns Empty if the amount wasn't
   *     set.
   */
  static Optional<BenefitComponent> addBenefitBalanceFinancialMedicalInt(
      ExplanationOfBenefit eob, CcwCodebookInterface financialType, Optional<BigDecimal> value) {
    return value.map(val -> addBenefitBalanceFinancialMedicalInt(eob, financialType, value.get()));
  }

  /**
   * Finds or adds a benefit balance component to the specified {@link ExplanationOfBenefit}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link BenefitComponent} should be part of
   * @param benefitCategory the {@link BenefitCategory} to map to {@link
   *     BenefitBalanceComponent#getCategory()}
   * @return the already-existing {@link BenefitBalanceComponent} that matches the specified
   *     parameters, or the newly added one
   */
  private static BenefitBalanceComponent findOrAddBenefitBalance(
      ExplanationOfBenefit eob, ExBenefitcategory benefitCategory) {

    Optional<BenefitBalanceComponent> matchingBenefitBalance =
        eob.getBenefitBalance().stream()
            .filter(
                bb ->
                    isCodeInConcept(
                        bb.getCategory(), benefitCategory.getSystem(), benefitCategory.toCode()))
            .findAny();

    // Found an existing BenefitBalance that matches the coding system
    if (matchingBenefitBalance.isPresent()) {
      return matchingBenefitBalance.get();
    }

    CodeableConcept benefitCategoryConcept = new CodeableConcept();
    benefitCategoryConcept
        .addCoding()
        .setSystem(benefitCategory.getSystem())
        .setCode(benefitCategory.toCode())
        .setDisplay(benefitCategory.getDisplay());

    BenefitBalanceComponent newBenefitBalance = new BenefitBalanceComponent(benefitCategoryConcept);
    eob.addBenefitBalance(newBenefitBalance);

    return newBenefitBalance;
  }

  /**
   * Optionally adds a member to {@link ExplanationOfBenefit#getCareTeam()}.
   *
   * <p>Used for Institutional claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param item the {@link ItemComponent} that should be linked to the {@link CareTeamComponent}
   * @param type the type to use
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @return the optional
   */
  static Optional<CareTeamComponent> addCareTeamMember(
      ExplanationOfBenefit eob,
      ItemComponent item,
      C4BBPractitionerIdentifierType type,
      C4BBClaimInstitutionalCareTeamRole role,
      Optional<String> id) {
    return id.map(
        i ->
            addCareTeamPractitioner(
                eob, item, type, i, role.getSystem(), role.toCode(), role.getDisplay()));
  }

  /**
   * Adds a member to {@link ExplanationOfBenefit#getCareTeam()}.
   *
   * <p>Used for Institutional claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param type the type to use
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @return the optional
   */
  static Optional<CareTeamComponent> addCareTeamMember(
      ExplanationOfBenefit eob,
      C4BBPractitionerIdentifierType type,
      C4BBClaimInstitutionalCareTeamRole role,
      Optional<String> id) {
    return addCareTeamMember(eob, null, type, role, id);
  }

  /**
   * Optionally adds a member to {@link ExplanationOfBenefit#getCareTeam()}.
   *
   * <p>Used for Pharmacy claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param item the {@link ItemComponent} that should be linked to the {@link CareTeamComponent}
   * @param type the type to use
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @return the optional
   */
  static Optional<CareTeamComponent> addCareTeamMember(
      ExplanationOfBenefit eob,
      ItemComponent item,
      C4BBPractitionerIdentifierType type,
      C4BBClaimPharmacyTeamRole role,
      Optional<String> id) {
    return id.map(
        i ->
            addCareTeamPractitioner(
                eob, item, type, i, role.getSystem(), role.toCode(), role.getDisplay()));
  }

  /**
   * Adds a member to {@link ExplanationOfBenefit#getCareTeam()}.
   *
   * <p>Used for Professional and Non-Clinician claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param item the {@link ItemComponent} that should be linked to the {@link CareTeamComponent}
   * @param type the type to use
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @return the optional
   */
  static Optional<CareTeamComponent> addCareTeamMember(
      ExplanationOfBenefit eob,
      ItemComponent item,
      C4BBPractitionerIdentifierType type,
      C4BBClaimProfessionalAndNonClinicianCareTeamRole role,
      Optional<String> id) {
    return id.map(
        i ->
            addCareTeamPractitioner(
                eob, item, type, i, role.getSystem(), role.toCode(), role.getDisplay()));
  }

  /**
   * Adds a member to {@link ExplanationOfBenefit#getCareTeam() with npiOrgDisplay}.
   *
   * <p>Used for Professional and Non-Clinician claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param item the Item component
   * @param type to use, either NPI or UPIN
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @param npiOrgDisplay The NPI display as a optional string
   * @return the care team component that was added
   */
  static CareTeamComponent addCareTeamMemberWithNpiOrg(
      ExplanationOfBenefit eob,
      ItemComponent item,
      C4BBPractitionerIdentifierType type,
      C4BBClaimProfessionalAndNonClinicianCareTeamRole role,
      String id,
      Optional<String> npiOrgDisplay) {

    CareTeamComponent careTeamEntry =
        addCareTeamPractitionerWithNpiOrg(
            eob, type, id, role.getSystem(), role.toCode(), role.getDisplay(), npiOrgDisplay);

    // care team entry is at eob level so no need to create item link id
    if (item == null) {
      return careTeamEntry;
    }

    // ExplanationOfBenefit.careTeam.sequence => ExplanationOfBenefit.item.careTeamSequence
    if (!item.getCareTeamSequence().contains(new PositiveIntType(careTeamEntry.getSequence()))) {
      item.addCareTeamSequence(careTeamEntry.getSequence());
    }

    return careTeamEntry;
  }

  /**
   * Adds a member to {@link ExplanationOfBenefit#getCareTeam()}.
   *
   * <p>Used for Professional and Non-Clinician claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param type the type to use
   * @param role The care team member's role
   * @param id The NPI or UPIN coded as a string
   * @return the optional
   */
  static Optional<CareTeamComponent> addCareTeamMember(
      ExplanationOfBenefit eob,
      C4BBPractitionerIdentifierType type,
      C4BBClaimProfessionalAndNonClinicianCareTeamRole role,
      Optional<String> id) {
    return addCareTeamMember(eob, null, type, role, id);
  }

  /**
   * Handles mapping the following values to the appropriate member of {@link
   * ExplanationOfBenefit#getCareTeam()}. This updates the passed in {@link ExplanationOfBenefit} in
   * place.
   *
   * <p>For Institutional claims
   *
   * @param eob the {@link ExplanationOfBenefit} that the {@link CareTeamComponent} should be part
   *     of
   * @param attendingPhysicianNpi AT_PHYSN_NPI
   * @param operatingPhysicianNpi OP_PHYSN_NPI
   * @param otherPhysicianNpi OT_PHYSN_NPI
   * @param attendingPhysicianUpin AT_PHYSN_UPIN
   * @param operatingPhysicianUpin OP_PHYSN_UPIN
   * @param otherPhysicianUpin OT_PHYSN_UPIN
   */
  static void mapCareTeam(
      ExplanationOfBenefit eob,
      Optional<String> attendingPhysicianNpi,
      Optional<String> operatingPhysicianNpi,
      Optional<String> otherPhysicianNpi,
      Optional<String> attendingPhysicianUpin,
      Optional<String> operatingPhysicianUpin,
      Optional<String> otherPhysicianUpin) {

    // AT_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimInstitutionalCareTeamRole.ATTENDING,
        attendingPhysicianNpi);

    // AT_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.UPIN,
        C4BBClaimInstitutionalCareTeamRole.ATTENDING,
        attendingPhysicianUpin);

    // OP_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimInstitutionalCareTeamRole.OPERATING,
        operatingPhysicianNpi);

    // OP_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.UPIN,
        C4BBClaimInstitutionalCareTeamRole.OPERATING,
        operatingPhysicianUpin);

    // OT_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimInstitutionalCareTeamRole.OTHER_OPERATING,
        otherPhysicianNpi);

    // OT_PHYSN_UPIN => ExplanationOfBenefit.careTeam.provider
    addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.UPIN,
        C4BBClaimInstitutionalCareTeamRole.OTHER_OPERATING,
        otherPhysicianUpin);
  }

  /**
   * Transforms the common group level data elements between the {@link InpatientClaim} {@link
   * OutpatientClaim} and {@link SNFClaim} claim types to FHIR.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param bloodDeductibleLiabilityAmount NCH_BENE_BLOOD_DDCTBL_LBLTY_AM
   * @param mcoPaidSw CLM_MCO_PD_SW
   */
  static void mapEobCommonGroupInpOutSNF(
      ExplanationOfBenefit eob,
      BigDecimal bloodDeductibleLiabilityAmount,
      Optional<Character> mcoPaidSw) {

    // NCH_BENE_BLOOD_DDCTBL_LBLTY_AM => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(
        eob, CcwCodebookVariable.NCH_BENE_BLOOD_DDCTBL_LBLTY_AM, bloodDeductibleLiabilityAmount);

    // CLM_MCO_PD_SW => ExplanationOfBenefit.supportingInfo.code
    if (mcoPaidSw.isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob, CcwCodebookVariable.CLM_MCO_PD_SW, CcwCodebookVariable.CLM_MCO_PD_SW, mcoPaidSw);
    }
  }

  /**
   * Transforms the common group level data elements between the {@link InpatientClaimLine} {@link
   * OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine} and {@link SNFClaimLine}
   * claim types to FHIR. The method parameter fields from {@link InpatientClaimLine} {@link
   * OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine} and {@link SNFClaimLine} are
   * listed below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * InpatientClaimColumn} {@link OutpatientClaimColumn} {@link HospiceClaimColumn} {@link
   * HHAClaimColumn}* and {@link SNFClaimColumn}).
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param organizationNpi ORG_NPI_NUM
   * @param npiOrgName the npi org name
   * @param claimFacilityTypeCode CLM_FAC_TYPE_CD
   * @param claimFrequencyCode CLM_FREQ_CD
   * @param claimNonPaymentReasonCode CLM_MDCR_NON_PMT_RSN_CD
   * @param patientDischargeStatusCode PTNT_DSCHRG_STUS_CD
   * @param claimServiceClassificationTypeCode CLM_SRVC_CLSFCTN_TYPE_CD
   * @param claimPrimaryPayerCode NCH_PRMRY_PYR_CD
   * @param totalChargeAmount CLM_TOT_CHRG_AMT
   * @param primaryPayerPaidAmount NCH_PRMRY_PYR_CLM_PD_AMT
   * @param fiscalIntermediaryNumber FI_NUM
   * @param lastUpdated the last updated
   * @param fiDocClmControlNum FI_DOC_CLM_CNTL_NUM
   * @param fiClmProcDt FI_CLM_PROC_DT
   * @param c4bbInstutionalClaimSubtype the {@link C4BBbInstutionalClaimSubtype} that is passed in
   * @param claimQueryCode the CLAIM_QUERY_CODE
   */
  static void mapEobCommonGroupInpOutHHAHospiceSNF(
      ExplanationOfBenefit eob,
      Optional<String> organizationNpi,
      Optional<String> npiOrgName,
      char claimFacilityTypeCode,
      char claimFrequencyCode,
      Optional<String> claimNonPaymentReasonCode,
      String patientDischargeStatusCode,
      char claimServiceClassificationTypeCode,
      Optional<Character> claimPrimaryPayerCode,
      BigDecimal totalChargeAmount,
      BigDecimal primaryPayerPaidAmount,
      Optional<String> fiscalIntermediaryNumber,
      Optional<Instant> lastUpdated,
      Optional<String> fiDocClmControlNum,
      Optional<LocalDate> fiClmProcDt,
      C4BBInstutionalClaimSubtypes c4bbInstutionalClaimSubtype,
      Optional<Character> claimQueryCode) {

    // CLAIM_QUERY_CODE => ExplanationOfBenefit.billablePeriod.extension
    claimQueryCode.ifPresent(
        queryCode ->
            eob.getBillablePeriod()
                .addExtension(
                    createExtensionCoding(eob, CcwCodebookVariable.CLAIM_QUERY_CD, queryCode)));

    // FI_DOC_CLM_CNTL_NUM => ExplanationOfBenefit.extension
    fiDocClmControlNum.ifPresent(
        cntlNum ->
            eob.addExtension(
                createExtensionIdentifier(
                    CcwCodebookMissingVariable.FI_DOC_CLM_CNTL_NUM, cntlNum)));

    // FI_CLM_PROC_DT => ExplanationOfBenefit.extension
    fiClmProcDt.ifPresent(
        procDt ->
            eob.addExtension(
                TransformerUtilsV2.createExtensionDate(
                    CcwCodebookVariable.FI_CLM_PROC_DT, procDt)));

    // ORG_NPI_NUM => ExplanationOfBenefit.provider
    addProviderSlice(
        eob, C4BBOrganizationIdentifierType.NPI, organizationNpi, npiOrgName, lastUpdated);

    // CLM_FAC_TYPE_CD => ExplanationOfBenefit.facility.extension
    eob.getFacility()
        .addExtension(
            createExtensionCoding(eob, CcwCodebookVariable.CLM_FAC_TYPE_CD, claimFacilityTypeCode));

    // CLM_FREQ_CD => ExplanationOfBenefit.supportingInfo
    addInformationSliceWithCode(
        eob,
        C4BBSupportingInfoType.TYPE_OF_BILL,
        CcwCodebookVariable.CLM_FREQ_CD,
        CcwCodebookVariable.CLM_FREQ_CD,
        claimFrequencyCode);

    // CLM_MDCR_NON_PMT_RSN_CD => ExplanationOfBenefit.extension
    if (claimNonPaymentReasonCode.isPresent()) {
      eob.addExtension(
          createExtensionCoding(
              eob, CcwCodebookVariable.CLM_MDCR_NON_PMT_RSN_CD, claimNonPaymentReasonCode));
    }

    // PTNT_DSCHRG_STUS_CD => ExplanationOfBenefit.supportingInfo
    if (!patientDischargeStatusCode.isEmpty()) {
      addInformationSliceWithCode(
          eob,
          C4BBSupportingInfoType.DISCHARGE_STATUS,
          CcwCodebookVariable.PTNT_DSCHRG_STUS_CD,
          CcwCodebookVariable.PTNT_DSCHRG_STUS_CD,
          patientDischargeStatusCode);
    }

    // CLM_SRVC_CLSFCTN_TYPE_CD => ExplanationOfBenefit.extension
    eob.addExtension(
        createExtensionCoding(
            eob, CcwCodebookVariable.CLM_SRVC_CLSFCTN_TYPE_CD, claimServiceClassificationTypeCode));

    // NCH_PRMRY_PYR_CD => ExplainationOfBenefit.supportingInfo
    if (claimPrimaryPayerCode.isPresent()) {
      addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PRMRY_PYR_CD,
          CcwCodebookVariable.NCH_PRMRY_PYR_CD,
          claimPrimaryPayerCode.get());
    }

    // FI_NUM => ExplanationOfBenefit.extension
    if (fiscalIntermediaryNumber.isPresent()) {
      eob.addExtension(
          createExtensionCoding(eob, CcwCodebookVariable.FI_NUM, fiscalIntermediaryNumber));
    }

    // CLM_TOT_CHRG_AMT => ExplainationOfBenefit.total
    addTotal(
        eob,
        createTotalAdjudicationAmountSlice(
            eob,
            CcwCodebookVariable.CLM_TOT_CHRG_AMT,
            C4BBAdjudication.SUBMITTED,
            totalChargeAmount));

    // NCH_PRMRY_PYR_CLM_PD_AMT => ExplanationOfBenefit.benefitBalance.financial
    addBenefitBalanceFinancialMedicalAmt(eob, CcwCodebookVariable.PRPAYAMT, primaryPayerPaidAmount);

    eob.setSubType(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(TransformerConstants.C4BB_Institutional_Claim_SubType)
                    .setCode(c4bbInstutionalClaimSubtype.label))
            .setText(c4bbInstutionalClaimSubtype.name()));
  }

  /**
   * Transforms the common item level data elements between the {@link CarrierClaimLine} and {@link
   * DMEClaimLine}* claim types to FHIR. The method parameter fields from {@link CarrierClaimLine}
   * and {@link DMEClaimLine} are listed below and their corresponding RIF CCW fields (denoted in
   * all CAPS below from {@link CarrierClaimColumn} and {@link DMEClaimColumn}).
   *
   * @param item the {@link ItemComponent} to modify
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param claimId CLM_ID,
   * @param sequence the sequence
   * @param serviceCount LINE_SRVC_CNT,
   * @param placeOfServiceCode LINE_PLACE_OF_SRVC_CD,
   * @param firstExpenseDate LINE_1ST_EXPNS_DT,
   * @param lastExpenseDate LINE_LAST_EXPNS_DT,
   * @param beneficiaryPaymentAmount LINE_BENE_PMT_AMT,
   * @param providerPaymentAmount LINE_PRVDR_PMT_AMT,
   * @param beneficiaryPartBDeductAmount LINE_BENE_PTB_DDCTBL_AMT,
   * @param primaryPayerCode LINE_BENE_PRMRY_PYR_CD,
   * @param primaryPayerPaidAmount LINE_BENE_PRMRY_PYR_PD_AMT,
   * @param betosCode BETOS_CD,
   * @param paymentAmount LINE_NCH_PMT_AMT,
   * @param paymentCode LINE_PMT_80_100_CD,
   * @param coinsuranceAmount LINE_COINSRNC_AMT,
   * @param submittedChargeAmount LINE_SBMTD_CHRG_AMT,
   * @param allowedChargeAmount LINE_ALOWD_CHRG_AMT,
   * @param processingIndicatorCode LINE_PRCSG_IND_CD,
   * @param serviceDeductibleCode LINE_SERVICE_DEDUCTIBLE,
   * @param hctHgbTestTypeCode LINE_HCT_HGB_TYPE_CD
   * @param hctHgbTestResult LINE_HCT_HGB_RSLT_NUM,
   * @param cmsServiceTypeCode LINE_CMS_TYPE_SRVC_CD,
   * @param nationalDrugCode LINE_NDC_CD,
   * @param drugCode the drug code
   * @return the {@link ItemComponent}
   */
  static ItemComponent mapEobCommonItemCarrierDME(
      ItemComponent item,
      ExplanationOfBenefit eob,
      Long claimId,
      int sequence,
      BigDecimal serviceCount,
      String placeOfServiceCode,
      Optional<LocalDate> firstExpenseDate,
      Optional<LocalDate> lastExpenseDate,
      BigDecimal beneficiaryPaymentAmount,
      BigDecimal providerPaymentAmount,
      BigDecimal beneficiaryPartBDeductAmount,
      Optional<Character> primaryPayerCode,
      BigDecimal primaryPayerPaidAmount,
      Optional<String> betosCode,
      BigDecimal paymentAmount,
      Optional<Character> paymentCode,
      BigDecimal coinsuranceAmount,
      BigDecimal submittedChargeAmount,
      BigDecimal allowedChargeAmount,
      Optional<String> processingIndicatorCode,
      Optional<Character> serviceDeductibleCode,
      Optional<String> hctHgbTestTypeCode,
      BigDecimal hctHgbTestResult,
      char cmsServiceTypeCode,
      Optional<String> nationalDrugCode,
      String drugCode) {

    // LINE_SRVC_CNT => ExplanationOfBenefit.item.quantity
    item.setQuantity(new SimpleQuantity().setValue(serviceCount));

    // LINE_CMS_TYPE_SRVC_CD => ExplanationOfBenefit.item.category
    item.setCategory(
        createCodeableConcept(eob, CcwCodebookVariable.LINE_CMS_TYPE_SRVC_CD, cmsServiceTypeCode));

    // LINE_PLACE_OF_SRVC_CD => ExplanationOfBenefit.item.location
    item.setLocation(
        createCodeableConcept(eob, CcwCodebookVariable.LINE_PLACE_OF_SRVC_CD, placeOfServiceCode));

    // BETOS_CD => ExplanationOfBenefit.item.extension
    betosCode.ifPresent(
        code -> item.addExtension(createExtensionCoding(eob, CcwCodebookVariable.BETOS_CD, code)));

    // LINE_1ST_EXPNS_DT => ExplanationOfBenefit.item.servicedPeriod
    // LINE_LAST_EXPNS_DT => ExplanationOfBenefit.item.servicedPeriod
    if (firstExpenseDate.isPresent() && lastExpenseDate.isPresent()) {
      validatePeriodDates(firstExpenseDate, lastExpenseDate);

      item.setServiced(
          new Period()
              .setStart((convertToDate(firstExpenseDate.get())), TemporalPrecisionEnum.DAY)
              .setEnd((convertToDate(lastExpenseDate.get())), TemporalPrecisionEnum.DAY));
    }
    // If we only have one, set servicedDate
    // LINE_1ST_EXPNS_DT => ExplanationOfBenefit.item.servicedDate
    else if (firstExpenseDate.isPresent()) {
      item.setServiced(new DateType(convertToDate(firstExpenseDate.get())));
    }

    // LINE_NCH_PMT_AMT => ExplanationOfBenefit.item.adjudication
    Optional<AdjudicationComponent> nchPmtAmtAdjudication =
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_NCH_PMT_AMT, C4BBAdjudication.BENEFIT, paymentAmount);
    addAdjudication(item, nchPmtAmtAdjudication);

    // LINE_PMT_80_100_CD => ExplanationOfBenefit.item.adjudication.extension
    if (paymentCode.isPresent() && nchPmtAmtAdjudication.isPresent()) {
      nchPmtAmtAdjudication
          .get()
          .addExtension(
              createExtensionCoding(eob, CcwCodebookVariable.LINE_PMT_80_100_CD, paymentCode));
    }

    // PAID_TO_PATIENT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_BENE_PMT_AMT,
            C4BBAdjudication.PAID_TO_PATIENT,
            beneficiaryPaymentAmount));

    // LINE_PRVDR_PMT_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_PRVDR_PMT_AMT,
            C4BBAdjudication.PAID_TO_PROVIDER,
            providerPaymentAmount));

    // LINE_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_BENE_PTB_DDCTBL_AMT,
            C4BBAdjudication.DEDUCTIBLE,
            beneficiaryPartBDeductAmount));

    // LINE_BENE_PRMRY_PYR_CD => ExplanationOfBenefit.item.extension
    primaryPayerCode.ifPresent(
        code ->
            item.addExtension(
                createExtensionCoding(eob, CcwCodebookVariable.LINE_BENE_PRMRY_PYR_CD, code)));

    // LINE_BENE_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_BENE_PRMRY_PYR_PD_AMT,
            C4BBAdjudication.PRIOR_PAYER_PAID,
            primaryPayerPaidAmount));

    // LINE_COINSRNC_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_COINSRNC_AMT,
            C4BBAdjudication.COINSURANCE,
            coinsuranceAmount));

    // LINE_SBMTD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_SBMTD_CHRG_AMT,
            C4BBAdjudication.SUBMITTED,
            submittedChargeAmount));

    // LINE_ALOWD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.LINE_ALOWD_CHRG_AMT,
            C4BBAdjudication.ELIGIBLE,
            allowedChargeAmount));

    // LINE_BENE_PRMRY_PYR_CD => ExplanationOfBenefit.item.extension
    processingIndicatorCode.ifPresent(
        code ->
            item.addExtension(
                createExtensionCoding(eob, CcwCodebookVariable.LINE_PRCSG_IND_CD, code)));

    // LINE_SERVICE_DEDUCTIBLE => ExplanationOfBenefit.item.extension
    serviceDeductibleCode.ifPresent(
        code ->
            item.addExtension(
                createExtensionCoding(eob, CcwCodebookVariable.LINE_SERVICE_DEDUCTIBLE, code)));

    // LINE_HCT_HGB_TYPE_CD => Observation.code
    // LINE_HCT_HGB_RSLT_NUM => Observation.value
    if (hctHgbTestTypeCode.isPresent()) {
      String observationId = "line-observation-" + sequence;
      String observationRef = "#" + observationId;

      // The `item` will link to a `supportingInfo` that references the embedded Observation
      SupportingInformationComponent comp =
          addInformation(eob, CcwCodebookVariable.LINE_HCT_HGB_RSLT_NUM);
      comp.setValue(new Reference(observationRef));

      // Create embedded Observation in ExplanationOfBenefit.contained
      Observation hctHgbObservation = findOrCreateContainedObservation(eob, observationId);
      hctHgbObservation.setStatus(ObservationStatus.UNKNOWN);
      hctHgbObservation.setCode(
          createCodeableConcept(eob, CcwCodebookVariable.LINE_HCT_HGB_TYPE_CD, hctHgbTestTypeCode));
      hctHgbObservation.setValue(new Quantity().setValue(hctHgbTestResult));

      // Link item.informationSequence to the new `supportingInfo` entry
      item.addInformationSequence(comp.getSequence());
    }

    // LINE_NDC_CD => ExplanationOfBenefit.item.productOrService.extension
    addNationalDrugCode(item, nationalDrugCode, drugCode);

    return item;
  }

  /**
   * Generically attempts to retrieve a procedure from the current claim.
   *
   * @param procedure Procedure accessors all follow the same pattern except for an integer
   *     difference. This value is used as a substitution when looking up the method name.
   * @param codes The mapping of procedure codes by their property name and respective value
   * @param codeVersions The mapping of procedure code versions by their property name and
   *     respective value
   * @param dates The mapping of procedure dates by their property name and respective value
   * @return a {@link CCWProcedure} or {@link Optional#empty()}
   */
  public static Optional<CCWProcedure> extractCCWProcedure(
      int procedure,
      Map<String, Optional<String>> codes,
      Map<String, Optional<Character>> codeVersions,
      Map<String, Optional<LocalDate>> dates) {
    Optional<String> code =
        codes.getOrDefault(String.format("procedure%dCode", procedure), Optional.empty());
    Optional<Character> codeVersion =
        codeVersions.getOrDefault(
            String.format("procedure%dCodeVersion", procedure), Optional.empty());
    Optional<LocalDate> date =
        dates.getOrDefault(String.format("procedure%dDate", procedure), Optional.empty());
    return CCWProcedure.from(code, codeVersion, date);
  }

  /**
   * Generically attempts to retrieve the procedures from the current claim.
   *
   * @param codes The mapping of procedure codes by their property name and respective value
   * @param codeVersions The mapping of procedure code versions by their property name and
   *     respective value
   * @param dates The mapping of procedure dates by their property name and respective value
   * @return a list of {@link CCWProcedure}
   */
  public static List<CCWProcedure> extractCCWProcedures(
      Map<String, Optional<String>> codes,
      Map<String, Optional<Character>> codeVersions,
      Map<String, Optional<LocalDate>> dates) {
    // Handle Procedures
    // ICD_PRCDR_CD(1-25)        => ExplanationOfBenefit.procedure.procedureCodableConcept
    // ICD_PRCDR_VRSN_CD(1-25)   => ExplanationOfBenefit.procedure.procedureCodableConcept
    // PRCDR_DT(1-25)            => ExplanationOfBenefit.procedure.date
    final int FIRST_PROCEDURE = 1;
    final int LAST_PROCEDURE = 25;
    return IntStream.range(FIRST_PROCEDURE, LAST_PROCEDURE + 1)
        .mapToObj(i -> TransformerUtilsV2.extractCCWProcedure(i, codes, codeVersions, dates))
        .filter(p -> p.isPresent())
        .map(p -> p.get())
        .toList();
  }

  /**
   * Adds a procedure code to the specified {@link ExplanationOfBenefit} if it does not exist.
   *
   * @param eob the {@link ExplanationOfBenefit} to (possibly) modify
   * @param procedure the {@link CCWProcedure} to add, if it's not already present
   * @return the {@link ProcedureComponent#getSequence()} of the existing or newly-added entry
   */
  static int addProcedureCode(ExplanationOfBenefit eob, CCWProcedure procedure) {
    Optional<ProcedureComponent> existingProcedure =
        eob.getProcedure().stream()
            .filter(pc -> pc.getProcedure() instanceof CodeableConcept)
            .filter(
                pc ->
                    isCodeInConcept(
                        (CodeableConcept) pc.getProcedure(),
                        procedure.getFhirSystem(),
                        procedure.getCode()))
            .findAny();

    if (existingProcedure.isPresent()) {
      return existingProcedure.get().getSequenceElement().getValue();
    }

    ProcedureComponent procedureComponent =
        new ProcedureComponent()
            .setSequence(eob.getProcedure().size() + 1)
            .setProcedure(
                createCodeableConcept(
                    procedure.getFhirSystem(),
                    null,
                    CommonTransformerUtils.retrieveProcedureCodeDisplay(procedure.getCode()),
                    procedure.getCode()));

    if (procedure.getProcedureDate().isPresent()) {
      procedureComponent.setDate(convertToDate(procedure.getProcedureDate().get()));
    }

    eob.getProcedure().add(procedureComponent);

    return procedureComponent.getSequenceElement().getValue();
  }

  /**
   * Adds an {@link ItemComponent} to the passed in {@link ExplanationOfBenefit}. It is added to the
   * end of the list and the Sequence is set appropriately.
   *
   * @param eob The {@link ExplanationOfBenefit} to add the {@link ItemComponent} to
   * @return The newly created {@link ItemComponent}
   */
  static ItemComponent addItem(ExplanationOfBenefit eob) {
    // addItem adds and returns, so we want size() not size() + 1 here
    return eob.addItem().setSequence(eob.getItem().size());
  }

  /**
   * Looks for an {@link Observation} with the given resource ID in {@link
   * ExplanationOfBenefit#getContained()} or adds one if it doesn't exist.
   *
   * @param eob the eob to search in and add to
   * @param id the id to find/add
   * @return the observation that was added or existed
   */
  static Observation findOrCreateContainedObservation(ExplanationOfBenefit eob, String id) {
    Optional<Resource> observation =
        eob.getContained().stream().filter(r -> r.getId().equals(id)).findFirst();

    // If it isn't there, add one
    if (observation.isEmpty()) {
      observation = Optional.of(new Observation().setId(id));
      eob.getContained().add(observation.get());
    }

    // At this point `observation.get()` will always return
    if (!(observation.get() instanceof Observation)) {
      throw new BadCodeMonkeyException();
    }

    return (Observation) observation.get();
  }

  /**
   * Looks for an {@link Organization} with the given resource ID in {@link
   * ExplanationOfBenefit#getContained()} or adds one if it doesn't exist.
   *
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param id The resource ID
   * @return The found or new {@link Organization} resource
   */
  static Organization findOrCreateContainedOrganization(ExplanationOfBenefit eob, String id) {
    Optional<Resource> organization =
        eob.getContained().stream().filter(r -> r.getId() == id).findFirst();

    // If it isn't there, add one
    if (!organization.isPresent()) {
      organization = Optional.of(new Organization().setId(id));
      organization.get().getMeta().addProfile(ProfileConstants.C4BB_ORGANIZATION_URL);
      eob.getContained().add(organization.get());
    }

    // At this point `organization.get()` will always return
    if (!(organization.get() instanceof Organization)) {
      throw new BadCodeMonkeyException();
    }

    return (Organization) organization.get();
  }

  /**
   * Looks up or adds a contained {@link Organization} object to the current {@link
   * ExplanationOfBenefit}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param eob The {@link ExplanationOfBenefit} to provider org details to
   * @param type The {@link C4BBIdentifierType} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   * @param lastUpdated the last updated value to use for the slice
   */
  static void addProviderSlice(
      ExplanationOfBenefit eob,
      C4BBOrganizationIdentifierType type,
      String value,
      Optional<Instant> lastUpdated) {
    addProviderSlice(eob, type, Optional.of(value), Optional.empty(), lastUpdated);
  }

  /**
   * Looks up or adds a contained {@link Organization} object to the current {@link
   * ExplanationOfBenefit}*. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param eob The {@link ExplanationOfBenefit} to provider org details to
   * @param type The {@link C4BBIdentifierType} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   * @param npiOrgName the npi org name
   * @param lastUpdated the last updated to use for the slice
   */
  static void addProviderSlice(
      ExplanationOfBenefit eob,
      C4BBOrganizationIdentifierType type,
      Optional<String> value,
      Optional<String> npiOrgName,
      Optional<Instant> lastUpdated) {
    if (value.isPresent()) {
      Organization organization = findOrCreateContainedOrganization(eob, PROVIDER_ORG_ID);

      // Add the new Identifier to the Organization
      Identifier id =
          new Identifier()
              .setType(createCodeableConcept(type.getSystem(), type.toCode()))
              .setValue(value.get());

      // Certain types have specific systems
      if (type == C4BBOrganizationIdentifierType.NPI) {
        id.setSystem(TransformerConstants.CODING_NPI_US);
        if (!npiOrgName.isEmpty()) {
          organization.setName(npiOrgName.get());
        } else {
          organization.setName(NPI_ORG_DISPLAY_DEFAULT);
          /*if (value.isPresent())
            LOGGER.info("Organization not found for npi number:" + value.get());
          else LOGGER.info("Organization not found for empty npi number");*/
        }
      }

      organization.addIdentifier(id);

      // Set active to value of true
      organization.setActive(true);

      setLastUpdated(organization, lastUpdated);

      // This gets updated for every call, but always set to the same value
      eob.getProvider().setReference(PROVIDER_ORG_REFERENCE);
    }
  }

  /**
   * Convenience function when passing non-optional values.
   *
   * @param eob The {@link ExplanationOfBenefit} to provider org details to
   * @param type The {@link C4BBIdentifierType} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   * @param npiOrgName the npi org name
   * @param lastupdated the last updated to use for the slice
   */
  static void addProviderSlice(
      ExplanationOfBenefit eob,
      C4BBOrganizationIdentifierType type,
      String value,
      Optional<String> npiOrgName,
      Optional<Instant> lastupdated) {
    addProviderSlice(eob, type, Optional.of(value), npiOrgName, lastupdated);
  }

  /**
   * Maps a hcpcs {@link CodeableConcept} and any applicable modifiders to the given {@link
   * ItemComponent}.
   *
   * @param eob the {@link ExplanationOfBenefit} that the HCPCS code is being mapped into
   * @param item the {@link ItemComponent} that the HCPCS code is being mapped into
   * @param hcpcsCode the hcpcs code to add a code to if passed
   * @param year the {@link CcwCodebookVariable#CARR_CLM_HCPCS_YR_CD} identifying the HCPCS code
   *     version in use
   * @param modifiers the {@link CcwCodebookVariable#HCPCS_1ST_MDFR_CD}, etc. values to be mapped
   *     (if any)
   */
  static void mapHcpcs(
      ExplanationOfBenefit eob,
      ItemComponent item,
      Optional<String> hcpcsCode,
      Optional<Character> year,
      List<Optional<String>> modifiers) {

    hcpcsCode.ifPresent(
        code ->
            item.setProductOrService(
                createCodeableConcept(TransformerConstants.CODING_SYSTEM_HCPCS, code)));

    for (Optional<String> hcpcsModifier : modifiers) {
      if (hcpcsModifier.isPresent()) {
        CodeableConcept modifier =
            createCodeableConcept(TransformerConstants.CODING_SYSTEM_HCPCS, hcpcsModifier.get());

        // Set Coding.version for all of the mappings, if it's available.
        if (year.isPresent()) {
          // Note: Only CARRIER and DME claims have the year/version field.
          modifier.getCodingFirstRep().setVersion(year.get().toString());
        }

        item.addModifier(modifier);
      }
    }
  }

  /**
   * Transforms the common item level data elements between the {@link InpatientClaimLine} {@link
   * OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link SNFClaimLine}
   * claim types to FHIR. The method parameter fields from {@link InpatientClaimLine} {@link
   * OutpatientClaimLine} {@link HospiceClaimLine} {@link HHAClaimLine}and {@link SNFClaimLine} are
   * listed below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * InpatientClaimColumn} {@link OutpatientClaimColumn} {@link HospiceClaimColumn} {@link
   * HHAClaimColumn} and {@link SNFClaimColumn}).
   *
   * @param item the {@link ItemComponent} to modify
   * @param eob the {@link ExplanationOfBenefit} to modify
   * @param revenueCenterCode REV_CNTR,
   * @param rateAmount REV_CNTR_RATE_AMT,
   * @param totalChargeAmount REV_CNTR_TOT_CHRG_AMT,
   * @param nonCoveredChargeAmount REV_CNTR_NCVRD_CHRG_AMT,
   * @param nationalDrugCodeQuantity REV_CNTR_NDC_QTY,
   * @param nationalDrugCodeQualifierCode REV_CNTR_NDC_QTY_QLFR_CD,
   * @param unitCount REV_CNTR_UNIT_CNT,
   * @return the {@link ItemComponent}
   */
  static ItemComponent mapEobCommonItemRevenue(
      ItemComponent item,
      ExplanationOfBenefit eob,
      String revenueCenterCode,
      BigDecimal rateAmount,
      BigDecimal totalChargeAmount,
      Optional<BigDecimal> nonCoveredChargeAmount,
      Optional<BigDecimal> nationalDrugCodeQuantity,
      Optional<String> nationalDrugCodeQualifierCode,
      BigDecimal unitCount) {

    // REV_CNTR => ExplanationOfBenefit.item.revenue
    item.setRevenue(createCodeableConcept(eob, CcwCodebookVariable.REV_CNTR, revenueCenterCode));

    // REV_CNTR_RATE_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.REV_CNTR_RATE_AMT, C4BBAdjudication.SUBMITTED, rateAmount));

    // REV_CNTR_TOT_CHRG_AMT => ExplanationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.REV_CNTR_TOT_CHRG_AMT,
            C4BBAdjudication.SUBMITTED,
            totalChargeAmount));

    // REV_CNTR_NCVRD_CHRG_AMT => ExplanationOfBenefit.item.adjudication
    if (nonCoveredChargeAmount.isPresent()) {
      addAdjudication(
          item,
          createAdjudicationAmtSlice(
              CcwCodebookVariable.REV_CNTR_NCVRD_CHRG_AMT,
              C4BBAdjudication.NONCOVERED,
              nonCoveredChargeAmount));
    }

    // REV_CNTR_NDC_QTY_QLFR_CD => ExplanationOfBenefit.item.modifier
    if (nationalDrugCodeQualifierCode.isPresent()) {
      item.getModifier()
          .add(
              TransformerUtilsV2.createCodeableConcept(
                  eob,
                  CcwCodebookVariable.REV_CNTR_NDC_QTY_QLFR_CD,
                  nationalDrugCodeQualifierCode));
    }

    // REV_CNTR_NDC_QTY => ExplanationOfBenefit.item.quantity
    if (nationalDrugCodeQuantity.isPresent()) {
      Extension drugQuantityExtension =
          createExtensionQuantity(CcwCodebookVariable.REV_CNTR_NDC_QTY, nationalDrugCodeQuantity);
      Quantity drugQuantity = (Quantity) drugQuantityExtension.getValue();
      item.setQuantity(drugQuantity);
    }

    // REV_CNTR_UNIT_CNT => ExplanationOfBenefit.item.extension.valueQuantity
    if (unitCount != null && unitCount.compareTo(BigDecimal.ZERO) != 0) {
      Extension unitCountExtension =
          createExtensionQuantity(CcwCodebookMissingVariable.REV_CNTR_UNIT_CNT, unitCount);
      item.addExtension(unitCountExtension);
    }

    return item;
  }

  /**
   * Maps the Revenue Status Indicator Code to the eob's item revenue as an extension, if the status
   * code is present.
   *
   * <p>REV_CNTR_STUS_IND_CD => ExplanationOfBenefit.item.revenue.extension
   *
   * @param item the item to add the extension to, if the required data is present
   * @param eob the root eob (only used for logging purposes)
   * @param statusCode the status code to check for and add data from if exists
   * @return the {@link ItemComponent}
   */
  static ItemComponent mapEobCommonItemRevenueStatusCode(
      @Nonnull ItemComponent item, @Nonnull ExplanationOfBenefit eob, Optional<String> statusCode) {

    Assert.notNull(item, "Item must be non-null");
    Assert.notNull(eob, "Eob must be non-null");

    if (statusCode.isPresent()) {
      item.getRevenue()
          .addExtension(
              TransformerUtilsV2.createExtensionCoding(
                  eob, CcwCodebookVariable.REV_CNTR_STUS_IND_CD, statusCode));
    }

    return item;
  }

  /**
   * Gets the reference variable.
   *
   * @param ccwCodebookVariable the {@link CcwCodebookVariable} to get the url
   * @return url as a string
   */
  static String getReferenceUrl(CcwCodebookVariable ccwCodebookVariable) {
    return CCWUtils.calculateVariableReferenceUrl(ccwCodebookVariable);
  }

  /**
   * Checks to see if there is a extension that already exists in the {@link CareTeamComponent} so a
   * duplicate entry for extension is not added.
   *
   * @param careTeamComponent - Careteam component
   * @param referenceUrl the {@link String} is the reference url to compare
   * @param codeValue - the {@link String} is the code value to compare
   * @return {@link Boolean} whether it was found or not
   */
  public static boolean careTeamHasMatchingExtension(
      CareTeamComponent careTeamComponent, String referenceUrl, String codeValue) {

    if (!Strings.isNullOrEmpty(referenceUrl)
        && !Strings.isNullOrEmpty(codeValue)
        && careTeamComponent.getExtension().size() > 0) {
      List<Extension> extensions = careTeamComponent.getExtensionsByUrl(referenceUrl);

      for (Extension ext : extensions) {
        if (ext.getValue() instanceof Coding) {
          Coding coding = (Coding) ext.getValue();

          if (coding != null && coding.getCode().equals(codeValue)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Transforms the common item level data elements between the {@link OutpatientClaimLine} {@link
   * HospiceClaimLine} and {@link HHAClaimLine} claim types to FHIR. The method parameter fields
   * from {@link OutpatientClaimLine} {@link HospiceClaimLine} and {@link HHAClaimLine} are listed
   * below and their corresponding RIF CCW fields (denoted in all CAPS below from {@link
   * OutpatientClaimColumn} {@link HospiceClaimColumn} and {@link HHAClaimColumn}.
   *
   * @param item the {@link ItemComponent} to modify
   * @param revenueCenterDate REV_CNTR_DT,
   * @param paymentAmount REV_CNTR_PMT_AMT_AMT
   */
  static void mapEobCommonItemRevenueOutHHAHospice(
      ItemComponent item, Optional<LocalDate> revenueCenterDate, BigDecimal paymentAmount) {

    // Revenue Center Date
    // REV_CNTR_DT => ExplainationOfBenefit.item.serviced
    if (revenueCenterDate.isPresent()) {
      item.setServiced(new DateType().setValue(convertToDate(revenueCenterDate.get())));
    }

    // REV_CNTR_PMT_AMT_AMT => ExplainationOfBenefit.item.adjudication
    addAdjudication(
        item,
        createAdjudicationAmtSlice(
            CcwCodebookVariable.REV_CNTR_PMT_AMT_AMT, C4BBAdjudication.SUBMITTED, paymentAmount));
  }

  /**
   * Looks up or adds a contained {@link Identifier} object to the current {@link Patient}. This is
   * used to store Identifier slices related to the Patient.
   *
   * @param patient The {@link Patient} to Patient.identifier details to
   * @param type The {@link C4BBIdentifierType} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   */
  static void addIdentifierSlice(Patient patient, C4BBIdentifierType type, Optional<String> value) {
    if (value.isPresent()) {
      Identifier id =
          new Identifier()
              .setType(createCodeableConcept(type.getSystem(), type.toCode()))
              .setValue(value.get());

      patient.addIdentifier(id);
    }
  }

  /**
   * Looks up or adds a contained {@link Organization} object to the current {@link
   * ExplanationOfBenefit}. This is used to store Identifier slices related to the Provider
   * organization.
   *
   * @param patient The {@link Patient} to Patient.identifier details to
   * @param codeable The {@link CodeableConcept} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   */
  static void addIdentifierSlice(
      Patient patient, CodeableConcept codeable, Optional<String> value) {
    addIdentifierSlice(patient, codeable, value, Optional.empty());
  }

  /**
   * Looks up or adds a contained {@link Identifier} object to the current {@link Patient}. This is
   * used to store Identifier slices related to the Provider organization.
   *
   * @param patient The {@link Patient} to Patient.identifier details to
   * @param codeable The {@link CodeableConcept} of the identifier slice
   * @param value The value of the identifier. If empty, this call is a no-op
   * @param systemUri optional system namespace for thee value
   */
  static void addIdentifierSlice(
      Patient patient,
      CodeableConcept codeable,
      Optional<String> value,
      Optional<String> systemUri) {
    if (value.isPresent()) {
      Identifier id = new Identifier().setType(codeable).setValue(value.get());
      if (systemUri.isPresent()) {
        id.setSystem(systemUri.get());
      }
      patient.addIdentifier(id);
    }
  }

  /**
   * Convenience method to convert race code {@link CcwCodebookVariable#RACE} to a {@link
   * RaceCategory}*. Input values can be: 0 Unknown 1 White 2 Black 3 Other 4 Asian 5 Hispanic 6
   * North American Native.
   *
   * @param value The race code to categorize
   * @return the race category denoted by the input value
   */
  static RaceCategory getRaceCategory(char value) {
    switch (value) {
      case '1':
        return RaceCategory.WHITE;
      case '2':
        return RaceCategory.BLACK_OR_AFRICAN_AMERICAN;
      case '4':
        return RaceCategory.ASIAN;
      case '6':
        return RaceCategory.AMERICAN_INDIAN_OR_ALASKA_NATIVE;
      default:
        return RaceCategory.UNKNOWN;
    }
  }

  /**
   * Sets the provider number field which is common among these claim types: Inpatient, Outpatient,
   * Hospice, HHA and SNF.
   *
   * @param eob the {@link ExplanationOfBenefit} this method will modify
   * @param providerNumber a {@link String} PRVDR_NUM: representing the provider number for the
   *     claim
   */
  static void setProviderNumber(ExplanationOfBenefit eob, String providerNumber) {
    eob.setProvider(
        new Reference()
            .setIdentifier(
                TransformerUtilsV2.createIdentifier(
                    CcwCodebookVariable.PRVDR_NUM, providerNumber)));
  }

  /**
   * Logs the mbi hash to mdc.
   *
   * @param mbiHash the mbi hash to log
   */
  public static void logMbiHashToMdc(String mbiHash) {
    if (!Strings.isNullOrEmpty(mbiHash)) {
      BfdMDC.put("mbi_hash", mbiHash);
    }
  }

  /**
   * Compares {@link LocalDate} a against {@link LocalDate} using the supplied {@link
   * ParamPrefixEnum}.
   *
   * @param a the first item to compare
   * @param b the second item to compare
   * @param prefix prefix to use. Supported: {@link ParamPrefixEnum#GREATERTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#GREATERTHAN}, {@link ParamPrefixEnum#LESSTHAN_OR_EQUALS}, {@link
   *     ParamPrefixEnum#LESSTHAN}
   * @return true if the comparison between a and b returned true
   * @throws IllegalArgumentException if caller supplied an unsupported prefix
   */
  public static boolean compareLocalDate(
      @Nullable LocalDate a, @Nullable LocalDate b, ParamPrefixEnum prefix) {
    if (a == null || b == null) {
      return false;
    }
    switch (prefix) {
      case GREATERTHAN_OR_EQUALS:
        return !a.isBefore(b);
      case GREATERTHAN:
        return a.isAfter(b);
      case LESSTHAN_OR_EQUALS:
        return !a.isAfter(b);
      case LESSTHAN:
        return a.isBefore(b);
      default:
        throw new InvalidRequestException(String.format("Unsupported prefix supplied: %s", prefix));
    }
  }

  /**
   * Process a {@link Set} of {@link ClaimType} entries and build an {@link EnumSet} of {@link
   * ClaimType} entries that meet the criteria of having claims data claims data (derived from int
   * bitmask) and match claim(s) requested by caller.
   *
   * @param claimTypes {@link Set} set of {@link ClaimType} identifiers requested by client.
   * @param val int bitmask denoting the claim types that have data.
   * @return {@link EnumSet} of {@link ClaimType} types to process.
   */
  public static EnumSet<ClaimType> fetchClaimsAvailability(Set<ClaimType> claimTypes, int val) {
    EnumSet<ClaimType> availSet = EnumSet.noneOf(ClaimType.class);
    if ((val & QueryUtils.V_CARRIER_HAS_DATA) != 0 && claimTypes.contains(ClaimType.CARRIER)) {
      availSet.add(ClaimType.CARRIER);
    }
    if ((val & QueryUtils.V_DME_HAS_DATA) != 0 && claimTypes.contains(ClaimType.DME)) {
      availSet.add(ClaimType.DME);
    }
    if ((val & QueryUtils.V_PART_D_HAS_DATA) != 0 && claimTypes.contains(ClaimType.PDE)) {
      availSet.add(ClaimType.PDE);
    }
    if ((val & QueryUtils.V_INPATIENT_HAS_DATA) != 0 && claimTypes.contains(ClaimType.INPATIENT)) {
      availSet.add(ClaimType.INPATIENT);
    }
    if ((val & QueryUtils.V_OUTPATIENT_HAS_DATA) != 0
        && claimTypes.contains(ClaimType.OUTPATIENT)) {
      availSet.add(ClaimType.OUTPATIENT);
    }
    if ((val & QueryUtils.V_HOSPICE_HAS_DATA) != 0 && claimTypes.contains(ClaimType.HOSPICE)) {
      availSet.add(ClaimType.HOSPICE);
    }
    if ((val & QueryUtils.V_SNF_HAS_DATA) != 0 && claimTypes.contains(ClaimType.SNF)) {
      availSet.add(ClaimType.SNF);
    }
    if ((val & QueryUtils.V_HHA_HAS_DATA) != 0 && claimTypes.contains(ClaimType.HHA)) {
      availSet.add(ClaimType.HHA);
    }
    return availSet;
  }
}
